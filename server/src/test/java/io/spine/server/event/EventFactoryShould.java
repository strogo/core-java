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

package io.spine.server.event;

import com.google.protobuf.Any;
import io.spine.client.TestActorRequestFactory;
import io.spine.core.CommandEnvelope;
import io.spine.test.Tests;
import io.spine.test.command.event.MandatoryFieldEvent;
import io.spine.validate.ValidationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static io.spine.base.Identifier.pack;
import static io.spine.test.TestValues.newUuidValue;

/**
 * @author Alexander Yevsyukov
 */
public class EventFactoryShould {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final TestActorRequestFactory requestFactory =
            TestActorRequestFactory.newInstance(getClass());

    private Any producerId;
    private CommandEnvelope origin;

    @Before
    public void setUp() {
        producerId = pack(newUuidValue());
        origin = requestFactory.generateEnvelope();
    }

    @Test
    public void require_producer_id() {
        thrown.expect(NullPointerException.class);
        EventFactory.on(origin, Tests.nullRef());
    }

    @Test
    public void require_origin() {
        thrown.expect(NullPointerException.class);
        EventFactory.on(Tests.<CommandEnvelope>nullRef(), producerId);
    }

    @Test
    public void validate_event_messages_before_creation() {
        EventFactory factory = EventFactory.on(origin, producerId);
        thrown.expect(ValidationException.class);
        factory.createEvent(MandatoryFieldEvent.getDefaultInstance(), null);
    }
}
