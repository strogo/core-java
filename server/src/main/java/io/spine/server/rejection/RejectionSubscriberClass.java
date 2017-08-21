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

package io.spine.server.rejection;

import io.spine.annotation.Internal;
import io.spine.core.RejectionClass;
import io.spine.server.model.HandlerClass;
import io.spine.server.model.MessageHandlerMap;

import java.util.Set;

/**
 * Provides type information on a {@link RejectionSubscriber} class.
 *
 * @param <S> the type of rejection subscribers
 * @author Alexander Yevsyukov
 */
@Internal
public final class RejectionSubscriberClass<S extends RejectionSubscriber> extends HandlerClass<S> {

    private static final long serialVersionUID = 0L;

    private final
    MessageHandlerMap<RejectionClass, RejectionSubscriberMethod> rejectionSubscriptions;

    private RejectionSubscriberClass(Class<? extends S> cls) {
        super(cls);
        rejectionSubscriptions = new MessageHandlerMap<>(cls, RejectionSubscriberMethod.factory());
    }

    /**
     * Creates new instance for the passed class value.
     */
    public static <S extends RejectionSubscriber> RejectionSubscriberClass<S> of(Class<S> cls) {
        return new RejectionSubscriberClass<>(cls);
    }

    Set<RejectionClass> getRejectionSubscriptions() {
        return rejectionSubscriptions.getMessageClasses();
    }

    RejectionSubscriberMethod getSubscriber(RejectionClass cls) {
        return rejectionSubscriptions.getMethod(cls);
    }
}