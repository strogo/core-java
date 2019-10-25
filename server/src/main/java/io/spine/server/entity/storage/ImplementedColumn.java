/*
 * Copyright 2019, TeamDev. All rights reserved.
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

import com.google.errorprone.annotations.Immutable;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.code.proto.FieldDeclaration;
import io.spine.server.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

@Internal
public final class ImplementedColumn
        extends AbstractColumn
        implements ColumnDeclaredInProto, ManuallyCalculatedColumn {

    private final GetterFromState getterFromState;
    private final GetterFromEntity getterFromEntity;
    private final FieldDeclaration field;

    ImplementedColumn(ColumnName name,
                      Class<?> type,
                      GetterFromState getterFromState,
                      GetterFromEntity getterFromEntity,
                      FieldDeclaration field) {
        super(name, type);
        this.getterFromState = getterFromState;
        this.getterFromEntity = getterFromEntity;
        this.field = field;
    }

    @Override
    public @Nullable Object valueIn(Entity<?, ?> entity) {
        return getterFromEntity.apply(entity);
    }

    @Override
    public @Nullable Object valueIn(Message entityState) {
        return getterFromState.apply(entityState);
    }

    @Override
    public FieldDeclaration protoField() {
        return field;
    }

    @Immutable
    interface GetterFromState extends Function<Message, Object> {
    }

    @Immutable
    interface GetterFromEntity extends Function<Entity<?, ?>, Object> {
    }
}
