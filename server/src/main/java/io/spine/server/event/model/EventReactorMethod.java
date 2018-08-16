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

package io.spine.server.event.model;

import com.google.protobuf.Message;
import io.spine.base.EventMessage;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.React;
import io.spine.server.event.EventReactor;
import io.spine.server.model.AbstractHandlerMethod;
import io.spine.server.model.MethodAccessChecker;
import io.spine.server.model.MethodFactory;
import io.spine.server.model.ReactorMethodResult;

import java.lang.reflect.Method;

import static io.spine.server.model.MethodAccessChecker.forMethod;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A wrapper for a method which {@linkplain React reacts} on events.
 *
 * @author Alexander Yevsyukov
 * @see React
 */
public final class EventReactorMethod
        extends AbstractHandlerMethod<EventReactor, EventClass, EventContext, ReactorMethodResult> {

    private EventReactorMethod(Method method) {
        super(method);
    }

    @Override
    public EventClass getMessageClass() {
        return EventClass.from(rawMessageClass());
    }

    /**
     * {@inheritDoc}
     *
     * @return the list of event messages (or an empty list if the reactor method returns nothing)
     */
    @Override
    public ReactorMethodResult invoke(EventReactor target, Message message, EventContext context) {
        ensureExternalMatch(context.getExternal());
        ReactorMethodResult result = super.invoke(target, message, context);
        return result;
    }

    @Override
    protected ReactorMethodResult toResult(EventReactor target, Object rawMethodOutput) {
        return new ReactorMethodResult(target, rawMethodOutput);
    }

    static MethodFactory<EventReactorMethod> factory() {
        return Factory.INSTANCE;
    }

    /**
     * The factory for creating {@link EventReactorMethod event reactor} methods.
     */
    private static class Factory extends MethodFactory<EventReactorMethod> {

        private static final Factory INSTANCE = new Factory();

        private Factory() {
            super(EventReactorMethod.class, new Filter());
        }

        @Override
        public void checkAccessModifier(Method method) {
            MethodAccessChecker checker = forMethod(method);
            checker.checkPackagePrivate("Reactive handler method {} should be package-private.");
        }

        @Override
        protected EventReactorMethod doCreate(Method method) {
            return new EventReactorMethod(method);
        }
    }

    /**
     * The predicate that filters event reactor methods.
     */
    private static class Filter extends EventMethodPredicate {

        private Filter() {
            super(React.class);
        }

        @Override
        protected boolean verifyReturnType(Method method) {
            if (returnsMessage(method, EventMessage.class)) {
                checkMessageClass(method);
                return true;
            }

            boolean iterableOrOptional = returnsIterableOrOptional(method);
            return iterableOrOptional;
        }

        /**
         * Ensures that the method does not return the same class of message as one that passed
         * as the first method parameter.
         *
         * <p>This protection is needed to prevent repeated events passed to Event Store.
         *
         * @throws IllegalStateException if the type of the first parameter is the same as
         *                               the return value
         */
        private static void checkMessageClass(Method method) {
            Class<?> returnType = method.getReturnType();

            // The returned value must not be of the same type as the passed message param.
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length < 1) {
                /* The number of parameters is checked by `verifyParams()`.
                   We check the number of parameters here to avoid accidental exception in case
                   this method is called before `verifyParams()`. */
                return;
            }
            Class<?> firstParamType = paramTypes[0];
            if (firstParamType.equals(returnType)) {
                throw newIllegalStateException(
                        "React method cannot return the same event message {}",
                        firstParamType.getName());
            }
        }
    }
}