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

package io.spine.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.spine.annotation.Internal;
import io.spine.core.BoundedContextName;
import io.spine.core.BoundedContextNames;
import io.spine.logging.Logging;
import io.spine.server.aggregate.AggregateRootDirectory;
import io.spine.server.aggregate.InMemoryRootDirectory;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.commandbus.CommandDispatcher;
import io.spine.server.enrich.Enricher;
import io.spine.server.entity.Entity;
import io.spine.server.entity.Repository;
import io.spine.server.event.EventBus;
import io.spine.server.event.EventDispatcher;
import io.spine.server.event.EventEnricher;
import io.spine.server.integration.IntegrationBus;
import io.spine.server.stand.Stand;
import io.spine.server.tenant.TenantIndex;
import io.spine.system.server.NoOpSystemClient;
import io.spine.system.server.SystemClient;
import io.spine.system.server.SystemContext;
import io.spine.system.server.SystemReadSide;
import io.spine.system.server.SystemWriteSide;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static io.spine.core.BoundedContextNames.assumingTestsValue;
import static io.spine.server.ContextSpec.multitenant;
import static io.spine.server.ContextSpec.singleTenant;

/**
 * A builder for producing {@code BoundedContext} instances.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})
// OK for this central piece.
public final class BoundedContextBuilder implements Logging {

    private final ContextSpec spec;
    private CommandBus.Builder commandBus;
    private EventBus.Builder eventBus;
    private EventEnricher eventEnricher;
    private Stand.Builder stand;
    private IntegrationBus.Builder integrationBus;
    private Supplier<AggregateRootDirectory> rootDirectory;
    private TenantIndex tenantIndex;

    /** Repositories to be registered with the Bounded Context being built after its creation. */
    private final Collection<Repository<?, ?>> repositories = new ArrayList<>();

    /**
     * Command dispatchers to be registered with the context {@link CommandBus} after the Bounded
     * Context creation.
     */
    private final Collection<CommandDispatcher<?>> commandDispatchers = new ArrayList<>();

    /**
     * Event dispatchers to be registered with the context {@link EventBus} and/or
     * {@link IntegrationBus} after the Bounded Context creation.
     */
    private final Collection<EventDispatcher<?>> eventDispatchers = new ArrayList<>();

    /**
     * Prevents direct instantiation.
     *
     * @param spec
     *         the context spec for the built context
     * @see BoundedContext#singleTenant
     * @see BoundedContext#multitenant
     */
    BoundedContextBuilder(ContextSpec spec) {
        this.spec = checkNotNull(spec);
    }

    /**
     * Creates a new builder for a test-only bounded context.
     */
    @Internal
    @VisibleForTesting
    public static BoundedContextBuilder assumingTests(boolean multitenant) {
        ContextSpec spec = multitenant
                           ? multitenant(assumingTestsValue())
                           : singleTenant(assumingTestsValue());
        return new BoundedContextBuilder(spec);
    }

    /**
     * Creates a new builder for a single tenant test-only Bounded Context.
     */
    @Internal
    @VisibleForTesting
    public static BoundedContextBuilder assumingTests() {
        return assumingTests(false);
    }

    /**
     * Obtains the context spec.
     */
    public ContextSpec spec() {
        return spec;
    }

    /**
     * Returns the name of the resulting context.
     */
    public BoundedContextName name() {
        return spec.name();
    }

    public boolean isMultitenant() {
        return spec.isMultitenant();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setCommandBus(CommandBus.Builder commandBus) {
        this.commandBus = checkNotNull(commandBus);
        return this;
    }

    Optional<IntegrationBus.Builder> integrationBus() {
        return Optional.ofNullable(integrationBus);
    }

    public Optional<CommandBus.Builder> commandBus() {
        return Optional.ofNullable(commandBus);
    }

    /**
     * Obtains {@code TenantIndex} implementation associated with the Bounded Context.
     */
    public Optional<? extends TenantIndex> tenantIndex() {
        return Optional.ofNullable(tenantIndex);
    }

    TenantIndex buildTenantIndex() {
        TenantIndex result = isMultitenant()
            ? checkNotNull(tenantIndex)
            : TenantIndex.singleTenant();
        return result;
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setEventBus(EventBus.Builder eventBus) {
        this.eventBus = checkNotNull(eventBus);
        return this;
    }

    public Optional<EventBus.Builder> eventBus() {
        return Optional.ofNullable(eventBus);
    }

    /**
     * Sets a custom {@link Enricher} for events posted to
     * the {@code EventBus} of the context being built.
     *
     * <p>If the {@code Enricher} is not set, the enrichments
     * will <strong>NOT</strong> be supported in this context.
     *
     * @param enricher
     *         the {@code Enricher} for events or {@code null} if enrichment is not supported
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder enrichEventsUsing(EventEnricher enricher) {
        this.eventEnricher = checkNotNull(enricher);
        return this;
    }

    /**
     * Obtains {@code EventEnricher} assigned to the context to be built, or
     * empty {@code Optional} if no enricher was assigned prior to this call.
     */
    public Optional<EventEnricher> eventEnricher() {
        return Optional.ofNullable(eventEnricher);
    }

    EventBus buildEventBus(BoundedContext context) {
        checkNotNull(context);
        eventBus.injectContext(context);
        if (eventEnricher != null) {
            eventBus.injectEnricher(eventEnricher);
        }
        return eventBus.build();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setStand(Stand.Builder stand) {
        this.stand = checkNotNull(stand);
        return this;
    }

    public Optional<Stand.Builder> stand() {
        return Optional.ofNullable(stand);
    }

    Stand buildStand() {
        return stand.build();
    }

    @CanIgnoreReturnValue
    public BoundedContextBuilder setTenantIndex(TenantIndex tenantIndex) {
        if (isMultitenant()) {
            checkNotNull(tenantIndex,
                         "`%s` cannot be null in a multitenant `BoundedContext`.",
                         TenantIndex.class.getSimpleName());
        }
        this.tenantIndex = tenantIndex;
        return this;
    }

    /**
     * Adds the passed command dispatcher to the dispatcher registration list which will be
     * processed after the Bounded Context is created.
     *
     * @apiNote This method is also capable of registering {@linkplain Repository repositories}
     *         that implement {@code CommandDispatcher}, but the {@link #add(Repository)} method
     *         should be preferred for this purpose.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder addCommandDispatcher(CommandDispatcher<?> commandDispatcher) {
        checkNotNull(commandDispatcher);
        if (commandDispatcher instanceof Repository) {
            return add((Repository<?, ?>) commandDispatcher);
        }
        commandDispatchers.add(commandDispatcher);
        return this;
    }

    /**
     * Adds the passed event dispatcher to the dispatcher registration list which will be processed
     * after the Bounded Context is created.
     *
     * @apiNote This method is also capable of registering {@linkplain Repository repositories}
     *         that implement {@code EventDispatcher}, but the {@link #add(Repository)} method
     *         should be preferred for this purpose.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder addEventDispatcher(EventDispatcher<?> eventDispatcher) {
        checkNotNull(eventDispatcher);
        if (eventDispatcher instanceof Repository) {
            return add((Repository<?, ?>) eventDispatcher);
        }
        eventDispatchers.add(eventDispatcher);
        return this;
    }

    /**
     * Adds the {@linkplain DefaultRepository default repository} for the passed entity class to
     * the repository registration list.
     */
    @CanIgnoreReturnValue
    public <I, E extends Entity<I, ?>> BoundedContextBuilder add(Class<E> entityClass) {
        checkNotNull(entityClass);
        return add(DefaultRepository.of(entityClass));
    }

    /**
     * Adds the passed repository to the registration list which will be processed after
     * the Bounded Context is created.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder add(Repository<?, ?> repository) {
        checkNotNull(repository);
        repositories.add(repository);
        return this;
    }

    /**
     * Removes the passed command dispatcher from the corresponding registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder removeCommandDispatcher(CommandDispatcher<?> commandDispatcher) {
        checkNotNull(commandDispatcher);
        if (commandDispatcher instanceof Repository) {
            return remove((Repository<?, ?>) commandDispatcher);
        }
        commandDispatchers.remove(commandDispatcher);
        return this;
    }

    /**
     * Removes the passed event dispatcher from the corresponding registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder removeEventDispatcher(EventDispatcher<?> eventDispatcher) {
        checkNotNull(eventDispatcher);
        if (eventDispatcher instanceof Repository) {
            return remove((Repository<?, ?>) eventDispatcher);
        }
        eventDispatchers.remove(eventDispatcher);
        return this;
    }

    /**
     * Removes the repository from the registration list by the passed entity class.
     */
    @CanIgnoreReturnValue
    public <I, E extends Entity<I, ?>> BoundedContextBuilder remove(Class<E> entityClass) {
        checkNotNull(entityClass);
        repositories.removeIf(repository -> repository.entityClass()
                                                      .equals(entityClass));
        return this;
    }

    /**
     * Removes the passed repository from the registration list.
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder remove(Repository<?, ?> repository) {
        checkNotNull(repository);
        repositories.remove(repository);
        return this;
    }

    /**
     * Verifies if the passed command dispatcher was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasCommandDispatcher(CommandDispatcher<?> commandDispatcher) {
        checkNotNull(commandDispatcher);
        if (commandDispatcher instanceof Repository) {
            return hasRepository((Repository<?, ?>) commandDispatcher);
        }
        boolean result = commandDispatchers.contains(commandDispatcher);
        return result;
    }

    /**
     * Verifies if the passed event dispatcher was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasEventDispatcher(EventDispatcher<?> eventDispatcher) {
        checkNotNull(eventDispatcher);
        if (eventDispatcher instanceof Repository) {
            return hasRepository((Repository<?, ?>) eventDispatcher);
        }
        boolean result = eventDispatchers.contains(eventDispatcher);
        return result;
    }

    /**
     * Verifies if the repository with a passed entity class was previously added into the
     * registration list of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    <I, E extends Entity<I, ?>> boolean hasRepository(Class<E> entityClass) {
        checkNotNull(entityClass);
        boolean result =
                repositories.stream()
                            .anyMatch(repository -> repository.entityClass()
                                                              .equals(entityClass));
        return result;
    }

    /**
     * Verifies if the passed repository was previously added into the registration list
     * of the Bounded Context this builder is going to build.
     */
    @VisibleForTesting
    boolean hasRepository(Repository<?, ?> repository) {
        checkNotNull(repository);
        boolean result = repositories.contains(repository);
        return result;
    }

    /**
     * Obtains the list of repositories added to the builder by the time of the call.
     *
     * <p>Adding repositories to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<Repository<?, ?>> repositories() {
        return ImmutableList.copyOf(repositories);
    }

    /**
     * Obtains the list of command dispatchers added to the builder by the time of the call.
     *
     * <p>Adding dispatchers to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<CommandDispatcher<?>> commandDispatchers() {
        return ImmutableList.copyOf(commandDispatchers);
    }

    /**
     * Obtains the list of event dispatchers added to the builder by the time of the call.
     *
     * <p>Adding dispatchers to the builder after this method returns will not update the
     * returned list.
     */
    public ImmutableList<EventDispatcher<?>> eventDispatchers() {
        return ImmutableList.copyOf(eventDispatchers);
    }

    /**
     * Obtains the {@link AggregateRootDirectory} to be used in the built context.
     *
     * <p>If no custom implementation is specified, an in-mem implementation is used.
     */
    AggregateRootDirectory aggregateRootDirectory() {
        if (rootDirectory == null) {
            rootDirectory = InMemoryRootDirectory::new;
        }
        return rootDirectory.get();
    }

    /**
     * Sets the supplier of {@link AggregateRootDirectory}-s to use in the built context.
     *
     * <p>By default, an in-mem implementation is used.
     *
     * @param directory
     *         the supplier of aggregate root directories
     */
    @CanIgnoreReturnValue
    public BoundedContextBuilder
    setAggregateRootDirectory(Supplier<AggregateRootDirectory> directory) {
        this.rootDirectory = checkNotNull(directory);
        return this;
    }

    /**
     * Creates a new instance of {@code BoundedContext} with the set configurations.
     *
     * <p>The resulting domain-specific Bounded Context has as internal System Bounded Context.
     * The entities of the System domain describe the entities of the resulting Bounded Context.
     *
     * <p>The System Bounded Context shares some configuration with the Domain Bounded Context,
     * such as:
     * <ul>
     *     <li>{@linkplain #tenantIndex()} tenancy;
     *     <li>{@linkplain ServerEnvironment#transportFactory()} transport facilities.
     * </ul>
     *
     * <p>All the other configuration is NOT shared.
     *
     * <p>The name of the System Bounded Context is derived from the name of the Domain Bounded
     * Context.
     *
     * @return new {@code BoundedContext}
     */
    public BoundedContext build() {
        SystemContext system = buildSystem();
        BoundedContext result = buildDomain(system);
        log().debug("{} created.", result.nameForLogging());

        registerRepositories(result);
        registerDispatchers(result);
        return result;
    }

    private void registerRepositories(BoundedContext result) {
        for (Repository<?, ?> repository : repositories) {
            result.register(repository);
            log().debug("{} registered.", repository);
        }
    }

    private void registerDispatchers(BoundedContext result) {
        commandDispatchers.forEach(result::registerCommandDispatcher);
        eventDispatchers.forEach(result::registerEventDispatcher);
    }

    private BoundedContext buildDomain(SystemContext system) {
        SystemClient systemClient = system.createClient();
        Function<BoundedContextBuilder, DomainContext> instanceFactory =
                builder -> DomainContext.newInstance(builder, systemClient);
        BoundedContext result = buildPartial(instanceFactory, systemClient);
        return result;
    }

    private SystemContext buildSystem() {
        String name = BoundedContextNames.system(spec.name()).getValue();
        boolean multitenant = isMultitenant();
        BoundedContextBuilder system = multitenant
                                       ? BoundedContext.multitenant(name)
                                       : BoundedContext.singleTenant(name);
        Optional<? extends TenantIndex> tenantIndex = tenantIndex();
        tenantIndex.ifPresent(system::setTenantIndex);

        SystemContext result = system.buildPartial(SystemContext::newInstance,
                                                   NoOpSystemClient.INSTANCE
        );
        return result;
    }

    private <B extends BoundedContext>
    B buildPartial(Function<BoundedContextBuilder, B> instanceFactory, SystemClient client) {
        initTenantIndex();
        initCommandBus(client.writeSide());
        initEventBus();
        initStand(client.readSide());
        initIntegrationBus();

        B result = instanceFactory.apply(this);
        return result;
    }

    private void initTenantIndex() {
        if (tenantIndex == null) {
            tenantIndex = isMultitenant()
                          ? TenantIndex.createDefault()
                          : TenantIndex.singleTenant();
        }
    }

    private void initCommandBus(SystemWriteSide systemWriteSide) {
        if (commandBus == null) {
            commandBus = CommandBus.newBuilder()
                                   .setMultitenant(isMultitenant());
        } else {
            Boolean commandBusMultitenancy = commandBus.isMultitenant();
            if (commandBusMultitenancy != null) {
                checkSameValue("CommandBus must match multitenancy of BoundedContext. " +
                                       "Status in BoundedContextBuilder: %s CommandBus: %s",
                               commandBusMultitenancy);
            } else {
                commandBus.setMultitenant(isMultitenant());
            }
        }
        commandBus.injectSystem(systemWriteSide)
                  .injectTenantIndex(tenantIndex);
    }

    private void initEventBus() {
        if (eventBus == null) {
            eventBus = EventBus.newBuilder();
        }
    }

    private void initStand(SystemReadSide systemReadSide) {
        if (stand == null) {
            stand = createStand();
        } else {
            Boolean standMultitenant = stand.isMultitenant();
            // Check that both either multi-tenant or single-tenant.
            if (standMultitenant == null) {
                stand.setMultitenant(isMultitenant());
            } else {
                checkSameValue("`Stand` must match multitenancy of `BoundedContext`. " +
                                       "Status in `BoundedContext.Builder`: %s, `Stand`: %s.",
                               standMultitenant);
            }
        }
        stand.setSystemReadSide(systemReadSide);
    }

    private void initIntegrationBus() {
        integrationBus = IntegrationBus.newBuilder();
    }

    /**
     * Ensures that the value of the passed flag is equal to the value of
     * the {@link BoundedContextBuilder#isMultitenant()}.
     *
     * @throws IllegalStateException if the flags values do not match
     */
    private void checkSameValue(String errMsgFmt, boolean partMultitenancy) {
        boolean multitenant = isMultitenant();
        checkState(multitenant == partMultitenancy,
                   errMsgFmt,
                   String.valueOf(multitenant),
                   String.valueOf(partMultitenancy));
    }

    private Stand.Builder createStand() {
        Stand.Builder result = Stand.newBuilder()
                                    .setMultitenant(isMultitenant());
        return result;
    }
}
