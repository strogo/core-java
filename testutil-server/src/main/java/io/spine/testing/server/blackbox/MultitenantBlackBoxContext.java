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

package io.spine.testing.server.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.core.TenantId;
import io.spine.server.event.Enricher;
import io.spine.server.tenant.TenantAwareRunner;
import io.spine.testing.client.TestActorRequestFactory;
import io.spine.testing.client.blackbox.Acknowledgements;
import io.spine.testing.client.blackbox.VerifyAcknowledgements;
import io.spine.testing.server.blackbox.verify.state.VerifyState;

import static com.google.common.base.Preconditions.checkState;

/**
 * A black box bounded context for writing integration tests in a multitenant environment.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
@VisibleForTesting
public class MultitenantBlackBoxContext
        extends BlackBoxBoundedContext<MultitenantBlackBoxContext> {

    private TenantId tenantId;

    /**
     * Creates a new multi-tenant instance.
     */
    MultitenantBlackBoxContext(Enricher enricher) {
        super(true, enricher);
    }

    /**
     * Switches the bounded context to operate on behalf of the specified tenant.
     *
     * @param tenant new tenant ID
     * @return current instance
     */
    public MultitenantBlackBoxContext withTenant(TenantId tenant) {
        this.tenantId = tenant;
        return this;
    }

    /**
     * Verifies emitted events by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the events emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyEvents verifier) {
        EmittedEvents events = emittedEvents(tenantId());
        verifier.verify(events);
        return this;
    }

    /**
     * Executes the provided verifier, which throws an assertion error in case of
     * unexpected results.
     *
     * @param verifier
     *         a verifier that checks the acknowledgements in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyAcknowledgements verifier) {
        Acknowledgements acks = output().commandAcks();
        verifier.verify(acks);
        return this;
    }

    /**
     * Verifies emitted commands by the passed verifier.
     *
     * @param verifier
     *         a verifier that checks the commands emitted in this Bounded Context
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyCommands verifier) {
        EmittedCommands commands = output().emittedCommands();
        verifier.verify(commands);
        return this;
    }

    /**
     * Asserts the state of an entity using the specified tenant ID.
     *
     * @param verifier
     *         a verifier of entity states
     * @return current instance
     */
    @CanIgnoreReturnValue
    public MultitenantBlackBoxContext assertThat(VerifyState verifier) {
        verifier.verify(boundedContext(), requestFactory().query());
        return this;
    }

    @Override
    protected TestActorRequestFactory requestFactory() {
        return requestFactory(tenantId());
    }

    private TenantId tenantId() {
        checkState(tenantId != null,
                   "Set a tenant ID before calling receive and assert methods");
        return tenantId;
    }

    /**
     * Reads all events from the bounded context for the provided tenant.
     */
    private EmittedEvents emittedEvents(TenantId tenantId) {
        TenantAwareRunner tenantAwareRunner = TenantAwareRunner.with(tenantId);
        EmittedEvents events = tenantAwareRunner.evaluate(() -> output().emittedEvents());
        return events;
    }

    /**
     * Creates a new {@link io.spine.client.ActorRequestFactory actor request factory} for tests
     * with a provided tenant ID.
     *
     * @param tenantId
     *         an identifier of a tenant that is executing requests in this Bounded Context
     * @return a new request factory instance
     */
    private static TestActorRequestFactory requestFactory(TenantId tenantId) {
        return TestActorRequestFactory.newInstance(MultitenantBlackBoxContext.class, tenantId);
    }
}
