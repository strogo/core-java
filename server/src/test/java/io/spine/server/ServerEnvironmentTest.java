/*
 * Copyright 2020, TeamDev. All rights reserved.
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

package io.spine.server;

import io.spine.base.Environment;
import io.spine.base.EnvironmentType;
import io.spine.base.Production;
import io.spine.base.Tests;
import io.spine.server.delivery.Delivery;
import io.spine.server.delivery.UniformAcrossAllShards;
import io.spine.server.given.environment.Local;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.storage.system.given.MemoizingStorageFactory;
import io.spine.server.trace.given.MemoizingTracerFactory;
import io.spine.server.transport.ChannelId;
import io.spine.server.transport.Publisher;
import io.spine.server.transport.Subscriber;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE;
import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_PATH;
import static io.spine.server.DeploymentDetector.APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE;
import static io.spine.server.DeploymentType.APPENGINE_CLOUD;
import static io.spine.server.DeploymentType.APPENGINE_EMULATOR;
import static io.spine.server.DeploymentType.STANDALONE;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ServerEnvironment should")
class ServerEnvironmentTest {

    private static final ServerEnvironment serverEnvironment = ServerEnvironment.instance();

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(ServerEnvironment.class);
    }

    @Test
    @DisplayName("allow to customize delivery mechanism")
    void allowToCustomizeDeliveryStrategy() {
        Delivery newDelivery = Delivery.newBuilder()
                                       .setStrategy(UniformAcrossAllShards.forNumber(42))
                                       .build();
        ServerEnvironment environment = serverEnvironment;
        Delivery defaultValue = environment.delivery();
        ServerEnvironment.when(Tests.class)
                         .use(newDelivery);
        assertEquals(newDelivery, environment.delivery());

        // Restore the default value.
        ServerEnvironment.when(Tests.class)
                         .use(defaultValue);
    }

    @Test
    @DisplayName("tell when not running without any specific server environment")
    void tellIfStandalone() {
        // Tests are not run by AppEngine by default.
        assertEquals(STANDALONE, serverEnvironment.deploymentType());
    }

    @Nested
    @DisplayName("when running on App Engine cloud infrastructure")
    class OnProdAppEngine extends WithAppEngineEnvironment {

        OnProdAppEngine() {
            super(APP_ENGINE_ENVIRONMENT_PRODUCTION_VALUE);
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE cloud infrastructure server environment")
        void receivesCloudEnvironment() {
            assertEquals(APPENGINE_CLOUD, serverEnvironment.deploymentType());
        }

        @Test
        @DisplayName("cache the property value")
        void cachesValue() {
            assertEquals(APPENGINE_CLOUD, serverEnvironment.deploymentType());
            setGaeEnvironment("Unrecognized Value");
            assertEquals(APPENGINE_CLOUD, serverEnvironment.deploymentType());
        }
    }

    @Nested
    @DisplayName("when running on App Engine local server")
    class OnDevAppEngine extends WithAppEngineEnvironment {

        OnDevAppEngine() {
            super(APP_ENGINE_ENVIRONMENT_DEVELOPMENT_VALUE);
        }

        @Test
        @DisplayName("obtain AppEngine environment GAE local dev server environment")
        void receivesEmulatorEnvironment() {
            assertEquals(APPENGINE_EMULATOR, serverEnvironment.deploymentType());
        }
    }

    @Nested
    @DisplayName("when running with invalid App Engine environment property")
    class InvalidGaeEnvironment extends WithAppEngineEnvironment {

        InvalidGaeEnvironment() {
            super("InvalidGaeEnvironment");
        }

        @Test
        @DisplayName("receive STANDALONE deployment type")
        void receivesStandalone() {
            assertEquals(STANDALONE, serverEnvironment.deploymentType());
        }
    }

    @Nested
    @DisplayName("configure production `StorageFactory`")
    class StorageFactoryConfig {

        private final Environment environment = Environment.instance();
        private final ServerEnvironment serverEnvironment = ServerEnvironment.instance();

        @BeforeEach
        void turnToProduction() {
            // Ensure the server environment is clear.
            serverEnvironment.reset();
            environment.setTo(Production.class);
        }

        @AfterEach
        void backToTests() {
            environment.setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("throwing an `IllegalStateException` if not configured in the Production mode")
        void throwsIfNotConfigured() {
            assertThrows(IllegalStateException.class, serverEnvironment::storageFactory);
        }

        @Test
        @DisplayName("return configured `StorageFactory` when asked in Production")
        void productionFactory() {
            StorageFactory factory = InMemoryStorageFactory.newInstance();
            ServerEnvironment.when(Production.class)
                             .use(factory);
            assertThat(((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate())
                    .isEqualTo(factory);
        }

        @Test
        @DisplayName("return `InMemoryStorageFactory` under Tests")
        void testsFactory() {
            environment.setTo(Tests.class);

            StorageFactory factory = serverEnvironment.storageFactory();
            assertThat(factory)
                    .isInstanceOf(SystemAwareStorageFactory.class);
            SystemAwareStorageFactory systemAware = (SystemAwareStorageFactory) factory;
            assertThat(systemAware.delegate()).isInstanceOf(InMemoryStorageFactory.class);
        }
    }

    @Nested
    @DisplayName("configure `StorageFactory` for tests")
    class TestStorageFactoryConfig {

        @AfterEach
        void resetEnvironment() {
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("wrapping `SystemAwareStorageFactory` over the passed instance")
        void getSet() {
            StorageFactory factory = new MemoizingStorageFactory();
            ServerEnvironment.when(Tests.class)
                             .use(factory);

            StorageFactory delegate =
                    ((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate();
            assertThat(delegate)
                    .isEqualTo(factory);
        }
    }

    @Nested
    @DisplayName("configure `StorageFactory` for a custom environment")
    class LocalStorageFactoryConfig {

        @BeforeEach
        void reset() {
            serverEnvironment.reset();
            Environment.instance()
                       .setTo(Local.class);
            Local.enable();
        }

        @AfterEach
        void backToTests() {
            Environment.instance()
                       .setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("throwing an `IllegalStateException` if not set")
        void illegalState() {
            assertThrows(IllegalStateException.class, serverEnvironment::storageFactory);
        }

        @Test
        @DisplayName("returning a configured wrapped instance")
        void returnConfiguredStorageFactory() {
            InMemoryStorageFactory inMemory = InMemoryStorageFactory.newInstance();
            ServerEnvironment.when(Local.class)
                             .use(inMemory);

            assertThat(((SystemAwareStorageFactory) serverEnvironment.storageFactory()).delegate())
                    .isEqualTo(inMemory);
        }
    }

    @Nested
    @DisplayName("configure `TransportFactory` for the production environment")
    class TransportFactoryConfig {

        private final Environment environment = Environment.instance();

        @BeforeEach
        void turnToProduction() {
            // Ensure the instance is clear.
            serverEnvironment.reset();
            environment.setTo(Production.class);
        }

        @AfterEach
        void backToTests() {
            environment.setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("throw an `IllegalStateException` if not configured")
        void throwsIfNotConfigured() {
            assertThrows(IllegalStateException.class, serverEnvironment::transportFactory);
        }

        @Test
        @DisplayName("return configured instance in Production")
        void productionValue() {
            TransportFactory factory = new StubTransportFactory();
            ServerEnvironment.when(Production.class)
                             .use(factory);
            assertThat(serverEnvironment.transportFactory())
                    .isEqualTo(factory);
        }
    }

    @Nested
    @DisplayName("configure `TransportFactory` for tests")
    class TestTransportFactoryConfig {

        @AfterEach
        void resetEnvironment() {
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("returning one when explicitly set")
        void setExplicitly() {
            TransportFactory factory = new StubTransportFactory();
            ServerEnvironment.when(Tests.class)
                             .use(factory);
            assertThat(serverEnvironment.transportFactory())
                    .isSameInstanceAs(factory);
        }

        @Test
        @DisplayName("returning an `InMemoryTransportFactory` when not set")
        void notSet() {
            Environment.instance()
                       .setTo(Tests.class);
            assertThat(serverEnvironment.transportFactory())
                    .isInstanceOf(InMemoryTransportFactory.class);
        }
    }

    @Nested
    @DisplayName("configure `TransportFactory` for a custom environment")
    class LocalTransportFactory {

        @BeforeEach
        void reset() {
            serverEnvironment.reset();
            Environment.instance()
                       .setTo(Local.class);
            Local.enable();
        }

        @AfterEach
        void backToTests() {
            Environment.instance()
                       .setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("throwing an `IllegalStateException` if not set")
        void illegalState() {
            assertThrows(IllegalStateException.class, serverEnvironment::transportFactory);
        }

        @Test
        @DisplayName("returning a configured instance")
        void ok() {
            InMemoryTransportFactory transportFactory = InMemoryTransportFactory.newInstance();
            ServerEnvironment.when(Local.class)
                             .use(transportFactory);
            assertThat(serverEnvironment.transportFactory())
                    .isSameInstanceAs(transportFactory);
        }
    }

    @Nested
    @DisplayName("configure `TracerFactory`")
    class TracerFactory {

        @BeforeEach
        void reset() {
            serverEnvironment.reset();
            Environment.instance()
                       .reset();
        }

        @AfterEach
        void backToTests() {
            Environment.instance()
                       .setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("returning an instance for the production environment")
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        void forProduction() {
            Environment.instance()
                       .setTo(Production.class);

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            ServerEnvironment.when(Production.class)
                             .use(memoizingTracer);

            assertThat(serverEnvironment.tracing()
                                        .get()).isSameInstanceAs(memoizingTracer);
        }

        @Test
        @DisplayName("for the testing environment")
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        void forTesting() {
            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            ServerEnvironment.when(Tests.class)
                             .use(memoizingTracer);

            assertThat(serverEnvironment.tracing()
                                        .get()).isSameInstanceAs(memoizingTracer);
        }

        @Test
        @DisplayName("for a custom environment")
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        void forCustom() {
            Environment.instance()
                       .setTo(Local.class);

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            ServerEnvironment.when(Local.class)
                             .use(memoizingTracer);

            assertThat(serverEnvironment.tracing()
                                        .get()).isSameInstanceAs(memoizingTracer);
        }

        @Test
        @DisplayName("for a custom environment, returning empty if it's disabled")
        void forCustomEmpty() {
            Local.disable();

            MemoizingTracerFactory memoizingTracer = new MemoizingTracerFactory();
            ServerEnvironment.when(Local.class)
                             .use(memoizingTracer);

            assertThat(serverEnvironment.tracing()).isEmpty();
        }
    }

    @Nested
    @DisplayName("configure `Delivery`")
    class DeliveryTest {

        @BeforeEach
        void reset() {
            serverEnvironment.reset();
            Environment.instance()
                       .reset();
        }

        @AfterEach
        void backToTests() {
            Environment.instance()
                       .setTo(Tests.class);
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("to default back to `Local` if no delivery is set")
        void backToLocal() {
            Delivery delivery = serverEnvironment.delivery();
            assertThat(delivery).isNotNull();
        }

        @Test
        @DisplayName("to a custom mechanism")
        void allowToCustomizeDeliveryStrategy() {
            Delivery newDelivery = Delivery.newBuilder()
                                           .setStrategy(UniformAcrossAllShards.forNumber(42))
                                           .build();
            ServerEnvironment environment = serverEnvironment;
            Delivery defaultValue = environment.delivery();
            Class<? extends EnvironmentType> currentType =
                    Environment.instance()
                               .type();

            ServerEnvironment.when(currentType)
                             .use(newDelivery);

            assertEquals(newDelivery, environment.delivery());

            // Restore the default value.
            ServerEnvironment.when(currentType)
                             .use(defaultValue);
        }

    }

    @Nested
    @DisplayName("while closing resources")
    class TestClosesResources {

        private InMemoryTransportFactory transportFactory;
        private MemoizingStorageFactory storageFactory;
        private MemoizingTracerFactory tracerFactory;

        @BeforeEach
        void setup() {
            transportFactory = InMemoryTransportFactory.newInstance();
            storageFactory = new MemoizingStorageFactory();
            tracerFactory = new MemoizingTracerFactory();
        }

        @AfterEach
        void resetEnvironment() {
            serverEnvironment.reset();
        }

        @Test
        @DisplayName("close the production transport, tracer and storage factories")
        void productionCloses() throws Exception {
            testClosesEnv(Production.class);
        }

        @Test
        @DisplayName("close the testing transport, tracer and storage factories")
        void testCloses() throws Exception {
            testClosesEnv(Tests.class);
        }

        @Test
        @DisplayName("close the custom env transport, tracer and storage factories")
        void customEnvCloses() throws Exception {
            testClosesEnv(Local.class);
        }

        private void testClosesEnv(Class<? extends EnvironmentType> envType) throws Exception {
            ServerEnvironment.when(envType)
                             .use(storageFactory)
                             .use(transportFactory)
                             .use(tracerFactory);

            ServerEnvironment.instance()
                             .close();

            assertThat(transportFactory.isOpen())
                    .isFalse();
            assertThat(storageFactory.isClosed())
                    .isTrue();
            assertThat(tracerFactory.closed())
                    .isTrue();
        }
    }

    @SuppressWarnings({
            "AccessOfSystemProperties" /* Testing the configuration loaded from System properties. */,
            "AbstractClassWithoutAbstractMethods" /* A test base with setUp and tearDown. */
    })

    abstract class WithAppEngineEnvironment {

        private final String targetEnvironment;

        private String initialValue;

        WithAppEngineEnvironment(String targetEnvironment) {
            this.targetEnvironment = targetEnvironment;
        }

        @BeforeEach
        void setUp() {
            initialValue = System.getProperty(APP_ENGINE_ENVIRONMENT_PATH);
            setGaeEnvironment(targetEnvironment);
            serverEnvironment.reset();
        }

        @AfterEach
        void tearDown() {
            if (initialValue == null) {
                System.clearProperty(APP_ENGINE_ENVIRONMENT_PATH);
            } else {
                setGaeEnvironment(initialValue);
            }
            serverEnvironment.reset();
        }

        void setGaeEnvironment(String value) {
            System.setProperty(APP_ENGINE_ENVIRONMENT_PATH, value);
        }
    }

    /**
     * Stub implementation of {@code TransportFactory} which delegates all the calls
     * to {@code InMemoryTransportFactory}.
     */
    private static class StubTransportFactory implements TransportFactory {

        private final TransportFactory delegate = InMemoryTransportFactory.newInstance();

        @Override
        public Publisher createPublisher(ChannelId id) {
            return delegate.createPublisher(id);
        }

        @Override
        public Subscriber createSubscriber(ChannelId id) {
            return delegate.createSubscriber(id);
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }
    }
}
