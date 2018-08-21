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

package io.spine.server.bus;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import io.spine.type.MessageClass;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Multimaps.synchronizedMultimap;

/**
 * A registry of message dispatchers.
 *
 * @param <C> the type of the class of dispatched messages
 * @param <D> the type of the message dispatchers
 * @author Alexander Yevsyukov
 * @author Alex Tymchenko
 */
public abstract class DispatcherRegistry<C extends MessageClass,
                                         D extends MessageDispatcher<C, ?, ?>> {

    /**
     * The map from a message class to one or more dispatchers of
     * messages of this class.
     *
     * <p>Some implementations like {@link io.spine.server.commandbus.CommandBus CommandBus}
     * will allow only one dispatcher per message class. This should be handled
     * when registering dispatchers.
     */
    private final Multimap<C, D> dispatchers = synchronizedMultimap(HashMultimap.create());

    /**
     * Registers the passed dispatcher.
     *
     * <p>If the dispatcher passes the {@linkplain #checkDispatcher(MessageDispatcher) check}
     * it is associated with the message classes it
     * {@linkplain MessageDispatcher#getMessageClasses()}exposes.
     *
     * @param dispatcher the dispatcher to register
     */
    protected void register(D dispatcher) {
        checkDispatcher(dispatcher);
        Set<C> messageClasses = dispatcher.getMessageClasses();
        for (C messageClass : messageClasses) {
            dispatchers.put(messageClass, dispatcher);
        }
    }

    /**
     * Removes registration for the passed dispatcher.
     *
     * @see #register(MessageDispatcher)
     */
    protected void unregister(D dispatcher) {
        checkNotNull(dispatcher);
        checkNotEmpty(dispatcher);

        Set<C> messageClasses = dispatcher.getMessageClasses();
        for (C messageClass : messageClasses) {
            dispatchers.remove(messageClass, dispatcher);
        }
    }

    /**
     * Unregisters all dispatchers.
     */
    protected void unregisterAll() {
        dispatchers.clear();
    }

    /**
     * Obtains message classes from all registered dispatchers.
     */
    protected Set<C> getRegisteredMessageClasses() {
        return dispatchers.keySet();
    }

    /**
     * Obtains dispatchers for the passed message class.
     *
     * @param messageClass the
     * @return a set of dispatchers or an empty set if no dispatchers are registered
     */
    protected Set<D> getDispatchers(C messageClass) {
        checkNotNull(messageClass);
        Collection<D> dispatchers = this.dispatchers.get(messageClass);
        return ImmutableSet.copyOf(dispatchers);
    }

    /**
     * Obtains a single dispatcher (if available) for the passed message class.
     *
     * @throws IllegalStateException
     *         if more than one dispatcher is found
     * @apiNote This method must be called only for serving {@link UnicastBus}es.
     */
    protected Optional<? extends D> getDispatcher(C messageClass) {
        Set<D> dispatchers = getDispatchers(messageClass);
        if (dispatchers.isEmpty()) {
            return Optional.empty();
        }

        int size = dispatchers.size();
        checkState(size == 1,
                   "More than one (%s) dispatchers found for the message class `%s`.",
                   size, messageClass);

        // Since there can be only one dispatcher per message class, and the set is not empty,
        // we get the single element.
        D result = dispatchers.stream()
                              .findFirst()
                              .get();
        return Optional.of(result);
    }

    /**
     * Ensures that the passed dispatcher is valid.
     *
     * <p>The passed dispatcher must {@linkplain MessageDispatcher#getMessageClasses() expose}
     * at least one message class.
     *
     * @param dispatcher the dispatcher to check
     * @throws IllegalArgumentException if the check is failed
     */
    protected void checkDispatcher(D dispatcher) throws IllegalArgumentException {
        checkNotNull(dispatcher);
        checkNotEmpty(dispatcher);
    }

    private static <D extends MessageDispatcher> void checkNotEmpty(D dispatcher) {
        Set<?> messageClasses = dispatcher.getMessageClasses();
        checkArgument(!messageClasses.isEmpty(),
                      "The dispatcher (%s) has empty message class set.",
                      dispatcher);
    }
}
