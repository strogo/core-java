/*
 * Copyright 2018, TeamDev. All rights reserved.
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

package io.spine.server.command.given;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.StringValue;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.validate.StringValueVBuilder;

import java.util.List;

import static io.spine.testing.TestValues.newUuidValue;

public class CommandHandlingEntityTestEnv {

    /** Prevents instantiation of this utility class. */
    private CommandHandlingEntityTestEnv() {
    }

    /**
     * @return generated {@code StringValue} based on generated UUID
     */
    public static StringValue msg() {
        return newUuidValue();
    }

    /**
     * @return generated {@code String} based on generated UUID
     */
    public static String str() {
        return msg().getValue();
    }

    public static class HandlingEntity extends CommandHandlingEntity<Long,
                                                                     StringValue,
                                                                     StringValueVBuilder> {
        public HandlingEntity(Long id) {
            super(id);
        }

        @Override
        protected List<Event> dispatchCommand(CommandEnvelope cmd) {
            return ImmutableList.of();
        }
    }
}