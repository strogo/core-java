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

package io.spine.testing.server;

import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.InProcessSharding;
import io.spine.server.delivery.Sharding;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A test suite {@link org.junit.jupiter.api.extension.Extension Extension}, which resets
 * the sharding configuration to {@link InProcessSharding} before and after each test case.
 *
 * <p>To apply the extension, declare it with
 * the {@link org.junit.jupiter.api.extension.ExtendWith @ExtendWith} annotation as follows:
 * <pre>
 *     {@code
 *     \@ExtendWith(ShardingReset.class)
 *     class MySuiteTest {
 *
 *         \@Test
 *         void testCase() {
 *             // ...
 *         }
 *     }
 *     }
 * </pre>
 *
 * @author Dmytro Dashenkov
 */
public final class ShardingReset implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        reset();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        reset();
    }

    private static void reset() {
        ServerEnvironment serverEnvironment = ServerEnvironment.getInstance();
        Sharding sharding = new InProcessSharding(InMemoryTransportFactory.newInstance());
        serverEnvironment.replaceSharding(sharding);
    }
}
