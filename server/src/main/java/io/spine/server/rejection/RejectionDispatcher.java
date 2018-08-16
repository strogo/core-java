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
package io.spine.server.rejection;

import io.spine.core.RejectionClass;
import io.spine.core.RejectionEnvelope;
import io.spine.server.bus.MulticastDispatcher;
import io.spine.server.integration.ExternalDispatcherFactory;

import java.util.Set;

/**
 * Delivers rejections to corresponding subscribers.
 *
 * @param <I> the type of IDs of entities to which deliver rejections
 * @author Alex Tymchenko
 */
public interface RejectionDispatcher<I>
        extends MulticastDispatcher<RejectionClass, RejectionEnvelope, I>,
                ExternalDispatcherFactory<I> {

    default Set<RejectionClass> getRejectionClasses() {
        return getMessageClasses();
    }

    Set<RejectionClass> getExternalRejectionClasses();

    /**
     * Verifies if this instance dispatches at least one domestic rejection.
     */
    default boolean dispatchesRejections() {
        return !getRejectionClasses().isEmpty();
    }

    /**
     * Verifies if this instance dispatches at least one external rejection.
     */
    default boolean dispatchesExternalRejections() {
        return !getExternalRejectionClasses().isEmpty();
    }
}