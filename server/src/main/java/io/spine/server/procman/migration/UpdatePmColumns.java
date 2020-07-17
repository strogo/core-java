/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server.procman.migration;

import io.spine.annotation.Experimental;
import io.spine.base.EntityState;
import io.spine.protobuf.ValidatingBuilder;
import io.spine.server.entity.Migration;
import io.spine.server.entity.Transaction;
import io.spine.server.procman.ProcessManager;
import io.spine.server.procman.ProcessManagerMigration;

/**
 * //TODO:2020-07-17:alex.tymchenko: introduce the `onBeforeCommit()` and rewrite the docs.
 *
 * A migration operation that does the update of interface-based columns of a
 * {@link ProcessManager}.
 *
 * <p>When applied to an entity, this operation will trigger the recalculation of entity storage
 * fields according to the current implementation of
 * {@link io.spine.query.EntityWithColumns EntityWithColumns}-derived methods.
 *
 * <p>Such operation may be useful when the logic behind manually calculated columns changes as
 * well as when adding the new columns to an entity.
 *
 * @implNote The operation relies on the fact that column values are automatically calculated and
 *         propagated to the entity state on a transaction {@linkplain Transaction#commit() commit}.
 *         It thus does not change the entity state itself in {@link #apply(EntityState)}.
 *
 * @see io.spine.server.entity.RecordBasedRepository#applyMigration(Object, Migration)
 */
@Experimental
public final class UpdatePmColumns<I,
                                   P extends ProcessManager<I, S, B>,
                                   S extends EntityState<I>,
                                   B extends ValidatingBuilder<S>>
        extends ProcessManagerMigration<I, P, S, B> {

    @Override
    public S apply(S s) {
        return s;
    }
}
