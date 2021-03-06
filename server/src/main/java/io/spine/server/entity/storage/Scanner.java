/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.server.entity.storage;

import io.spine.base.EntityState;
import io.spine.query.CustomColumn;
import io.spine.query.EntityColumn;
import io.spine.server.entity.Entity;
import io.spine.server.entity.model.EntityClass;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * Scans and extracts the {@link EntityRecordSpec specification} of the stored record
 * from the passed {@link io.spine.server.entity.Entity Entity}.
 */
final class Scanner<I, S extends EntityState<I>, E extends Entity<I, S>>  {

    /**
     * The name of the nested class generated by the Spine compiler as a container of
     * the entity column definitions.
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")   // coincidental duplication
    private static final String COLS_NESTED_CLASSNAME = "Column";

    /**
     * The name of the method inside the column container class generated by the Spine compiler.
     *
     * <p>The method returns all the definitions of the columns for this state class.
     */
    private static final String COL_DEFS_METHOD_NAME = "definitions";

    /**
     * The target entity class.
     */
    private final EntityClass<E> entityClass;

    Scanner(EntityClass<E> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     * Obtains the {@linkplain SystemColumn system} columns of the class.
     */
    SystemColumns<E> systemColumns() {
        Set<CustomColumn<E, ?>> columns = new HashSet<>();
        Class<?> entityClazz = entityClass.value();
        Method[] methods = entityClazz.getMethods();
        for (Method method : methods) {
            if (method.isAnnotationPresent(SystemColumn.class)) {
                SystemColumn annotation = method.getAnnotation(SystemColumn.class);
                EntityRecordColumn columnImpl = annotation.impl();
                @SuppressWarnings("unchecked")      // Ensured by the declaration.
                CustomColumn<E, ?> column = (CustomColumn<E, ?>) columnImpl.get();
                columns.add(column);
            }
        }
        SystemColumns<E> result = new SystemColumns<>(columns);
        return result;
    }

    /**
     * Obtains the {@linkplain EntityColumn entity-state-based} columns of the class.
     */
    @SuppressWarnings("OverlyBroadCatchBlock")  // Treating all exceptions equally.
    StateColumns<S> stateColumns() {
        Class<? extends EntityState<?>> stateClass = entityClass.stateClass();
        Class<?> columnClass = findColumnsClass(stateClass);
        if(columnClass == null) {
            return StateColumns.none();
        }
        try {
            Method getDefinitions = columnClass.getDeclaredMethod(COL_DEFS_METHOD_NAME);
            @SuppressWarnings("unchecked")  // ensured by the Spine code generation.
            Set<EntityColumn<S, ?>> columns =
                    (Set<EntityColumn<S, ?>>) getDefinitions.invoke(null);
            return new StateColumns<>(columns);
        } catch (Exception e) {
            throw newIllegalStateException(
                    e,
                    "Error fetching the declared columns by invoking the `%s.%s()` method" +
                            " of the entity state type `%s`.",
                    COLS_NESTED_CLASSNAME, COL_DEFS_METHOD_NAME, stateClass.getName());
        }
    }

    /**
     * Finds the {@code Column} class which is generated the messages representing the entity
     * state type.
     *
     * <p>If an entity has no such class generated, it does not declare any columns. In this
     * case, this method returns {@code null}.
     * @param stateClass the class of the entity state to look for the method in
     * @return the class declaring the entity columns,
     * or {@code null} if the entity declares no columns
     */
    private static @Nullable Class<?> findColumnsClass(Class<? extends EntityState<?>> stateClass) {
        Class<?>[] innerClasses = stateClass.getDeclaredClasses();
        Class<?> columnClass = null;
        for (Class<?> aClass : innerClasses) {
            if(aClass.getSimpleName().equals(COLS_NESTED_CLASSNAME)) {
                columnClass = aClass;
            }
        }
        return columnClass;
    }
}
