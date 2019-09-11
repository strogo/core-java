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

package io.spine.server.transport.memory.given;

import io.grpc.stub.StreamObserver;
import io.spine.server.integration.ExternalMessage;

import static io.spine.util.Exceptions.newIllegalArgumentException;

public final class SingleThreadInMemSubscriberTestEnv {

    /** Prevents instantiation of this test env class. */
    private SingleThreadInMemSubscriberTestEnv() {
    }

    public static class ThrowingObserver implements StreamObserver<ExternalMessage> {

        private static final String ERROR_MESSAGE = "Observer failed";

        private boolean onNextCalled = false;

        @Override
        public void onNext(ExternalMessage value) {
            onNextCalled = true;
            throw newIllegalArgumentException(ERROR_MESSAGE);
        }

        @Override
        public void onError(Throwable t) {
            // NO-OP.
        }

        @Override
        public void onCompleted() {
            // NO-OP.
        }

        public boolean onNextCalled() {
            return onNextCalled;
        }
    }
}
