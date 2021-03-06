/*
 * Copyright 2021, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

package io.spine.server.aggregate.model;

import com.google.common.testing.NullPointerTester;
import com.google.common.truth.extensions.proto.ProtoSubject;
import com.google.common.truth.extensions.proto.ProtoTruth;
import com.google.protobuf.Any;
import io.spine.core.CommandContext;
import io.spine.core.Event;
import io.spine.server.aggregate.Aggregate;
import io.spine.server.aggregate.Apply;
import io.spine.server.aggregate.model.EventApplierSignature.EventApplierParams;
import io.spine.server.dispatch.Success;
import io.spine.server.model.IllegalOutcomeException;
import io.spine.server.model.MatchCriterion;
import io.spine.server.model.SignatureMismatch;
import io.spine.server.test.shared.LongIdAggregate;
import io.spine.server.type.EventEnvelope;
import io.spine.test.reflect.event.RefProjectCreated;
import io.spine.testdata.Sample;
import io.spine.testing.logging.MuteLogging;
import io.spine.testing.server.model.ModelTests;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static io.spine.protobuf.AnyPacker.pack;
import static io.spine.testing.DisplayNames.NOT_ACCEPT_NULLS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("EventApplierMethod should")
class ApplierTest {

    private final EventApplierSignature signature = new EventApplierSignature();

    @Test
    @DisplayName(NOT_ACCEPT_NULLS)
    void passNullToleranceCheck() {
        new NullPointerTester()
                .setDefault(CommandContext.class, CommandContext.getDefaultInstance())
                .setDefault(Any.class, Any.getDefaultInstance())
                .testAllPublicStaticMethods(Applier.class);
    }

    @Test
    @DisplayName("be properly created from signature")
    void beCreatedFromFactory() {
        Method method = new ValidApplier().getMethod();


        Optional<Applier> actual = signature.classify(method);
        assertTrue(actual.isPresent());

        Applier expected = new Applier(method, EventApplierParams.MESSAGE);
        assertEquals(expected, actual.get());
    }

    @Test
    @DisplayName("allow invocation")
    void invokeApplierMethod() {
        ValidApplier applierObject = new ValidApplier();
        Optional<Applier> method = signature.classify(applierObject.getMethod());
        assertTrue(method.isPresent());
        Applier applier = method.get();
        RefProjectCreated eventMessage = Sample.messageOfType(RefProjectCreated.class);
        Event event = Event
                .newBuilder()
                .setMessage(pack(eventMessage))
                .build();
        EventEnvelope envelope = EventEnvelope.of(event);
        applier.invoke(applierObject, envelope);

        assertEquals(eventMessage, applierObject.eventApplied);
    }

    @Test
    @DisplayName("check method access modifier")
    void checkMethodAccessModifier() {
        Method method = new ValidApplierButNotPackagePrivate().getMethod();

        Collection<SignatureMismatch> mismatches = signature.match(method);
        assertEquals(1, mismatches.size());
        SignatureMismatch mismatch = mismatches.iterator().next();
        assertNotNull(mismatch);
        assertEquals(MatchCriterion.ACCESS_MODIFIER, mismatch.getUnmetCriterion());
    }

    @Test
    @DisplayName("convert `null` result to Success")
    void convertToSuccess() {
        ValidApplier applierObject = new ValidApplier();
        Optional<Applier> method = signature.classify(applierObject.getMethod());
        assertTrue(method.isPresent());
        Applier applier = method.get();

        Void result = null;
        Event event = Event
                .newBuilder()
                .setMessage(pack(RefProjectCreated.getDefaultInstance()))
                .build();
        EventEnvelope envelope = EventEnvelope.of(event);
        Success success = applier.toSuccessfulOutcome(result, applierObject, envelope);
        ProtoSubject assertSuccess = ProtoTruth.assertThat(success);
        assertSuccess.isNotNull();
        assertSuccess.isEqualToDefaultInstance();
    }

    @Test
    @DisplayName("throw on non-`null` return value")
    void failIfReturnsValue() {
        ValidApplier applierObject = new ValidApplier();
        Optional<Applier> method = signature.classify(applierObject.getMethod());
        assertTrue(method.isPresent());
        Applier applier = method.get();

        Event event = Event
                .newBuilder()
                .setMessage(pack(RefProjectCreated.getDefaultInstance()))
                .build();
        EventEnvelope envelope = EventEnvelope.of(event);
        String result = "hello there";
        IllegalOutcomeException exception =
                assertThrows(IllegalOutcomeException.class,
                             () -> applier.toSuccessfulOutcome(result,
                                                               applierObject,
                                                               envelope));
        assertThat(exception)
                .hasMessageThat()
                .contains(result);
    }

    @Nested
    @DisplayName("consider applier valid when")
    class ConsiderApplierValidWhen {

        @Test
        @DisplayName("it has one message parameter")
        void hasOneMessageParameter() {
            Method applier = new ValidApplier().getMethod();

            assertIsEventApplier(applier);
        }

        @Test
        @MuteLogging // Mute the warning about signature mismatch as it's expected.
        @DisplayName("it's not package-private")
        void isNotPrivate() {
            Method method = new ValidApplierButNotPackagePrivate().getMethod();

            assertIsEventApplier(method);
        }

        private void assertIsEventApplier(Method applier) {
            assertTrue(signature.matches(applier));
        }
    }

    @Nested
    @DisplayName("consider applier invalid when")
    class ConsiderApplierInvalidWhen {

        @Test
        @DisplayName("it's not annotated")
        void isNotAnnotated() {
            Method applier = new InvalidApplierNoAnnotation().getMethod();

            assertIsNotEventApplier(applier);
        }

        @Test
        @DisplayName("it has no params")
        void hasNoParams() {
            Method applier = new InvalidApplierNoParams().getMethod();

            assertIsNotEventApplier(applier);
        }

        @Test
        @DisplayName("it has too many params")
        void hasTooManyParams() {
            Method applier = new InvalidApplierTooManyParams().getMethod();

            assertIsNotEventApplier(applier);
        }

        @Test
        @DisplayName("it has one param of invalid type")
        void hasOneInvalidParam() {
            Method applier = new InvalidApplierOneNotMsgParam().getMethod();

            assertIsNotEventApplier(applier);
        }

        @Test
        @DisplayName("it has non-void return type")
        void returnsNonVoidType() {
            Method applier = new InvalidApplierNotVoid().getMethod();

            assertIsNotEventApplier(applier);
        }

        private void assertIsNotEventApplier(Method applier) {
            assertFalse(signature.match(applier).isEmpty());
        }
    }

    /*
     * Valid appliers
     ****************/

    private static class ValidApplier extends TestEventApplier {

        private RefProjectCreated eventApplied;

        @Apply
        private void apply(RefProjectCreated event) {
            this.eventApplied = event;
        }
    }

    private static class ValidApplierButNotPackagePrivate extends TestEventApplier {

        @Apply
        public void apply(RefProjectCreated event) {
        }
    }

    /*
     * Invalid appliers
     *******************/

    private static class InvalidApplierNoAnnotation extends TestEventApplier {

        @SuppressWarnings("unused")
        public void apply(RefProjectCreated event) {
        }
    }

    private static class InvalidApplierNoParams extends TestEventApplier {

        @Apply
        public void apply() {
        }
    }

    private static class InvalidApplierTooManyParams extends TestEventApplier {

        @Apply
        public void apply(RefProjectCreated event, Object redundant) {
        }
    }

    private static class InvalidApplierOneNotMsgParam extends TestEventApplier {

        @Apply
        public void apply(Exception invalid) {
        }
    }

    private static class InvalidApplierNotVoid extends TestEventApplier {

        @Apply
        public Object apply(RefProjectCreated event) {
            return event;
        }
    }

    private abstract static class TestEventApplier
            extends Aggregate<Long, LongIdAggregate, LongIdAggregate.Builder> {

        private static final String APPLIER_METHOD_NAME = "apply";

        public Method getMethod() {
            return ModelTests.getMethod(getClass(), APPLIER_METHOD_NAME);
        }
    }
}
