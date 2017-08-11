/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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
package io.spine.server.integration;

import com.google.protobuf.Message;
import io.spine.core.BoundedContextId;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventContext;
import io.spine.core.EventEnvelope;
import io.spine.core.ExternalMessageEnvelope;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.core.Rejections.isRejection;

/**
 * @author Alex Tymchenko
 */
class EventBusAdapter extends BusAdapter<EventEnvelope, EventDispatcher<?>>{

    EventBusAdapter(Builder builder) {
        super(builder);
    }

    static Builder builderWith(EventBus eventBus) {
        checkNotNull(eventBus);
        return new Builder(eventBus);
    }

    @Override
    ExternalMessageEnvelope toExternalEnvelope(Message message) {
        final Event event = (Event) message;
        //TODO:2017-08-11:alex.tymchenko: should we inline this method?
        final ExternalMessageEnvelope result = ExternalMessageEnvelope.of(event);
        return result;
    }

    @Override
    ExternalMessageEnvelope markExternal(Message message) {
        final Event event = (Event) message;
        final Event.Builder eventBuilder = event.toBuilder();
        final EventContext modifiedContext = eventBuilder.getContext()
                                                         .toBuilder()
                                                         .setExternal(true)
                                                         .build();

        final Event marked = eventBuilder.setContext(modifiedContext)
                                         .build();
        return toExternalEnvelope(marked);
    }

    @Override
    boolean accepts(Class<? extends Message> messageClass) {
        checkNotNull(messageClass);
        //TODO:2017-08-11:alex.tymchenko: use app model instead, once it's available.
        return !isRejection(messageClass);
    }

    @Override
    EventDispatcher<?> createDispatcher(Class<? extends Message> messageClass,
                                        BoundedContextId boundedContextId) {
        final LocalEventSubscriber result =
                new LocalEventSubscriber(boundedContextId,
                                         getPublisherHub(),
                                         EventClass.of(messageClass));
        return result;
    }

    static class Builder extends AbstractBuilder<Builder, EventEnvelope, EventDispatcher<?>> {

        Builder(EventBus eventBus) {
            super(eventBus);
        }

        @Override
        protected EventBusAdapter doBuild() {
            return new EventBusAdapter(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
