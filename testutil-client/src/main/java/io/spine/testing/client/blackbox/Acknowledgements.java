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

package io.spine.testing.client.blackbox;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;
import io.spine.base.Error;
import io.spine.core.Ack;
import io.spine.core.Event;
import io.spine.core.RejectionClass;
import io.spine.core.Status;
import io.spine.type.TypeUrl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static io.spine.protobuf.AnyPacker.unpack;

/**
 * Contains the data on provided acknowledgements, allowing it to be queried about acks, errors, 
 * and rejections.
 *
 * @author Mykhailo Drachuk
 */
@VisibleForTesting
public class Acknowledgements {

    private static final Event EMPTY_EVENT = Event.getDefaultInstance();
    private static final Error EMPTY_ERROR = Error.getDefaultInstance();

    private final List<Ack> acks = newArrayList();
    private final List<Error> errors = newArrayList();
    private final List<Event> rejectionEvents = newArrayList();
    private final Map<RejectionClass, Integer> rejectionTypes;

    public Acknowledgements(Iterable<Ack> responses) {
        for (Ack response : responses) {
            acks.add(response);

            Status status = response.getStatus();

            Error error = status.getError();
            if (!error.equals(EMPTY_ERROR)) {
                errors.add(error);
            }

            Event rejection = status.getRejection();
            if (!rejection.equals(EMPTY_EVENT)) {
                rejectionEvents.add(rejection);
            }
        }
        rejectionTypes = countRejectionTypes(rejectionEvents);
    }

    /**
     * Counts the number of times the domain event types are included in the provided list.
     *
     * @param rejectionEvents a list of rejection events
     * @return a mapping of Rejection classes to their count
     */
    private static Map<RejectionClass, Integer> countRejectionTypes(List<Event> rejectionEvents) {
        Map<RejectionClass, Integer> countForType = new HashMap<>();
        for (Event rejection : rejectionEvents) {
            RejectionClass type = RejectionClass.of(rejection);
            int currentCount = countForType.getOrDefault(type, 0);
            countForType.put(type, currentCount + 1);
        }
        return ImmutableMap.copyOf(countForType);
    }

    /**
     * @return the total number of acknowledgements observed in a Bounded Context.
     */
    public int count() {
        return acks.size();
    }

    /*
     * Errors
     ******************************************************************************/

    /**
     * @return {@code true} if errors did occur in the Bounded Context during command handling,
     * {@code false} otherwise.
     */
    public boolean containErrors() {
        return !errors.isEmpty();
    }

    /**
     * @return a total number of errors which were observed in Bounded Context acknowledgements.
     */
    public int countErrors() {
        return errors.size();
    }

    /**
     * @return {@code true} if an error which matches the provided criterion did occur in
     * the Bounded Context during command handling, {@code false} otherwise.
     */
    public boolean containErrors(ErrorCriterion criterion) {
        checkNotNull(criterion);
        return errors.stream()
                     .anyMatch(criterion::matches);
    }

    /**
     * @param criterion an error criterion specifying which kind of an error to count
     * @return a total number of times errors matching the provided criterion were
     * observed in the Bounded Context responses
     */
    public long countErrors(ErrorCriterion criterion) {
        checkNotNull(criterion);
        return errors.stream()
                     .filter(criterion::matches)
                     .count();
    }

    /*
     * Rejections
     ******************************************************************************/

    /**
     * @return {@code true} if there were any rejections in the Bounded Context,
     * {@code false} otherwise
     */
    public boolean containRejections() {
        return !rejectionEvents.isEmpty();
    }

    /**
     * @return a total amount of rejections observed in Bounded Context
     */
    public int countRejections() {
        return rejectionEvents.size();
    }

    /**
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return {@code true} if the rejection of a provided type was observed in the Bounded Context,
     * {@code false} otherwise
     */
    public boolean containRejections(RejectionClass type) {
        return rejectionTypes.containsKey(type);
    }

    /**
     * @param type rejection type in a form of {@link RejectionClass RejectionClass}
     * @return an amount of rejections of the provided type observed in Bounded Context
     */
    public int countRejections(RejectionClass type) {
        return rejectionTypes.getOrDefault(type, 0);
    }

    /**
     * @param predicate a domain message representing the rejection
     * @param type      a class of a domain rejection
     * @param <T>       a domain rejection type
     * @return {@code true} if the rejection matching the predicate was observed
     * in the Bounded Context, {@code false} otherwise
     */
    public <T extends Message> boolean containRejection(Class<T> type,
                                                        RejectionCriterion<T> predicate) {
        return rejectionEvents.stream()
                              .anyMatch(new RejectionFilter<>(type, predicate));
    }

    /**
     * @param predicate a domain message representing the rejection
     * @param type      a class of a domain rejection
     * @param <T>       a domain rejection type
     * @return an amount of rejections matching the predicate observed in Bounded Context
     */
    public <T extends Message> long countRejections(Class<T> type,
                                                    RejectionCriterion<T> predicate) {
        return rejectionEvents.stream()
                              .filter(new RejectionFilter<>(type, predicate))
                              .count();
    }

    /**
     * A predicate filtering the {@link io.spine.base.ThrowableMessage rejections} which match
     * the provided predicate.
     */
    private static class RejectionFilter<T extends Message> implements Predicate<Event> {

        private final TypeUrl typeUrl;
        private final RejectionCriterion<T> predicate;

        private RejectionFilter(Class<T> rejectionType, RejectionCriterion<T> predicate) {
            this.typeUrl = TypeUrl.of(rejectionType);
            this.predicate = predicate;
        }

        @Override
        public boolean test(Event rejection) {
            T message = unpack(rejection.getMessage());
            return typeUrl.equals(TypeUrl.of(message)) && predicate.matches(message);
        }
    }
}