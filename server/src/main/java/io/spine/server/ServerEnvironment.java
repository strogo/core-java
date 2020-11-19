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

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.base.Environment;
import io.spine.base.EnvironmentType;
import io.spine.base.Identifier;
import io.spine.base.Production;
import io.spine.base.Tests;
import io.spine.reflect.Invokables;
import io.spine.server.commandbus.CommandScheduler;
import io.spine.server.commandbus.ExecutorCommandScheduler;
import io.spine.server.delivery.Delivery;
import io.spine.server.storage.StorageFactory;
import io.spine.server.storage.memory.InMemoryStorageFactory;
import io.spine.server.storage.system.SystemAwareStorageFactory;
import io.spine.server.trace.TracerFactory;
import io.spine.server.transport.TransportFactory;
import io.spine.server.transport.memory.InMemoryTransportFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.storage.system.SystemAwareStorageFactory.wrap;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * The server conditions and configuration under which the application operates.
 *
 * <h3>Configuration</h3>
 * 
 * <p>Some parts of the {@code ServerEnvironment} can be customized based on the {@code
 * EnvironmentType}. To do so, one of the overloads of the {@code use} method can be called.
 * Two environment types exist out of the box: {@link Tests} and {@link Production}.
 * For example:
 * <pre>
 *
 *     ServerEnvironment.when(Production.class)
 *                      .use(productionStorageFactory)
 *                      .use(memoizingTracerFactory);
 *     ServerEnvironment.when(Tests.class)
 *                      .use(testingStorageFactory);
 * </pre>
 *
 * <p>A custom environment type may also be used:
 * <pre>
 *
 *     final class StagingEnvironment extends EnvironmentType {
 *         ...
 *     }
 *
 *     ServerEnvironment.when(Staging.class)
 *                      .use(inMemoryStorageFactory);
 * </pre>
 * If {@code Staging} is {@link Environment#type() enabled}, the specified value is going to be
 * returned on {@link #storageFactory()}.
 */
@SuppressWarnings("ClassWithTooManyMethods" /* there are some deprecated methods to be eliminated later. */)
public final class ServerEnvironment implements AutoCloseable {

    private static final ServerEnvironment INSTANCE = new ServerEnvironment();

    /**
     * The deployment detector is instantiated with a system {@link DeploymentDetector} and
     * can be reassigned the value using {@link #configureDeployment(Supplier)}.
     *
     * <p>Value from this supplier are used to {@linkplain #deploymentType()
     * get the deployment type}.
     */
    private Supplier<DeploymentType> deploymentDetector = DeploymentDetector.newInstance();

    /**
     * The identifier of the server instance running in scope of this application.
     *
     * <p>It is currently impossible to set the node identifier directly.
     * This is a subject to change in the future framework versions.
     */
    private final NodeId nodeId;

    /**
     * The strategy of delivering the messages received by entity repositories
     * to the entity instances.
     *
     * <p>If not {@linkplain #use(Delivery, EnvironmentType)}) configured by the end-user},
     * initialized with the {@linkplain Delivery#local() local} delivery by default.
     *
     * <p>Differs between {@linkplain EnvironmentType environment types}.
     */
    private final EnvSetting<Delivery> delivery = new EnvSetting<>();

    /**
     * Storage factory setting.
     *
     * <p>Defaults to an {@code InMemoryStorageFactory} for tests.
     */
    private final EnvSetting<StorageFactory> storageFactory =
            new EnvSetting<>(Tests.class, () -> wrap(InMemoryStorageFactory.newInstance()));

    /**
     * The setting for the factory of {@code Tracer}s.
     */
    private final EnvSetting<TracerFactory> tracerFactory = new EnvSetting<>();

    /**
     * The setting for the factory for channel-based transport.
     *
     * <p>Defaults to an {@code InMemoryTransportFactory} for tests.
     */
    private final EnvSetting<TransportFactory> transportFactory =
            new EnvSetting<>(Tests.class, InMemoryTransportFactory::newInstance);

    /**
     * Provides schedulers used by all {@code CommandBus} instances of this environment.
     */
    private Supplier<CommandScheduler> commandScheduler;

    private ServerEnvironment() {
        nodeId = NodeId.newBuilder()
                       .setValue(Identifier.newUuid())
                       .vBuild();
        commandScheduler = ExecutorCommandScheduler::new;
    }

    /**
     * Returns a singleton instance.
     */
    public static ServerEnvironment instance() {
        return INSTANCE;
    }

    /**
     * The type of the environment application is deployed to.
     */
    public DeploymentType deploymentType() {
        return deploymentDetector.get();
    }

    /**
     * Returns the delivery mechanism specific to this environment.
     *
     * <p>Unless {@linkplain #use(Delivery, EnvironmentType) updated manually}, returns
     * a {@linkplain Delivery#local() local implementation} of {@code Delivery}.
     */
    public synchronized Delivery delivery() {
        Class<? extends EnvironmentType> currentEnv = environment().type();
        Optional<Delivery> currentDelivery = this.delivery.optionalValue(currentEnv);
        if (currentDelivery.isPresent()) {
            return currentDelivery.get();
        }
        Delivery localDelivery = Delivery.local();
        this.delivery.use(localDelivery, currentEnv);
        return localDelivery;
    }

    /**
     * Obtains command scheduling mechanism used by {@code CommandBus} in this environment.
     */
    public CommandScheduler newCommandScheduler() {
        return commandScheduler.get();
    }

    /**
     * Assigns command scheduling mechanism used at this environment by all
     * {@code CommandBus} instances.
     *
     * <p>If not configured, {@link ExecutorCommandScheduler} is used.
     */
    public void scheduleCommandsUsing(Supplier<CommandScheduler> commandScheduler) {
        checkNotNull(commandScheduler);
        this.commandScheduler = commandScheduler;
    }

    /**
     * Obtains the identifier of the server node, on which this code is running at the moment.
     *
     * <p>At the moment, the node identifier is always UUID-generated. In future versions of the
     * framework it is expected to become configurable.
     */
    public NodeId nodeId() {
        return nodeId;
    }

    /**
     * Sets the default {@linkplain DeploymentType deployment type}
     * {@linkplain Supplier supplier} which utilizes system properties.
     */
    private void resetDeploymentType() {
        Supplier<DeploymentType> supplier = DeploymentDetector.newInstance();
        configureDeployment(supplier);
    }

    /**
     * Makes the {@link #deploymentType()} return the values from the provided supplier.
     *
     * <p>When supplying your own deployment type in tests, remember to
     * {@linkplain #reset() reset it} during tear down.
     */
    @VisibleForTesting
    public void configureDeployment(Supplier<DeploymentType> supplier) {
        checkNotNull(supplier);
        deploymentDetector = supplier;
    }

    /**
     * Assigns the specified {@code TransportFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(StorageFactory) use(StorageFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(StorageFactory factory, EnvironmentType envType) {
        checkNotNull(factory);
        SystemAwareStorageFactory wrapped = wrap(factory);
        storageFactory.registerTypeAndUse(envType, wrapped);
        return this;
    }

    /**
     * Assigns the specified {@code TransportFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(StorageFactory) use(StorageFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(StorageFactory factory, Class<? extends EnvironmentType> type) {
        checkNotNull(factory);
        SystemAwareStorageFactory wrapped = wrap(factory);
        storageFactory.use(wrapped, type);
        return this;
    }

    /**
     * Assigns the specified {@code Delivery} for the selected environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(Delivery) use(Delivery)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(Delivery delivery, EnvironmentType envType) {
        this.delivery.registerTypeAndUse(envType, delivery);
        return this;
    }

    /**
     * Assigns the specified {@code Delivery} for the selected environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(Delivery) use(Delivery)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(Delivery delivery, Class<? extends EnvironmentType> envType) {
        this.delivery.use(delivery, envType);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(TracerFactory) use(TracerFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory factory, EnvironmentType envType) {
        tracerFactory.registerTypeAndUse(envType, factory);
        return this;
    }

    /**
     * Assigns {@code TracerFactory} for the specified application environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(TracerFactory) use(TracerFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(TracerFactory factory, Class<? extends EnvironmentType> envType) {
        tracerFactory.use(factory, envType);
        return this;
    }

    /**
     * Configures the specified transport factory for the selected type of environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(TransportFactory) use(TransportFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory factory, EnvironmentType envType) {
        transportFactory.registerTypeAndUse(envType, factory);
        return this;
    }

    /**
     * Configures the specified transport factory for the selected type of environment.
     *
     * @return this instance of {@code ServerEnvironment}
     * @deprecated please use {@link #when(Class) when(Class<? extends EnvironmentType>)}
     *         followed by {@link TypeConfigurator#use(TransportFactory) use(TransportFactory)}
     */
    @Deprecated
    @CanIgnoreReturnValue
    public ServerEnvironment use(TransportFactory factory, Class<? extends EnvironmentType> type) {
        transportFactory.use(factory, type);
        return this;
    }

    /**
     * Obtains the {@link TracerFactory} associated with the current environment, if it was set.
     */
    public Optional<TracerFactory> tracing() {
        Class<? extends EnvironmentType> currentType = environment().type();
        return this.tracerFactory.optionalValue(currentType);
    }

    /**
     * Obtains the storage factory for the current environment.
     *
     * <p>For tests, if the value was not set, defaults to a new {@code InMemoryStorageFactory}.
     *
     * <p>For other environments, if the value was not set, throws a {@code IllegalStateException}.
     *
     * @return {@code StorageFactory} instance for the storage for the current environment
     * @throws IllegalStateException
     *         if the value {@code StorageFactory} was not
     *         {@linkplain TypeConfigurator#use(StorageFactory) configured} prior to the call
     */
    public StorageFactory storageFactory() {
        Class<? extends EnvironmentType> type = environment().type();
        StorageFactory result = storageFactory.optionalValue(type)
                .orElseThrow(() -> newIllegalStateException(
                        "The storage factory for environment `%s` was not configured."
                                + " Please call `.when(environmentType).use(storageFactory)`.",
                        type.getSimpleName()));
        return result;
    }

    /**
     * Returns a storage factory for the current environment, or an empty {@code Optional} if it
     * was not configured.
     */
    @Internal
    public Optional<StorageFactory> optionalStorageFactory() {
        Class<? extends EnvironmentType> type = environment().type();
        return storageFactory.optionalValue(type);
    }

    /**
     * Obtains the transport factory for the current environment.
     *
     * <p>In the {@linkplain Tests testing environment}, if the factory was not assigned, assigns
     * a new {@code InMemoryTransportFactory} and returns it.
     *
     * <p>For all other environment types, throws an {@code IllegalStateException}.
     *
     * <p>If the factory is not assigned in the Tests mode, assigns the instance of
     * {@link InMemoryTransportFactory} and returns it.
     */
    public TransportFactory transportFactory() {
        Class<? extends EnvironmentType> type = environment().type();
        TransportFactory result = transportFactory.optionalValue(type)
                .orElseThrow(() -> newIllegalStateException(
                        "Transport factory is not assigned for the current environment `%s`."
                                + " Please call `.when(environmentType).use(transportFactory)`.",
                        type.getSimpleName()));

        return result;
    }

    private static Environment environment() {
        return Environment.instance();
    }

    /**
     * This is test-only method required for cleaning of the server environment instance in tests.
     */
    @VisibleForTesting
    public void reset() {
        transportFactory.reset();
        tracerFactory.reset();
        storageFactory.reset();
        delivery.reset();
        Class<? extends EnvironmentType> currentEnv = environment().type();
        delivery.use(Delivery.local(), currentEnv);
        resetDeploymentType();
    }

    /**
     * Releases resources associated with this instance.
     */
    @Override
    public void close() throws Exception {
        tracerFactory.apply(AutoCloseable::close);
        transportFactory.apply(AutoCloseable::close);
        storageFactory.apply(AutoCloseable::close);
    }

    /**
     * Starts flowing API chain for configuring {@code ServerEnvironment} for the passed type.
     */
    public static TypeConfigurator when(Class<? extends EnvironmentType> type) {
        checkNotNull(type);
        return new TypeConfigurator(type);
    }

    /**
     * Allows to configure values used by the {@code ServerEnvironment} for the given type.
     */
    public static class TypeConfigurator {

        private final ServerEnvironment se;
        private final Class<? extends EnvironmentType> type;

        private TypeConfigurator(Class<? extends EnvironmentType> type) {
            this.se = instance();
            EnvironmentType et = Invokables.callParameterlessCtor(type);
            Environment.instance()
                       .register(et);
            this.type = checkNotNull(type);
        }

        /**
         * Assigns the specified {@code Delivery} for the selected environment.
         */
        @CanIgnoreReturnValue
        public TypeConfigurator use(Delivery delivery) {
            se.use(delivery, type);
            return this;
        }

        /**
         * Assigns {@code TracerFactory} for the selected environment.
         */
        @CanIgnoreReturnValue
        public TypeConfigurator use(TracerFactory factory) {
            se.tracerFactory.use(factory, type);
            return this;
        }

        /**
         * Configures the specified transport factory for the selected environment.
         */
        @CanIgnoreReturnValue
        public TypeConfigurator use(TransportFactory factory) {
            se.transportFactory.use(factory, type);
            return this;
        }

        /**
         * Assigns the specified {@code TransportFactory} for the selected environment.
         */
        @CanIgnoreReturnValue
        public TypeConfigurator use(StorageFactory factory) {
            SystemAwareStorageFactory wrapped = wrap(factory);
            se.storageFactory.use(wrapped, type);
            return this;
        }
    }
}
