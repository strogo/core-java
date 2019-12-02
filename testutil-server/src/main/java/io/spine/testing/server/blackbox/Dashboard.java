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

package io.spine.testing.server.blackbox;

import com.google.common.flogger.FluentLogger;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.MessageId;
import io.spine.core.Subscribe;
import io.spine.logging.Logging;
import io.spine.server.event.AbstractEventSubscriber;
import io.spine.system.server.AggregateHistoryCorrupted;
import io.spine.system.server.CannotDispatchDuplicateCommand;
import io.spine.system.server.CannotDispatchDuplicateEvent;
import io.spine.system.server.ConstraintViolated;
import io.spine.system.server.HandlerFailedUnexpectedly;
import io.spine.system.server.RoutingFailed;

import java.io.PrintStream;

import static io.spine.json.Json.toJson;
import static java.lang.String.format;

final class Dashboard
        extends AbstractEventSubscriber
        implements Logging {

    @Subscribe
    public void on(ConstraintViolated event) {
        String typeUrl = event.getEntity()
                              .getTypeUrl();
        String idAsString = Identifier.toString(event.getEntity().getId());
        log(event, "Entity state (ID: %s, type: %s) is invalid.", idAsString, typeUrl);
    }

    @Subscribe
    public void on(CannotDispatchDuplicateCommand event) {
        log(event, "Command should not be dispatched twice.");
    }

    @Subscribe
    public void on(CannotDispatchDuplicateEvent event) {
        log(event, "Event should not be dispatched twice.");
    }

    @Subscribe
    public void on(HandlerFailedUnexpectedly event) {
        log(event, "Signal %s could not be handled by %s:%n%s",
            event.getHandledSignal()
                 .getTypeUrl(),
            event.getEntity()
                 .getTypeUrl(),
            event.getError()
                 .getMessage());
    }

    @Subscribe
    public void on(RoutingFailed event) {
        log(event, "Signal %s could not be routed to %s:%n%s",
            event.getHandledSignal()
                 .getTypeUrl(),
            event.getEntityType()
                 .getJavaClassName(),
            event.getError()
                 .getMessage());
    }

    @Subscribe
    public void on(AggregateHistoryCorrupted event) {
        MessageId aggregate = event.getEntity();
        String idAsString = Identifier.toString(aggregate.getId());
        log(event, "History of aggregate %s (ID: %s) could not be loaded:%n%s",
            aggregate.getTypeUrl(),
            idAsString,
            event.getError()
                 .getMessage());
    }

    @FormatMethod
    private void log(EventMessage event, @FormatString String errorMessage, Object... formatArgs) {
        String msg = format(errorMessage, formatArgs);
        FluentLogger.Api severeLogger = logger().atSevere();
        boolean loggingEnabled = severeLogger.isEnabled();
        if (loggingEnabled) {
            severeLogger.log(msg);
            severeLogger.log(toJson(event));
        } else {
            @SuppressWarnings("UseOfSystemOutOrSystemErr")
                // Edge case for disabled/misconfigured logging .
            PrintStream stderr = System.err;
            stderr.println(msg);
            stderr.println(toJson(event));
        }
    }
}
