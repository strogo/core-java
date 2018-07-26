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

package io.spine.server.command.model;

import com.google.protobuf.Message;
import io.spine.base.ThrowableMessage;
import io.spine.core.CommandClass;
import io.spine.core.CommandContext;
import io.spine.server.command.model.CommandSubstituteMethod.Result;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodExceptionChecker;
import io.spine.server.model.MethodResult;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/**
 * A method that produces one or more command messages in response to an incoming command.
 *
 * @author Alexander Yevsyukov
 */
public final class CommandSubstituteMethod
        extends CommandAcceptingMethod<Result>
        implements CommandingMethod<CommandClass, CommandContext, Result> {

    private CommandSubstituteMethod(Method method) {
        super(method);
    }

    @Override
    protected Result toResult(Object target, Object rawMethodOutput) {
        Result result = new Result(rawMethodOutput);
        return result;
    }

    static CommandSubstituteMethod from(Method method) {
        return new CommandSubstituteMethod(method);
    }

    static AbstractHandlerMethod.Factory<CommandSubstituteMethod> factory() {
        return Factory.INSTANCE;
    }

    private static class Factory extends AbstractHandlerMethod.Factory<CommandSubstituteMethod> {

        private static final Factory INSTANCE = new Factory();

        @Override
        public Class<CommandSubstituteMethod> getMethodClass() {
            return CommandSubstituteMethod.class;
        }

        @Override
        public Predicate<Method> getPredicate() {
            return Filter.INSTANCE;
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = MethodAccessChecker.forMethod(method);
            checker.checkPackagePrivate(
                    "Command substitution method {} should be package-private."
            );
        }

        @Override
        protected void checkThrownExceptions(Method method) {
            MethodExceptionChecker checker = MethodExceptionChecker.forMethod(method);
            checker.checkThrowsNoExceptionsBut(ThrowableMessage.class);
        }

        @Override
        protected CommandSubstituteMethod doCreate(Method method) {
            return from(method);
        }
    }

    /**
     * Filters command substitution methods.
     */
    private static final class Filter extends AbstractPredicate<CommandContext> {

        private static final Filter INSTANCE = new Filter();

        private Filter() {
            super(CommandContext.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            boolean result = returnsMessageOrIterable(method);
            return result;
        }
    }

    /**
     * A command substitution method returns a one or more command messages.
     */
    public static final class Result extends MethodResult<Message> {

        private Result(Object rawMethodOutput) {
            super(rawMethodOutput);
            List<Message> messages = toMessages(rawMethodOutput);
            setMessages(messages);
        }
    }
}
