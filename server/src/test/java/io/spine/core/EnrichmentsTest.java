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

package io.spine.core;

import com.google.common.testing.NullPointerTester;
import com.google.protobuf.BoolValue;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import io.spine.base.EventMessage;
import io.spine.base.Identifier;
import io.spine.core.given.GivenEvent;
import io.spine.test.core.given.GivenProjectCreated;
import io.spine.testing.server.TestEventFactory;
import io.spine.type.TypeName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.spine.base.Identifier.newUuid;
import static io.spine.core.Enrichments.getEnrichment;
import static io.spine.core.Enrichments.getEnrichments;
import static io.spine.core.given.GivenEvent.context;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.protobuf.TypeConverter.toMessage;
import static io.spine.testing.DisplayNames.HAVE_PARAMETERLESS_CTOR;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static io.spine.testing.Tests.assertHasPrivateParameterlessCtor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Alexander Yevsyukov
 */
@DisplayName("Enrichments utility should")
class EnrichmentsTest {

    private GivenProjectCreated projectCreated;
    private BoolValue boolValue;
    private TestEventFactory eventFactory;
    private EventContext context;

    /**
     * Creates a new {@link EventContext} enriched with the passed message.
     *
     * <p>The key in the map is a fully-qualified {@code TypeName} of the message.
     * See {@link Enrichment.Container#getItemsMap()} or {@code Enrichment} proto type definition
     * for details.
     */
    private static EventContext givenContextEnrichedWith(Message enrichment) {
        String enrichmentKey = TypeName.of(enrichment)
                                       .value();
        Enrichment.Builder enrichments =
                Enrichment.newBuilder()
                          .setContainer(Enrichment.Container.newBuilder()
                                                            .putItems(enrichmentKey,
                                                                      pack(enrichment)));
        EventContext context = context().toBuilder()
                                        .setEnrichment(enrichments.build())
                                        .build();
        return context;
    }

    @BeforeEach
    void setUp() {
        String producerId = newUuid();
        projectCreated = GivenProjectCreated
                .newBuilder()
                .setId(producerId)
                .build();
        boolValue = toMessage(false);
        eventFactory = TestEventFactory.newInstance(Identifier.pack(producerId), getClass());
        context = eventFactory.createEvent(projectCreated)
                              .getContext();
    }

    @Test
    @DisplayName(HAVE_PARAMETERLESS_CTOR)
    void haveUtilityConstructor() {
        assertHasPrivateParameterlessCtor(Enrichments.class);
    }

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(StringValue.class, StringValue.getDefaultInstance())
                .setDefault(EventContext.class, context())
                .testAllPublicStaticMethods(Enrichments.class);
    }

    @Test
    @DisplayName("recognize if event enrichment is enabled")
    void recognizeEnrichmentEnabled() {
        EventEnvelope event = EventEnvelope.of(eventFactory.createEvent(projectCreated));

        assertTrue(event.isEnrichmentEnabled());
    }

    @Test
    @DisplayName("recognize if event enrichment is disabled")
    void recognizeEnrichmentDisabled() {
        EventEnvelope event = EventEnvelope.of(GivenEvent.withDisabledEnrichmentOf(projectCreated));

        assertFalse(event.isEnrichmentEnabled());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    @DisplayName("obtain all event enrichments from context")
    void getAllEnrichments() {
        EventContext context = givenContextEnrichedWith(projectCreated);

        Optional<Enrichment.Container> enrichments = getEnrichments(context);

        assertTrue(enrichments.isPresent());
        assertEquals(context.getEnrichment()
                            .getContainer(), enrichments.get());
    }

    @Test
    @DisplayName("return absent if there are no enrichments in context")
    void returnAbsentOnNoEnrichments() {
        assertFalse(getEnrichments(context).isPresent());
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    // We're sure the optional is populated in this method.
    @Test
    @DisplayName("obtain specific event enrichment from context")
    void obtainSpecificEnrichment() {
        EventContext context = givenContextEnrichedWith(projectCreated);
        Optional<? extends EventMessage> enrichment =
                getEnrichment(projectCreated.getClass(), context);
        assertTrue(enrichment.isPresent());
        assertEquals(projectCreated, enrichment.get());
    }

    @Test
    @DisplayName("return absent if there are no enrichments in context when searching for one")
    void returnAbsentOnNoEnrichmentsSearch() {
        assertFalse(getEnrichment(StringValue.class, context).isPresent());
    }

    @Test
    @DisplayName("return absent if there is no specified enrichment in context")
    void returnAbsentOnEnrichmentNotFound() {
        EventContext context = givenContextEnrichedWith(boolValue);
        assertFalse(getEnrichment(StringValue.class, context).isPresent());
    }
}