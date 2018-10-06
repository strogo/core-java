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

package io.spine.system.server;

import com.google.protobuf.Any;
import io.spine.base.CommandMessage;
import io.spine.base.EventMessage;
import io.spine.client.Query;
import io.spine.core.TenantId;
import io.spine.server.tenant.TenantAwareOperation;
import io.spine.server.tenant.TenantAwareRunner;

import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link SystemGateway} which works for a specified tenant.
 *
 * <p>A system command is {@linkplain #postCommand(CommandMessage) posted} within
 * a {@link TenantAwareOperation} with the given tenant set.
 *
 * <p>Any call to this gateway is delegated to another instance of {@link SystemGateway}
 * passed on construction.
 *
 * @author Dmytro Dashenkov
 * @see TenantAwareOperation
 */
final class TenantAwareSystemGateway implements SystemGateway {

    private final SystemGateway delegate;
    private final TenantAwareRunner runner;

    private TenantAwareSystemGateway(TenantId tenantId, SystemGateway delegate) {
        this.runner = TenantAwareRunner.with(checkNotNull(tenantId));
        this.delegate = checkNotNull(delegate);
    }

    static SystemGateway forTenant(TenantId tenantId, SystemGateway systemGateway) {
        checkNotNull(tenantId);
        checkNotNull(systemGateway);
        SystemGateway result = new TenantAwareSystemGateway(tenantId, systemGateway);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the given system command under the context of the specified tenant.
     */
    @Override
    public void postCommand(CommandMessage systemCommand) {
        runner.run(() -> delegate.postCommand(systemCommand));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Posts the given system event under the context of the specified tenant.
     */
    @Override
    public void postEvent(EventMessage systemEvent) {
        runner.run(() -> delegate.postEvent(systemEvent));
    }

    @Override
    public Iterator<Any> readDomainAggregate(Query query) {
        return runner.evaluate(() -> delegate.readDomainAggregate(query));
    }
}