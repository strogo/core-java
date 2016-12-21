/*
 * Copyright 2016, TeamDev Ltd. All rights reserved.
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

package org.spine3.change;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;
import org.spine3.protobuf.AnyPacker;

import javax.annotation.Nullable;

import static org.spine3.protobuf.Values.pack;
import static org.spine3.util.Exceptions.wrapped;

/**
 * Factories for constructing {@link ValueMismatch} instances for different types of attributes.
 *
 * @author Alexander Yevsyukov
 * @author Andrey Lavrov
 */
public class Mismatch {

    private Mismatch() {
        // Prevent instantiation of this utility class.
    }

    /**
     * Creates a {@link ValueMismatch} instance for a string attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value found in an entity, or {@code null} if the value is not set
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(@Nullable String expected,
                                   @Nullable String actual,
                                   @Nullable String newValue,
                                   int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();
        if (expected != null) {
            builder.setExpectedPreviousValue(pack(expected));
        }
        if (actual != null) {
            builder.setActualPreviousValue(pack(actual));
        }
        if (newValue != null) {
            builder.setNewValue(pack(newValue));
        }
        builder.setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a integer attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(int expected, int actual, int newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a long integer attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(long expected, long actual, long newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a float attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(float expected, float actual, float newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a double attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(double expected, double actual, double newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a boolean attribute.
     *
     * @param expected the value expected by a command
     * @param actual   the value actual in an entity
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(boolean expected, boolean actual, boolean newValue, int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder()
                                                           .setExpectedPreviousValue(pack(expected))
                                                           .setActualPreviousValue(pack(actual))
                                                           .setNewValue(pack(newValue))
                                                           .setVersion(version);
        return builder.build();
    }

    /**
     * Creates a {@link ValueMismatch} instance for a Message attribute.
     *
     * @param expected the value expected by a command, or {@code null} if the command expects not populated field
     * @param actual   the value actual in an entity, or {@code null} if the value is not set
     * @param newValue the value from a command, which we wanted to set instead of {@code expected}
     * @param version  the current version of the entity  @return info on the mismatch
     * @return new {@link ValueMismatch} instance
     */
    public static ValueMismatch of(@Nullable Message expected,
                                   @Nullable Message actual,
                                   @Nullable Message newValue,
                                   int version) {
        final ValueMismatch.Builder builder = ValueMismatch.newBuilder();
        if (expected != null) {
            builder.setExpectedPreviousValue(AnyPacker.pack(expected));
        }
        if (actual != null) {
            builder.setActualPreviousValue(AnyPacker.pack(actual));
        }
        if (newValue != null) {
            builder.setNewValue(AnyPacker.pack(newValue));
        }
        builder.setVersion(version);
        return builder.build();
    }

    /**
     * Obtains expected string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String getExpectedString(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getExpectedPreviousValue()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }

    /**
     * Obtains actual string from the passed mismatch.
     *
     * @throws RuntimeException if the passed instance represent a mismatch of non-string values
     */
    public static String getActualString(ValueMismatch mismatch) {
        try {
            final StringValue result = mismatch.getActualPreviousValue()
                                               .unpack(StringValue.class);
            return result.getValue();
        } catch (InvalidProtocolBufferException e) {
            throw wrapped(e);
        }
    }
}
