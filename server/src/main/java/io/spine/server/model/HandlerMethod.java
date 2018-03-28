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
package io.spine.server.model;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.Message;
import io.spine.core.Event;
import io.spine.core.MessageEnvelope;
import io.spine.core.Version;
import io.spine.server.event.EventFactory;
import io.spine.type.MessageClass;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.model.MethodExceptionChecker.forMethod;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * An abstract base for wrappers over methods handling messages.
 *
 * <p>Two message handlers are equivalent when they refer to the same method on the
 * same object (not class).
 *
 * @param <M> the type of the message class
 * @param <C> the type of the message context or {@link com.google.protobuf.Empty Empty} if
 *            a context parameter is never used
 * @author Mikhail Melnik
 * @author Alexander Yevsyukov
 */
public abstract class HandlerMethod<M extends MessageClass, C extends Message> {

    /** The method to be called. */
    private final Method method;

    /** The class of the first parameter. */
    private final Class<? extends Message> messageClass;

    /** The number of parameters the method has. */
    private final int paramCount;

    /** The set of the metadata attributes set via method annotations. */
    private final Set<MethodAttribute<?>> attributes;

    /**
     * Creates a new instance to wrap {@code method} on {@code target}.
     *
     * @param method subscriber method
     */
    protected HandlerMethod(Method method) {
        this.method = checkNotNull(method);
        this.messageClass = getFirstParamType(method);
        this.paramCount = method.getParameterTypes().length;
        this.attributes = discoverAttributes(method);
        method.setAccessible(true);
    }

    protected final Class<? extends Message> rawMessageClass() {
        return messageClass;
    }

    public abstract M getMessageClass();

    public abstract HandlerKey key();

    /**
     * Returns a full method name without parameters.
     *
     * @param method a method to get name for
     * @return full method name
     */
    private static String getFullMethodName(Method method) {
        return method.getDeclaringClass()
                     .getName() + '.' + method.getName() + "()";
    }

    /**
     * Returns the class of the first parameter of the passed handler method object.
     *
     * <p>It is expected that the first parameter of the passed method is always of
     * a class implementing {@link Message}.
     *
     * @param handler the method object to take first parameter type from
     * @return the class of the first method parameter
     * @throws ClassCastException if the first parameter isn't a class implementing {@link Message}
     */
    public static Class<? extends Message> getFirstParamType(Method handler) {
        @SuppressWarnings("unchecked") /* we always expect first param as {@link Message} */
        final Class<? extends Message> result =
                (Class<? extends Message>) handler.getParameterTypes()[0];
        return result;
    }

    public static List<Event> toEvents(final Any producerId,
                                       @Nullable final Version version,
                                       final List<? extends Message> eventMessages,
                                       final MessageEnvelope origin) {
        checkNotNull(producerId);
        checkNotNull(eventMessages);
        checkNotNull(origin);

        final EventFactory eventFactory =
                EventFactory.on(origin, producerId);

        return Lists.transform(eventMessages, new Function<Message, Event>() {
            @Override
            public Event apply(@Nullable Message eventMessage) {
                checkNotNull(eventMessage);
                final Event result = eventFactory.createEvent(eventMessage, version);
                return result;
            }
        });
    }

    /** Returns the handling method. */
    protected Method getMethod() {
        return method;
    }

    private int getModifiers() {
        return method.getModifiers();
    }

    /** Returns {@code true} if the method is declared {@code public}, {@code false} otherwise. */
    protected boolean isPublic() {
        final boolean result = Modifier.isPublic(getModifiers());
        return result;
    }

    /** Returns {@code true} if the method is declared {@code private}, {@code false} otherwise. */
    protected boolean isPrivate() {
        final boolean result = Modifier.isPrivate(getModifiers());
        return result;
    }

    /** Returns the count of the method parameters. */
    protected int getParamCount() {
        return paramCount;
    }

    /** Returns the set of method attributes configured for this method. */
    public final Set<MethodAttribute<?>> getAttributes() {
        return attributes;
    }

    private static Set<MethodAttribute<?>> discoverAttributes(Method method) {
        checkNotNull(method);
        final ExternalAttribute externalAttribute = ExternalAttribute.of(method);
        return ImmutableSet.<MethodAttribute<?>>of(externalAttribute);
    }

    /**
     * Casts a handling result to a list of event messages.
     *
     * @param output the command handler method return value.
     *               Could be a {@link Message}, a list of messages, or {@code null}.
     * @return the list of event messages or an empty list if {@code null} is passed
     */
    @SuppressWarnings({"unchecked", "ChainOfInstanceofChecks"})
    protected static List<? extends Message> toList(@Nullable Object output) {
        if (output == null) {
            return emptyList();
        }

        // Allow reacting methods to return `Empty` instead of empty `List`. Do not store such
        // events. Command Handling methods will not be able to use this trick because we check
        // for non-empty result of such methods.
        if (output instanceof Empty) {
            return emptyList();
        }

        if (output instanceof List) {
            // Cast to the list of messages as it is the one of the return types
            // we expect by methods we call.
            final List<? extends Message> result = (List<? extends Message>) output;
            return result;
        }

        // If it's not a list it could be another `Iterable`.
        if (output instanceof Iterable) {
            return ImmutableList.copyOf((Iterable<? extends Message>)output);
        }

        // Another type of result is single event message (as Message).
        final List<Message> result = singletonList((Message) output);
        return result;
    }

    /**
     * Invokes the wrapped method to handle {@code message} with the {@code context}.
     *
     * @param target  the target object on which call the method
     * @param message the message to handle
     * @param context the context of the message
     * @return the result of message handling
     */
    public Object invoke(Object target, Message message, C context) {
        checkNotNull(message);
        checkNotNull(context);
        try {
            final int paramCount = getParamCount();
            final Object returnedValue = (paramCount == 1)
                    ? method.invoke(target, message)
                    : method.invoke(target, message, context);
            return returnedValue;
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            throw whyFailed(target, message, context, e);
        }
    }

    /**
     * Creates an exception containing information on the failure of the handler method invocation.
     *
     * @param target  the object which method was invoked
     * @param message the message dispatched to the object
     * @param context the context of the message
     * @param cause   exception instance thrown by the invoked method
     * @return the exception thrown during the invocation
     */
    protected HandlerMethodFailedException whyFailed(Object target,
                                                     Message message,
                                                     C context,
                                                     Exception cause) {
        return new HandlerMethodFailedException(target, message, context, cause);
    }

    /**
     * Returns a full name of the handler method.
     *
     * <p>The full name consists of a fully qualified class name of the target object and
     * the method name separated with a dot character.
     *
     * @return full name of the subscriber
     */
    public String getFullName() {
        return getFullMethodName(method);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return (prime + method.hashCode());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final HandlerMethod other = (HandlerMethod) obj;

        return Objects.equals(this.method, other.method);
    }

    /**
     * @return full name of the handler method
     * @see #getFullName()
     */
    @Override
    public String toString() {
        return getFullName();
    }

    /**
     * The base class for factory objects that can filter {@link Method} objects
     * that represent handler methods and create corresponding {@code HandlerMethod} instances
     * that wrap those methods.
     *
     * @param <H> the type of the handler method objects to create
     */
    public abstract static class Factory<H extends HandlerMethod> {

        /** Returns the class of the method wrapper. */
        public abstract Class<H> getMethodClass();

        /** Returns a predicate for filtering methods. */
        public abstract Predicate<Method> getPredicate();

        /**
         * Checks an access modifier of the method and logs a warning if it is invalid.
         *
         * @param method the method to check
         * @see MethodAccessChecker
         */
        public abstract void checkAccessModifier(Method method);

        /**
         * Creates a {@linkplain HandlerMethod wrapper} for the method.
         *
         * <p>Performs various checks before wrapper creation, e.g. method access modifier or
         * whether method throws any prohibited exceptions.
         *
         * @param method the method to create wrapper for
         * @return a wrapper object for the method
         * @throws IllegalStateException in case some of the method checks fail
         */
        public H create(Method method) {
            checkAccessModifier(method);
            checkThrownExceptions(method);
            return createForMethod(method);
        }

        /** Creates a wrapper object for a method. */
        protected abstract H createForMethod(Method method);

        /**
         * Ensures method does not throw any prohibited exception types.
         *
         * <p>In case it does, the {@link IllegalStateException} containing diagnostics info is
         * thrown.
         *
         * @param method the method to check
         * @throws IllegalStateException if the method throws any prohibited exception types
         * @see MethodExceptionChecker
         */
        protected void checkThrownExceptions(Method method) {
            final MethodExceptionChecker checker = forMethod(method);
            checker.checkThrowsNoCheckedExceptions();
        }
    }
}
