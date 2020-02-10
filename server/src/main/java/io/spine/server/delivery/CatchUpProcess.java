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

package io.spine.server.delivery;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import com.google.protobuf.Duration;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import io.spine.annotation.Internal;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.base.Time;
import io.spine.core.Event;
import io.spine.core.EventContext;
import io.spine.grpc.MemoizingObserver;
import io.spine.server.BoundedContext;
import io.spine.server.ServerEnvironment;
import io.spine.server.delivery.event.CatchUpCompleted;
import io.spine.server.delivery.event.CatchUpRequested;
import io.spine.server.delivery.event.CatchUpStarted;
import io.spine.server.delivery.event.HistoryEventsRecalled;
import io.spine.server.delivery.event.HistoryFullyRecalled;
import io.spine.server.delivery.event.LiveEventsPickedUp;
import io.spine.server.delivery.event.ShardProcessingRequested;
import io.spine.server.event.AbstractStatefulReactor;
import io.spine.server.event.EventFactory;
import io.spine.server.event.EventFilter;
import io.spine.server.event.EventStore;
import io.spine.server.event.EventStreamQuery;
import io.spine.server.event.EventStreamQuery.Limit;
import io.spine.server.event.React;
import io.spine.server.projection.ProjectionRepository;
import io.spine.server.tuple.EitherOf2;
import io.spine.server.type.EventEnvelope;
import io.spine.type.TypeUrl;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.protobuf.util.Durations.fromNanos;
import static com.google.protobuf.util.Timestamps.subtract;
import static io.spine.server.delivery.CatchUpMessages.catchUpCompleted;
import static io.spine.server.delivery.CatchUpMessages.fullyRecalled;
import static io.spine.server.delivery.CatchUpMessages.limitOf;
import static io.spine.server.delivery.CatchUpMessages.liveEventsPickedUp;
import static io.spine.server.delivery.CatchUpMessages.recalled;
import static io.spine.server.delivery.CatchUpMessages.shardProcessingRequested;
import static io.spine.server.delivery.CatchUpMessages.started;
import static io.spine.server.delivery.CatchUpMessages.toFilters;
import static io.spine.server.delivery.DeliveryStrategy.newIndex;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * A process that performs a projection catch-up.
 *
 * <p>Starts the catch-up process by emitting the {@link CatchUpRequested} event.
 *
 * <p>Has its own {@link Inbox}, so the messages arriving to it are dispatched by the
 * {@link Delivery}.
 *
 * <p>In its lifecycle, moves through the several statuses.
 *
 * <p><b>{@linkplain CatchUpStatus#CUS_UNDEFINED Not started}</b>
 *
 * <p>The process is created in to this status upon receiving {@code CatchUpRequested} event.
 * The further actions include:
 *
 * <ul>
 *     <li>A {@link CatchUpStarted} event is emitted. The target projection repository listens to
 *     this event and kills the state of the matching entities.
 *
 *     <li>The catch-up process in moved to the {@link CatchUpStatus#STARTED STARTED} status.
 * </ul>
 *
 * <p><b>{@link CatchUpStatus#STARTED STARTED}</b>
 *
 * <p>When the process is in this status, the event history is read and the matching events are sent
 * to the {@code Inbox}es of the corresponding projections
 *
 * <p>The catch-up maintains this status until the history is read till the point in time,
 * which is very close to the {@link Time#currentTime() Time.currentTime()}.
 *
 * <p>At this stage the actions are as follows.
 *
 * <ul>
 *      <li>The historical event messages are read from the {@link EventStore} respecting
 *      the time range requested and the time of the last read operation performed by this process.
 *      The maximum number of the events read is determined by
 *      {@linkplain DeliveryBuilder#setCatchUpPageSize(int) one of the Delivery settings}.
 *
 *      <li>Depending on the targets requested for the catch-up, the events are posted to the
 *      corresponding entities.
 *
 *      <li>Unless the timestamps of the events are getting close to the current time, an
 *      {@link HistoryEventsRecalled} is emitted, leaving the process in the {@code STARTED} status
 *      and triggering the next round similar to this one.
 *
 *      <p>If the timestamps of the events read on this step are as close to the current time as
 *      {@linkplain CatchUpProcessBuilder#withTurbulencePeriod(Duration) the "turbulence" period},
 *      the {@link HistoryFullyRecalled} is emitted.
 * </ul>
 *
 * <p><b>{@link CatchUpStatus#FINALIZING FINALIZING}</b>
 *
 * <p>The process moves to this status when the event history has been fully recalled and the
 * corresponding {@code HistoryFullyRecalled} is received. At this stage the {@code Delivery} stops
 * the propagation of the events to the catch-up messages, waiting for this process to populate
 * the inboxes with the messages arriving to be dispatched during the "turbulence" period.
 * Potentially, the inboxes will contain the duplicates produced by both the live users and this
 * process. To deal with it, a de-duplication is peformed by the {@code Delivery}.
 * See {@link CatchUpStation} for more details.
 *
 * <p>The actions are as follows.
 *
 * <ul>
 *      <li>All the remaining messages of the matching event types are read {@link EventStore}.
 *      In this operation the read limits set by the {@code Delivery} are NOT used, since
 *      the goal is to read the remainder of the events.
 *
 *      <li>If there were no events read, the {@link CatchUpCompleted} is emitted and the process
 *      is moved to the {@link CatchUpStatus#COMPLETED COMPLETED} status.
 *
 *      <li>If there were some events read, the {@link LiveEventsPickedUp} event is emitted.
 *      This allows some time for the posted events to become visible to the {@code Delivery}.
 *      The reacting handler of the {@code LiveEventsPickedUp} completes the process.
 * </ul>
 *
 * <p>{@link CatchUpStatus#COMPLETED COMPLETED}</p>
 *
 * <p>Once the process moves to the {@code COMPLETED} status, the corresponding {@code Delivery}
 * routines de-duplicate, re-order and dispatch the "paused" events. Then the normal live delivery
 * flow is resumed.
 *
 * <p>To ensure that all the shards in which the "paused" historical events reside are processed,
 * this process additionally emits the "maintenance" {@link ShardProcessingRequested} events for
 * each shard involved. By dispatching them, the system guarantees that the {@code Delivery}
 * observes the {@code COMPLETED} status of this process and delivers the remainer of the messages.
 *
 * @implNote Technically, the instances of this class are not
 *         {@linkplain io.spine.server.procman.ProcessManager process managers}, since it is
 *         impossible to register the process managers with the same state in
 *         a multi-{@code BoundedContext} application.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyCoupledClass"})    // It does a lot.
public final class CatchUpProcess<I>
        extends AbstractStatefulReactor<CatchUpId, CatchUp, CatchUp.Builder> {

    private static final TypeUrl TYPE = TypeUrl.from(CatchUp.getDescriptor());

    private final ProjectionRepository<I, ?, ?> repository;
    private final DispatchCatchingUp<I> dispatchOperation;
    private final CatchUpStorage storage;
    private final CatchUpStarter.Builder<I> starterTemplate;
    private final Limit queryLimit;
    private final Duration turbulencePeriod;

    private @MonotonicNonNull CatchUpStarter<I> catchUpStarter;
    private @MonotonicNonNull Supplier<EventStore> eventStore;

    CatchUpProcess(CatchUpProcessBuilder<I> builder) {
        super(TYPE);
        this.repository = builder.repository();
        this.dispatchOperation = builder.dispatchOp();
        this.storage = builder.storage();
        this.queryLimit = limitOf(builder.pageSize());
        this.turbulencePeriod = builder.turbulencePeriod();
        this.starterTemplate = CatchUpStarter.newBuilder(this.repository, this.storage);
    }

    @Internal
    public static <I> CatchUpProcessBuilder<I> newBuilder(ProjectionRepository<I, ?, ?> repo) {
        checkNotNull(repo);
        return new CatchUpProcessBuilder<>(repo);
    }

    @Override
    @Internal
    public void registerWith(BoundedContext context) {
        super.registerWith(context);
        this.eventStore = () -> context.eventBus()
                                       .eventStore();
        this.catchUpStarter = starterTemplate.withContext(context)
                                             .build();
    }

    /**
     * Starts the catch-up for the projection instances selecting them by their identifiers.
     *
     * <p>If no identifiers is passed (i.e. {@code null}) then all of the instances should be
     * caught up
     *
     * @param since
     *         since when the catch-up should be performed
     * @param ids
     *         identifiers of the projections to catch up, or {@code null} if all of the
     *         instances should be caught up
     * @throws CatchUpAlreadyStartedException
     *         if at least one of the selected instances is already catching up at the moment
     */
    @Internal
    public void startCatchUp(Timestamp since, @Nullable Set<I> ids)
            throws CatchUpAlreadyStartedException {
        catchUpStarter.start(ids, since);
    }

    @React
    CatchUpStarted handle(CatchUpRequested e, EventContext ctx) {
        CatchUpId id = e.getId();

        CatchUp.Request request = e.getRequest();

        Timestamp sinceWhen = request.getSinceWhen();
        Timestamp withWindow = subtract(sinceWhen, fromNanos(1));
        builder().setWhenLastRead(withWindow)
                 .setRequest(request);
        CatchUpStarted started = started(id);
        builder().setStatus(CatchUpStatus.STARTED);
        flushState();

        Event event = wrapAsEvent(started, ctx);
        Set<I> ids = targetsForCatchUpSignals(request);
        dispatchAll(ImmutableList.of(event), ids);

        return started;
    }

    private Set<I> targetsForCatchUpSignals(CatchUp.Request request) {
        Set<I> ids;
        List<Any> rawTargets = request.getTargetList();
        ids = rawTargets.isEmpty()
              ? ImmutableSet.copyOf(repository.index())
              : unpack(rawTargets);
        return ids;
    }

    private Timestamp turbulenceStart() {
        return subtract(Time.currentTime(), turbulencePeriod);
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(CatchUpStarted event) {
        return recallMoreEvents(event.getId());
    }

    private Event wrapAsEvent(CatchUpSignal event, EventContext context) {
        Event firstEvent;
        EventFactory factory = EventFactory.forImport(context.actorContext(), producerId());
        firstEvent = factory.createEvent(event, null);
        return firstEvent;
    }

    @React
    EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> handle(HistoryEventsRecalled event) {
        return recallMoreEvents(event.getId());
    }

    private EitherOf2<HistoryEventsRecalled, HistoryFullyRecalled> recallMoreEvents(CatchUpId id) {
        CatchUp.Request request = builder().getRequest();

        int nextRound = builder().getCurrentRound() + 1;
        builder().setCurrentRound(nextRound);

        List<Event> readInThisRound = readMore(request, turbulenceStart(), queryLimit);
        if (!readInThisRound.isEmpty()) {
            List<Event> stripped = stripLastTimestamp(readInThisRound);

            Event lastEvent = stripped.get(stripped.size() - 1);
            Timestamp lastEventTimestamp = lastEvent.getContext()
                                                    .getTimestamp();
            builder().setWhenLastRead(lastEventTimestamp);
            dispatchAll(stripped);
        } else {
            return EitherOf2.withB(fullyRecalled(id));
        }
        return EitherOf2.withA(recalled(id));
    }

    private static List<Event> stripLastTimestamp(List<Event> events) {
        int lastIndex = events.size() - 1;
        Event lastEvent = events.get(lastIndex);
        Timestamp lastTimestamp = lastEvent.getContext()
                                           .getTimestamp();
        for (int index = lastIndex; index >= 0; index--) {
            Event event = events.get(index);
            Timestamp timestamp = event.getContext()
                                       .getTimestamp();
            if (!timestamp.equals(lastTimestamp)) {
                List<Event> result = events.subList(0, index + 1);
                return result;
            }
        }
        return events;
    }

    @React
    EitherOf2<LiveEventsPickedUp, CatchUpCompleted> handle(HistoryFullyRecalled event) {
        CatchUpId id = event.getId();
        builder().setStatus(CatchUpStatus.FINALIZING);
        flushState();

        CatchUp.Request request = builder().getRequest();
        List<Event> events = readMore(request, null, null);

        if (events.isEmpty()) {
            return EitherOf2.withB(completeProcess(id));
        }
        dispatchAll(events);
        return EitherOf2.withA(liveEventsPickedUp(id));
    }

    @React
    CatchUpCompleted on(LiveEventsPickedUp event, EventContext context) {
        return completeProcess(event.getId());
    }

    @SuppressWarnings("unused") // We just need the fact of dispatching.
    @React
    List<ShardProcessingRequested> on(CatchUpCompleted ignored) {
        int shardCount = builder().getTotalShards();
        List<Integer> affectedShards = builder().getAffectedShardList();
        List<ShardProcessingRequested> events = toShardEvents(shardCount, affectedShards);
        return events;
    }

    private static List<ShardProcessingRequested> toShardEvents(int totalShards,
                                                                List<Integer> indexes) {
        return indexes
                .stream()
                .map(indexValue -> {
                    ShardIndex shardIndex = newIndex(indexValue, totalShards);
                    ShardProcessingRequested event = shardProcessingRequested(shardIndex);
                    return event;
                })
                .collect(toList());
    }

    private CatchUpCompleted completeProcess(CatchUpId id) {
        builder().setStatus(CatchUpStatus.COMPLETED);
        flushState();
        CatchUpCompleted completed = catchUpCompleted(id);
        return completed;
    }

    private void recordAffectedShards(Set<I> actualTargets) {
        Delivery delivery = ServerEnvironment.instance()
                                             .delivery();
        String type = builder().getId()
                               .getProjectionType();
        TypeUrl projectionType = TypeUrl.parse(type);

        Set<Integer> affectedShards =
                actualTargets.stream()
                             .map(t -> delivery.whichShardFor(t, projectionType))
                             .map(ShardIndex::getIndex)
                             .collect(toSet());
        List<Integer> previouslyAffectedShards = builder().getAffectedShardList();
        Set<Integer> newValue = new TreeSet<>(affectedShards);
        newValue.addAll(previouslyAffectedShards);

        int totalShards = delivery.shardCount();
        builder().clearAffectedShard()
                 .addAllAffectedShard(newValue)
                 .setTotalShards(totalShards);
    }

    private void dispatchAll(List<Event> events) {
        if (events.isEmpty()) {
            return;
        }
        CatchUp.Request request = builder().getRequest();
        List<Any> packedIds = request.getTargetList();
        if (packedIds.isEmpty()) {
            dispatchAll(events, new HashSet<>());
        } else {
            Set<I> ids = unpack(packedIds);
            dispatchAll(events, ids);
        }
    }

    private void dispatchAll(List<Event> events, Set<I> targets) {
        if (events.isEmpty()) {
            return;
        }
        Set<I> actualTargets = new HashSet<>();
        @Nullable Set<I> targetsForDispatch = targets.isEmpty()
                                              ? null
                                              : targets;
        for (Event event : events) {
            Set<I> targetsOfThisDispatch = dispatchOperation.perform(event, targetsForDispatch);
            actualTargets.addAll(targetsOfThisDispatch);
        }
        if (!actualTargets.isEmpty()) {
            recordAffectedShards(actualTargets);
        }
    }

    private List<Event> readMore(CatchUp.Request request,
                                 @Nullable Timestamp readBefore,
                                 @Nullable Limit limit) {
        if (readBefore != null
                && Timestamps.compare(readBefore, builder().getWhenLastRead()) <= 0) {
            return ImmutableList.of();
        }
        EventStreamQuery query = toEventQuery(request, readBefore, limit);
        MemoizingObserver<Event> observer = new MemoizingObserver<>();
        eventStore.get()
                  .read(query, observer);
        List<Event> allEvents = observer.responses();
        return allEvents;
    }

    private Set<I> unpack(List<Any> packedIds) {
        return packedIds.stream()
                        .map((any) -> Identifier.unpack(any, repository.idClass()))
                        .collect(toSet());
    }

    private EventStreamQuery toEventQuery(CatchUp.Request request,
                                          @Nullable Timestamp readBefore,
                                          @Nullable Limit limit) {
        ImmutableList<EventFilter> filters = toFilters(request.getEventTypeList());
        Timestamp readAfter = builder().getWhenLastRead();
        EventStreamQuery.Builder builder =
                EventStreamQuery.newBuilder()
                                .setAfter(readAfter)
                                .addAllFilter(filters);
        if (readBefore != null) {
            builder.setBefore(readBefore);
        }
        if (limit != null) {
            builder.setLimit(limit);
        }
        return builder.vBuild();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tells if the passed envelope can be dispatched to this instance of the process
     * by verifying that the projection type URL passed with the event matches the one of the
     * projection repository configured for this instance.
     */
    @Override
    public boolean canDispatch(EventEnvelope envelope) {
        EventMessage raw = envelope.message();
        if (!(raw instanceof CatchUpSignal)) {
            return false;
        }
        CatchUpSignal asSignal = (CatchUpSignal) raw;
        String actualType = asSignal.getId()
                                    .getProjectionType();
        String expectedType = repository.entityStateType()
                                        .value();
        return expectedType.equals(actualType);
    }

    @Override
    protected ImmutableSet<CatchUpId> route(EventEnvelope event) {
        CatchUpSignal message = (CatchUpSignal) event.message();
        return ImmutableSet.of(message.getId());
    }

    @Override
    protected Optional<CatchUp> load(CatchUpId id) {
        return storage.read(new CatchUpReadRequest(id));
    }

    @Override
    protected void store(CatchUp updatedState) {
        storage.write(updatedState);
    }

    @Override
    protected CatchUp.Builder newStateBuilderWith(CatchUpId id) {
        return CatchUp.newBuilder()
                      .setId(id);
    }

    /**
     * A method object dispatching the event to catch-up.
     *
     * @param <I>
     *         the type of the identifiers of the entities to which the event should be dispatched.
     */
    @FunctionalInterface
    public interface DispatchCatchingUp<I> {

        /**
         * Dispatches the given event and optionally narrowing down the entities by the set
         * of entity identifiers to dispatch the event to.
         *
         * <p>If no particular IDs are specified, the event will be dispatched according to the
         * repository routing rules.
         *
         * @param event
         *         event to dispatch
         * @param narrowDownToIds
         *         optional set of identifiers of the targets to narrow down the event targets
         * @return the set of identifiers to which the event was actually dispatched
         */
        Set<I> perform(Event event, @Nullable Set<I> narrowDownToIds);
    }
}
