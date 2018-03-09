/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

import io.spine.annotation.Internal;
import io.spine.server.entity.Entity;
import io.spine.server.entity.storage.EntityColumn.MemoizedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newLinkedList;
import static io.spine.server.entity.storage.ColumnRecords.getAnnotatedVersion;
import static io.spine.util.Exceptions.newIllegalStateException;
import static io.spine.validate.Validate.checkNotEmptyOrBlank;
import static java.lang.String.format;

/**
 * A utility class for working with {@linkplain EntityColumn entity columns}.
 *
 * <p>The methods of all {@link Entity entities} that fit
 * <a href="http://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">
 * the Java Bean</a> getter spec and annotated with {@link Column}
 * are considered {@linkplain EntityColumn columns}.
 *
 * <p>Inherited columns are taken into account too, but building entity hierarchies is strongly
 * discouraged.
 *
 * <p>Note that the returned type of an {@link EntityColumn} getter must either be primitive or
 * serializable, otherwise a runtime exception is thrown when trying to get an instance of
 * {@link EntityColumn}.
 *
 * @author Dmytro Dashenkov
 * @see EntityColumn
 */
@Internal
public class Columns {

    private static final String SPINE_PACKAGE = "io.spine.";
    private static final String NON_PUBLIC_CLASS_WARNING =
            "Passed entity class %s is not public. Storage fields won't be extracted.";
    private static final String NON_PUBLIC_INTERNAL_CLASS_WARNING =
            "Passed entity class %s is probably a Spine internal non-public entity. " +
                    "Storage fields won't be extracted.";

    /**
     * Prevent instantiation of this utility class.
     */
    private Columns() {
    }

    /**
     * Ensures that the entity columns are valid for the specified entity class.
     *
     * <p>This method tries to extract {@linkplain EntityColumn entity columns} from the given class,
     * performing all checks along the way.
     *
     * <p>If extraction is performed without errors, the check is passed, if not - failed.
     *
     * @param entityClass the class to check entity columns
     */
    public static void checkColumnDefinitions(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        obtainColumns(entityClass);
    }

    /**
     * Retrieves an {@link EntityColumn} instance of the given name and from the given entity class.
     *
     * <p>If no column is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @param columnName  the entity column {@linkplain EntityColumn#getName() name}
     * @return an instance of {@link EntityColumn} with the given name
     * @throws IllegalArgumentException if the {@link EntityColumn} is not found
     */
    static EntityColumn findColumn(Class<? extends Entity> entityClass, String columnName) {
        checkNotNull(entityClass);
        checkNotEmptyOrBlank(columnName, "entity column name");

        final Collection<EntityColumn> entityColumns = Columns.obtainColumns(entityClass);
        for (EntityColumn column : entityColumns) {
            if (column.getName()
                    .equals(columnName)) {
                return column;
            }
        }

        throw new IllegalArgumentException(
                format("Could not find an EntityColumn description for %s.%s.",
                        entityClass.getCanonicalName(),
                        columnName));
    }

    /**
     * Retrieves {@linkplain EntityColumn columns} for the given {@code Entity} class.
     *
     * <p>Performs checks for entity column definitions correctness along the way.
     *
     * <p>If check for correctness fails, throws {@link IllegalStateException}.
     *
     * @param entityClass the class containing the {@link EntityColumn} definition
     * @return a {@link Collection} of {@link EntityColumn} corresponded to entity class
     * @throws IllegalStateException if entity column definitions are incorrect
     */
    static Collection<EntityColumn> obtainColumns(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        final BeanInfo entityDescriptor;
        try {
            entityDescriptor = Introspector.getBeanInfo(entityClass);
        } catch (IntrospectionException e) {
            throw new IllegalStateException(e);
        }

        final Collection<EntityColumn> entityColumns = newLinkedList();
        for (PropertyDescriptor property : entityDescriptor.getPropertyDescriptors()) {
            final Method getter = property.getReadMethod();
            final boolean isEntityColumn = getAnnotatedVersion(getter).isPresent();
            if (isEntityColumn) {
                final EntityColumn column = EntityColumn.from(getter);
                entityColumns.add(column);
            }
        }
        checkRepeatedColumnNames(entityColumns, entityClass);

        return entityColumns;
    }

    /**
     * Generates the {@linkplain EntityColumn column} values for the given {@linkplain Entity}.
     *
     * <p>Retrieves {@linkplain EntityColumn columns} for the given {@code Entity} class, then generates
     * {@linkplain MemoizedValue memoized values} from them.
     *
     * @param entity an {@link Entity} to get the {@linkplain EntityColumn column} values from
     * @param <E>    the type of the {@link Entity}
     * @return a {@link Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain MemoizedValue memoized values}
     * @see MemoizedValue
     */
    static <E extends Entity<?, ?>> Map<String, EntityColumn.MemoizedValue> extractColumnValues(E entity) {
        checkNotNull(entity);

        final Collection<EntityColumn> entityColumns = Columns.obtainColumns(entity.getClass());
        return Columns.extractColumnValues(entity, entityColumns);
    }

    /**
     * Generates the {@linkplain EntityColumn column} values for the given {@linkplain Entity}.
     *
     * <p>Uses given {@linkplain EntityColumn entity columns} for the value extraction.
     *
     * @param entity an {@link Entity} to get the {@linkplain EntityColumn column} values from
     * @param entityColumns {@linkplain EntityColumn entity columns} which values should be extracted
     * @param <E>    the type of the {@link Entity}
     * @return a {@link Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain MemoizedValue memoized values}
     * @see MemoizedValue
     */
    static <E extends Entity<?, ?>> Map<String, MemoizedValue> extractColumnValues(
            E entity,
            Collection<EntityColumn> entityColumns) {
        checkNotNull(entityColumns);
        checkNotNull(entity);

        final Class<? extends Entity> entityClass = entity.getClass();
        if (!Columns.isPublic(entityClass)) {
            return Collections.emptyMap();
        }

        final Map<String, MemoizedValue> values = recordColumnValuesToMap(entityColumns, entity);

        return values;
    }

    /**
     * Checks if the given {@link Entity entity class} is public.
     *
     * <p>Outputs a message to the log if the class is non-public.
     *
     * @param entityClass {@link Entity entity class} to check
     * @return {@code true} if class is public and {@code false} otherwise
     */
    private static boolean isPublic(Class<? extends Entity> entityClass) {
        checkNotNull(entityClass);

        final int modifiers = entityClass.getModifiers();
        if (!Modifier.isPublic(modifiers)) {
            logNonPublicClass(entityClass);
            return false;
        }

        return true;
    }

    /**
     * Generates {@linkplain MemoizedValue memoized values} for the given {@linkplain EntityColumn columns}
     * of the given {@linkplain Entity}.
     *
     * <p>Records the result to {@link Map}.
     *
     * @param columns {@link Collection collection} of columns to extract values from
     * @param entity {@link Entity} from which to extract the values
     * @return a {@link Map} of the column {@linkplain EntityColumn#getStoredName()
     *         names for storing} to their {@linkplain MemoizedValue memoized values}
     * @see MemoizedValue
     */
    private static <E extends Entity<?, ?>> Map<String, MemoizedValue> recordColumnValuesToMap(
            Collection<EntityColumn> columns,
            E entity) {
        final Map<String, MemoizedValue> values = new HashMap<>(columns.size());
        for (EntityColumn column : columns) {
            final String name = column.getStoredName();
            final MemoizedValue value = column.memoizeFor(entity);
            values.put(name, value);
        }
        return values;
    }

    /**
     * Ensures that the specified columns have no repeated names.
     *
     * <p>If the check fails, throws {@link IllegalStateException}.
     *
     * @param columns     the columns to check
     * @param entityClass the entity class for the columns
     * @throws IllegalStateException if columns contain repeated names
     */
    private static void checkRepeatedColumnNames(Iterable<EntityColumn> columns,
                                                 Class<? extends Entity> entityClass) {
        final Collection<String> checkedNames = newLinkedList();
        for (EntityColumn column : columns) {
            final String columnName = column.getStoredName();
            if (checkedNames.contains(columnName)) {
                throw newIllegalStateException(
                        "The entity `%s` has columns with the same name for storing `%s`.",
                        entityClass.getName(),
                        columnName);
            }
            checkedNames.add(columnName);
        }
    }

    /**
     * Writes the non-public {@code Entity} class warning into the log unless the passed class
     * represents one of the Spine internal {@link Entity} implementations.
     */
    private static void logNonPublicClass(Class<? extends Entity> cls) {
        final String className = cls.getCanonicalName();
        final boolean internal = className.startsWith(SPINE_PACKAGE);
        if (internal) {
            log().trace(format(NON_PUBLIC_INTERNAL_CLASS_WARNING, className));
        } else {
            log().warn(format(NON_PUBLIC_CLASS_WARNING, className));
        }
    }

    private static Logger log() {
        return LogSingleton.INSTANCE.value;
    }

    private enum LogSingleton {
        INSTANCE;
        @SuppressWarnings("NonSerializableFieldInSerializableClass")
        private final Logger value = LoggerFactory.getLogger(Columns.class);
    }
}
