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

package io.spine.server.command.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.Immutable;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.base.RejectionMessage;
import io.spine.core.CommandContext;
import io.spine.core.EventContext;
import io.spine.server.command.Command;
import io.spine.server.model.MethodParams;
import io.spine.server.model.MethodSignature;
import io.spine.server.model.ParameterSpec;
import io.spine.server.type.EventEnvelope;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A signature of {@link CommandReactionMethod}.
 */
public class CommandReactionSignature
        extends MethodSignature<CommandReactionMethod, EventEnvelope> {

    private static final ImmutableSet<CommandReactionParams>
            PARAM_SPECS = ImmutableSet.copyOf(CommandReactionParams.values());

    private static final ImmutableSet<TypeToken<?>>
            RETURN_TYPES = ImmutableSet.of(
                    TypeToken.of(CommandMessage.class),
                    new TypeToken<Iterable<CommandMessage>>() {},
                    new TypeToken<Optional<CommandMessage>>() {}
                    );

    CommandReactionSignature() {
        super(Command.class);
    }

    @Override
    public ImmutableSet<? extends ParameterSpec<EventEnvelope>> paramSpecs() {
        return PARAM_SPECS;
    }

    @Override
    protected ImmutableSet<TypeToken<?>> returnTypes() {
        return RETURN_TYPES;
    }

    @Override
    public CommandReactionMethod create(Method method, ParameterSpec<EventEnvelope> params) {
        return new CommandReactionMethod(method, params);
    }

    /**
     * Tells that the method may state that a reaction isn't needed by returning
     * {@link io.spine.server.model.DoNothing DoNothing}.
     */
    @Override
    public boolean mayReturnIgnored() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * @implNote This method distinguishes {@linkplain Command Commander} methods one from another,
     * as they use the same annotation, but have different parameter list. It skips the methods
     * which first parameter {@linkplain MethodParams#firstIsCommand(Method) is }
     * a {@code Command} message.
     */
    @Override
    protected boolean skipMethod(Method method) {
        boolean parentResult = !super.skipMethod(method);
        if (parentResult) {
            return MethodParams.firstIsCommand(method);
        }
        return true;
    }

    /**
     * Allowed combinations of parameters for {@linkplain CommandReactionMethod Command reaction}
     * methods.
     */
    @Immutable
    private enum CommandReactionParams implements ParameterSpec<EventEnvelope> {

        MESSAGE {
            @Override
            public boolean matches(MethodParams params) {
                return params.is(EventMessage.class) && params.declaredAsClasses();
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                return new Object[]{event.message()};
            }
        },

        EVENT_AND_EVENT_CONTEXT {
            @Override
            public boolean matches(MethodParams params) {
                return params.are(EventMessage.class, EventContext.class)
                        && params.declaredAsClasses();
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                return new Object[]{event.message(), event.context()};
            }
        },

        REJECTION_AND_COMMAND_CONTEXT {
            @Override
            public boolean matches(MethodParams params) {
                return params.are(RejectionMessage.class, CommandContext.class)
                        && params.declaredAsClasses();
            }

            @Override
            public Object[] extractArguments(EventEnvelope event) {
                CommandContext originContext =
                        event.context()
                             .getRejection()
                             .getCommand()
                             .getContext();
                return new Object[]{event.message(), originContext};
            }
        }
    }
}
