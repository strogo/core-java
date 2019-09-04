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

package io.spine.server.model;

import io.spine.server.command.model.CommandHandlerSignature;
import io.spine.server.event.model.EventReactorSignature;
import io.spine.server.model.given.map.DupEventFilterValue;
import io.spine.server.model.given.map.DuplicateCommandHandlers;
import io.spine.server.model.given.map.DuplicateEventReactors;
import io.spine.server.model.given.map.TwoFieldsInSubscription;
import io.spine.string.StringifierRegistry;
import io.spine.string.Stringifiers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.spine.server.model.HandlerMap.create;
import static io.spine.server.projection.model.ProjectionClass.asProjectionClass;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("`HandlerMap` should")
class HandlerMapTest {

    /**
     * Registers the stringifier for {@code Integer}, which is used for parsing filter field values.
     */
    @BeforeAll
    static void prepare() {
        StringifierRegistry.instance()
                           .register(Stringifiers.forInteger(), Integer.TYPE);
    }

    @Nested
    @DisplayName("not allow")
    class DuplicateHandler {

        @Test
        @DisplayName("duplicating message classes in handlers")
        void rejectDuplicateHandlers() {
            assertDuplication(
                    () -> create(DuplicateCommandHandlers.class, new CommandHandlerSignature())
            );
        }

        @Test
        @DisplayName("duplicating event reactors")
        void reactorDuplication() {
            assertDuplication(
                    () -> create(DuplicateEventReactors.class, new EventReactorSignature())
            );
        }

        @Test
        @DisplayName("the same value of the filtered event field")
        void rejectFilterFieldDuplication() {
            assertDuplication(() -> asProjectionClass(DupEventFilterValue.class));
        }

        @Test
        @DisplayName("the same event filtering by different fields")
        void failToSubscribeByDifferentFields() {
            assertThrows(
                    HandlerFieldFilterClashError.class,
                    () -> asProjectionClass(TwoFieldsInSubscription.class)
            );
        }

        void assertDuplication(Runnable runnable) {
            assertThrows(DuplicateHandlerMethodError.class, runnable::run);
        }
    }
}
