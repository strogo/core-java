/*
 * Copyright 2018, TeamDev Ltd. All rights reserved.
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

package io.spine.server.event;

import com.google.protobuf.Message;
import io.grpc.stub.StreamObserver;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.Ack;
import io.spine.core.Command;
import io.spine.core.Event;
import io.spine.server.BoundedContext;
import io.spine.server.event.given.EventRootCommandIdTestEnv.ProjectAggregateRepository;
import io.spine.server.event.given.EventRootCommandIdTestEnv.ResponseObserver;
import io.spine.test.event.ProjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.spine.grpc.StreamObservers.noOpObserver;
import static io.spine.core.Events.getRootCommandId;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.addTasks;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.createProject;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.newStreamObserver;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.newStreamQuery;
import static io.spine.server.event.given.EventRootCommandIdTestEnv.projectId;
import static org.junit.Assert.assertEquals;

public class EventRootCommandIdShould {

    private static final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(EventRootCommandIdShould.class);

    private static final ProjectId ID = projectId(EventRootCommandIdShould.class.getSimpleName());

    private BoundedContext boundedContext;

    @Before
    public void setUp() {
        boundedContext = BoundedContext.newBuilder()
                                       .build();
        final ProjectAggregateRepository repository = new ProjectAggregateRepository();
        boundedContext.register(repository);
    }

    @After
    public void tearDown() throws Exception {
        boundedContext.close();
    }

    @Test
    public void match_the_id_of_a_command_handled_by_an_aggregate() {
        final Command command = command(createProject(ID));
        
        postCommand(command);

        final List<Event> events = readEvents();
        final Event event = events.get(0);
        assertEquals(command.getId(), getRootCommandId(event));
    }

    @Test
    public void match_the_id_of_a_command_handled_by_an_aggregate_for_multiple_events() {
        final Command command = command(addTasks(ID, 3));
        
        postCommand(command);

        final List<Event> events = readEvents();
        assertEquals(command.getId(), getRootCommandId(events.get(0)));
        assertEquals(command.getId(), getRootCommandId(events.get(1)));
        assertEquals(command.getId(), getRootCommandId(events.get(2)));
    }

    @Test
    public void match_the_id_of_an_external_event_handled_by_an_aggregate() {

    }

    @Test
    public void match_the_id_of_a_command_handled_by_a_process_manager() {

    }

    @Test
    public void match_the_id_of_a_command_handled_by_a_process_manager_for_multiple_events() {

    }

    @Test
    public void match_the_id_of_an_external_event_handled_by_a_process_manager() {

    }

    private static Command command(Message message) {
        return requestFactory.createCommand(message);
    }

    private void postCommand(Command command) {
        final StreamObserver<Ack> observer = noOpObserver();
        boundedContext.getCommandBus()
                      .post(command, observer);
    }

    /**
     * Reads events from the bounded context event store.
     */
    private List<Event> readEvents() {
        final EventStreamQuery query = newStreamQuery();
        final ResponseObserver observer = newStreamObserver();

        boundedContext.getEventBus()
                      .getEventStore()
                      .read(query, observer);

        final List<Event> results = observer.getResults();
        return results;
    }

}
