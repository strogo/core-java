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

package io.spine.server.procman;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import io.spine.annotation.Internal;
import io.spine.core.CommandClass;
import io.spine.core.CommandEnvelope;
import io.spine.core.Event;
import io.spine.core.EventClass;
import io.spine.core.EventEnvelope;
import io.spine.core.RejectionEnvelope;
import io.spine.server.command.CommandHandlingEntity;
import io.spine.server.command.Commander;
import io.spine.server.command.model.CommandHandlerMethod;
import io.spine.server.command.model.CommandReactionMethod;
import io.spine.server.command.model.CommandSubstituteMethod;
import io.spine.server.commandbus.CommandBus;
import io.spine.server.event.EventReactor;
import io.spine.server.event.model.EventReactorMethod;
import io.spine.server.model.ReactorMethodResult;
import io.spine.server.procman.model.ProcessManagerClass;
import io.spine.server.rejection.RejectionReactor;
import io.spine.server.rejection.model.RejectionReactorMethod;
import io.spine.validate.ValidatingBuilder;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.spine.server.procman.model.ProcessManagerClass.asProcessManagerClass;
import static io.spine.util.Exceptions.newIllegalStateException;

/**
 * A central processing unit used to maintain the state of the business process and determine
 * the next processing step based on intermediate results.
 *
 * <p>A process manager reacts to domain events in a cross-aggregate, eventually consistent manner.
 *
 * <p>Event and command handlers are invoked by the {@link ProcessManagerRepository}
 * that manages instances of a process manager class.
 *
 * <p>For more information on Process Managers, please see:
 * <ul>
 * <li>
 *   <a href="http://www.enterpriseintegrationpatterns.com/patterns/messaging/ProcessManager.html">
 *       Process Manager Pattern</a>
 * <li><a href="http://kellabyte.com/2012/05/30/clarifying-the-saga-pattern/">
 *     Clarifying the Saga pattern</a> — the difference between Process Manager and Saga
 * <li><a href="https://dzone.com/articles/are-sagas-and-workflows-same-t">
 *     Are Sagas and Workflows the same...</a>
 * <li><a href="https://msdn.microsoft.com/en-us/library/jj591569.aspx">
 *     CQRS Journey Guide: A Saga on Sagas</a>
 * </ul>
 *
 * @param <I> the type of the process manager IDs
 * @param <S> the type of the process manager state
 * @author Alexander Litus
 * @author Alexander Yevsyukov
 */
@SuppressWarnings("OverlyCoupledClass") // OK for this central class.
public abstract class ProcessManager<I,
                                     S extends Message,
                                     B extends ValidatingBuilder<S, ? extends Message.Builder>>
        extends CommandHandlingEntity<I, S, B>
        implements EventReactor, RejectionReactor, Commander {

    /** The Command Bus to post routed commands. */
    private volatile @MonotonicNonNull CommandBus commandBus;

    /**
     * Creates a new instance.
     *
     * @param  id an ID for the new instance
     * @throws IllegalArgumentException if the ID type is unsupported
     */
    protected ProcessManager(I id) {
        super(id);
    }

    @Internal
    @Override
    protected ProcessManagerClass<?> getModelClass() {
        return asProcessManagerClass(getClass());
    }

    @Override
    protected ProcessManagerClass<?> thisClass() {
        return (ProcessManagerClass<?>) super.thisClass();
    }

    /** The method to inject {@code CommandBus} instance from the repository. */
    void setCommandBus(CommandBus commandBus) {
        this.commandBus = checkNotNull(commandBus);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In {@code ProcessManager}, this method must be called from an event reactor, a rejection
     * reactor, or a command handler.
     *
     * @throws IllegalStateException if the method is called from outside an event/rejection reactor
     *         or a command handler
     */
    @Override
    @VisibleForTesting
    protected B getBuilder() {
        return super.getBuilder();
    }

    /**
     * Dispatches the command to the handling method.
     *
     * @param  command the envelope with the command to dispatch
     * @return the list of events generated as the result of handling the command,
     *         <em>if</em> the process manager <em>handles</em> the event.
     *         Empty list, if the process manager substitutes the command
     */
    @Override
    protected List<Event> dispatchCommand(CommandEnvelope command) {
        ProcessManagerClass<?> thisClass = thisClass();
        CommandClass commandClass = command.getMessageClass();

        if (thisClass.handlesCommand(commandClass)) {
            CommandHandlerMethod method = thisClass.getHandler(commandClass);
            CommandHandlerMethod.Result result =
                    method.invoke(this, command.getMessage(), command.getCommandContext());
            List<Event> events = result.produceEvents(command);
            return events;
        }

        if (thisClass.substitutesCommand(commandClass)) {
            CommandSubstituteMethod method = thisClass.getCommander(commandClass);
            CommandSubstituteMethod.Result result =
                    method.invoke(this, command.getMessage(), command.getCommandContext());
            result.transformOrSplitAndPost(command, commandBus);
            return noEvents();
        }

        // We could not normally get here since the dispatching table is a union of handled and
        // substituted commands.
        throw newIllegalStateException(
                "ProcessManager `%s` neither handled nor transformed the command " +
                        "(id: `%s` class: `%s`).",
                this, command.getId(), commandClass
        );
    }

    /**
     * Dispatches an event the handling method.
     *
     * @param  event the envelope with the event
     * @return one of the following:
     * <ul>
     *  <li>a list of produced events, if the process manager chooses to react on the event;
     *  <li>an empty list, if the process manager chooses <em>NOT</em> to react on the event;
     *  <li>an empty list, if the process manager generates one or more commands in response
     *      to the event.
     * </ul>
     */
    List<Event> dispatchEvent(EventEnvelope event) {
        ProcessManagerClass<?> thisClass = thisClass();
        EventClass eventClass = event.getMessageClass();
        if (thisClass.reactsOnEvent(eventClass)) {
            EventReactorMethod method = thisClass.getReactor(eventClass);
            ReactorMethodResult methodResult =
                    method.invoke(this, event.getMessage(), event.getEventContext());
            List<Event> result = methodResult.produceEvents(event);
            return result;
        }

        if (thisClass.producesCommandsOn(eventClass)) {
            CommandReactionMethod method = thisClass.getCommander(eventClass);
            CommandReactionMethod.Result result =
                    method.invoke(this, event.getMessage(), event.getEventContext());
            result.produceAndPost(event, commandBus);
            return noEvents();
        }

        // We could not normally get here since the dispatching table is a union of handled and
        // substituted commands.
        throw newIllegalStateException(
                "ProcessManager `%s` neither reacted on the event (id: `%s` class: `%s`)," +
                        " nor produced commands.",
                this, event.getId(), eventClass
        );
    }

    private static ImmutableList<Event> noEvents() {
        return ImmutableList.of();
    }

    /**
     * Dispatches a rejection to the reacting method of the process manager.
     *
     * @param  rejection the envelope with the rejection
     * @return a list of produced events or an empty list if the process manager does not
     *         produce new events because of the passed event
     */
    List<Event> dispatchRejection(RejectionEnvelope rejection) {
        CommandClass commandClass = CommandClass.of(rejection.getCommandMessage());
        RejectionReactorMethod method =
                thisClass().getReactor(rejection.getMessageClass(), commandClass);
        ReactorMethodResult methodResult =
                method.invoke(this, rejection.getMessage(), rejection.getRejectionContext());
        List<Event> result = methodResult.produceEvents(rejection);
        return result;
    }

    @Override
    protected String getMissingTxMessage() {
        return "ProcessManager modification is not available this way. " +
                "Please modify the state from a command handling or event reacting method.";
    }
}