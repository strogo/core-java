/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
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

package org.spine3.base;

import com.google.common.base.Optional;
import com.google.common.reflect.TypeToken;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import org.spine3.validate.ConversionError;
import org.spine3.validate.IllegalConversionArgumentException;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.protobuf.TextFormat.shortDebugString;
import static java.lang.String.format;
import static org.spine3.protobuf.AnyPacker.unpack;

/**
 * Utility class for working with {@code Stringifier}s.
 *
 * @author Alexander Yevsyukov
 * @author Illia Shepilov
 */
public class Stringifiers {

    /** A {@code null} ID string representation. */
    public static final String NULL_ID = "NULL";

    /** An empty ID string representation. */
    public static final String EMPTY_ID = "EMPTY";

    private static final Pattern PATTERN_COLON = Pattern.compile(":");
    private static final Pattern PATTERN_T = Pattern.compile("T");
    private static final Pattern PATTERN_COLON_SPACE = Pattern.compile(": ");
    private static final String EQUAL_SIGN = "=";

    private Stringifiers() {
        // Disable instantiation of this utility class.
    }

    /**
     * Converts the passed timestamp to the string, which will serve as ID.
     *
     * @param timestamp the value to convert
     * @return string representation of timestamp-based ID
     */
    public static String toIdString(Timestamp timestamp) {
        checkNotNull(timestamp);
        String result = Timestamps.toString(timestamp);
        result = PATTERN_COLON.matcher(result)
                              .replaceAll("-");
        result = PATTERN_T.matcher(result)
                          .replaceAll("_T");

        return result;
    }

    /**
     * Converts the passed ID value into the string representation.
     *
     * @param id  the value to convert
     * @param <I> the type of the ID
     * @return <ul>
     * <li>for classes implementing {@link Message} &mdash; a Json form;
     * <li>for {@code String}, {@code Long}, {@code Integer} &mdash; the result of {@link Object#toString()};
     * <li>for {@code null} ID &mdash; the {@link #NULL_ID};
     * <li>if the result is empty or blank string &mdash; the {@link #EMPTY_ID}.
     * </ul>
     * @throws IllegalArgumentException if the passed type isn't one of the above or
     *                                  the passed {@link Message} instance has no fields
     * @see StringifierRegistry
     */
    public static <I> String idToString(@Nullable I id) {
        if (id == null) {
            return NULL_ID;
        }

        final Identifier<?> identifier;
        if (id instanceof Any) {
            final Message unpacked = unpack((Any) id);
            identifier = Identifier.fromMessage(unpacked);
        } else {
            identifier = Identifier.from(id);
        }

        final String result = identifier.toString();
        return result;
    }

    @SuppressWarnings("unchecked") // OK to cast to String as output type of Stringifier.
    static String idMessageToString(Message message) {
        checkNotNull(message);
        final String result;
        final StringifierRegistry registry = StringifierRegistry.getInstance();
        final Class<? extends Message> msgClass = message.getClass();
        final TypeToken<? extends Message> msgToken = TypeToken.of(msgClass);
        if (registry.hasStringifierFor(msgToken)) {
            @SuppressWarnings("OptionalGetWithoutIsPresent") // OK as we check for presence above.
            final Stringifier converter = registry.get(msgToken)
                                                  .get();
            result = (String) converter.convert(message);
        } else {
            result = convert(message);
        }
        return result;
    }

    private static String convert(Message message) {
        final Collection<Object> values = message.getAllFields()
                                                 .values();
        final String result;
        if (values.isEmpty()) {
            result = EMPTY_ID;
        } else if (values.size() == 1) {
            final Object object = values.iterator()
                                        .next();
            if (object instanceof Message) {
                result = idMessageToString((Message) object);
            } else {
                result = object.toString();
            }
        } else {
            result = messageWithMultipleFieldsToString(message);
        }
        return result;
    }

    private static String messageWithMultipleFieldsToString(MessageOrBuilder message) {
        String result = shortDebugString(message);
        result = PATTERN_COLON_SPACE.matcher(result)
                                    .replaceAll(EQUAL_SIGN);
        return result;
    }

    protected static class TimestampIdStringifer extends Stringifier<Timestamp> {

        @Override
        protected String doForward(Timestamp timestamp) {
            final String result = toIdString(timestamp);
            return result;
        }

        @Override
        @SuppressWarnings("ThrowInsideCatchBlockWhichIgnoresCaughtException")
        // It is OK, because all necessary information about exception
        // is passed to the {@code ConversionError} instance.
        protected Timestamp doBackward(String s) {
            try {
                return Timestamps.parse(s);
            } catch (ParseException e) {
                final ConversionError conversionError = new ConversionError(e.getMessage());
                throw new IllegalConversionArgumentException(conversionError);
            }
        }
    }

    protected static class EventIdStringifier extends Stringifier<EventId> {
        @Override
        protected String doForward(EventId eventId) {
            final String result = eventId.getUuid();
            return result;
        }

        @Override
        protected EventId doBackward(String s) {
            final EventId result = EventId.newBuilder()
                                          .setUuid(s)
                                          .build();
            return result;
        }
    }

    protected static class CommandIdStringifier extends Stringifier<CommandId> {
        @Override
        protected String doForward(CommandId commandId) {
            final String result = commandId.getUuid();
            return result;
        }

        @Override
        protected CommandId doBackward(String s) {
            final CommandId result = CommandId.newBuilder()
                                              .setUuid(s)
                                              .build();
            return result;
        }
    }

    /**
     * The stringifier for the {@code Map} classes.
     *
     * <p> The converter for the type of the elements in the map
     * should be registered in the {@code StringifierRegistry} class
     * for the correct usage of the {@code MapStringifier} converter.
     *
     * @param <K> the type of the keys in the map
     * @param <V> the type of the values in the map
     */
    protected static class MapStringifier<K, V> extends Stringifier<Map<K, V>> {

        private static final String DEFAULT_ELEMENTS_DELIMITER = ",";
        private static final String KEY_VALUE_DELIMITER = ":";

        /**
         * The delimiter for the passed elements in the {@code String} representation,
         * {@code DEFAULT_ELEMENTS_DELIMITER} by default.
         */
        private final String delimiter;
        private final Stringifier<K> keyStringifier;
        private final Stringifier<V> valueStringifier;
        private final Class<K> keyClass;
        private final Class<V> valueClass;
        private final StringifierRegistry stringifierRegistry = StringifierRegistry.getInstance();

        public MapStringifier(Class<K> keyClass, Class<V> valueClass) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            keyStringifier = getStringifier(keyClass);
            valueStringifier = getStringifier(valueClass);
            this.delimiter = DEFAULT_ELEMENTS_DELIMITER;
        }

        /**
         * That constructor should be used when need to use
         * a custom delimiter of the elements during conversion.
         *
         * @param keyClass   the class of the key elements
         * @param valueClass the class of the value elements
         * @param delimiter  the delimiter for the passed elements via string
         */
        public MapStringifier(Class<K> keyClass, Class<V> valueClass, String delimiter) {
            super();
            this.keyClass = keyClass;
            this.valueClass = valueClass;
            this.delimiter = delimiter;
            keyStringifier = getStringifier(keyClass);
            valueStringifier = getStringifier(valueClass);
        }

        @Override
        protected String doForward(Map<K, V> map) {
            final String result = map.toString();
            return result;
        }

        @Override
        protected Map<K, V> doBackward(String s) {
            final String[] buckets = s.split(delimiter);
            final Map<K, V> resultMap = newHashMap();

            for (String bucket : buckets) {
                saveConvertedBucket(resultMap, bucket);
            }

            return resultMap;
        }

        private Map<K, V> saveConvertedBucket(Map<K, V> resultMap, String element) {
            final String[] keyValue = element.split(KEY_VALUE_DELIMITER);
            checkKeyValue(keyValue);

            final String key = keyValue[0];
            final String value = keyValue[1];

            final K convertedKey = getConvertedElement(keyStringifier, keyClass, key);
            final V convertedValue = getConvertedElement(valueStringifier, valueClass, value);

            resultMap.put(convertedKey, convertedValue);
            return resultMap;
        }

        @SuppressWarnings("unchecked")
        // It is OK because it is checked with #isStringClass(Class<?>) method
        private static <I> I getConvertedElement(Stringifier<I> stringifier,
                                                 Class<I> elementClass,
                                                 String elementToConvert) {
            final I result;
            if (isStringClass(elementClass)) {
                result = stringifier.reverse()
                                    .convert(elementToConvert);
            } else {
                result = (I) elementToConvert;
            }
            return result;
        }

        private static boolean isStringClass(Class<?> aClass) {
            return String.class.equals(aClass);
        }

        private static void checkKeyValue(String[] keyValue) {
            if (keyValue.length != 2) {
                final String exMessage =
                        "Illegal key - value format, key value should be separated with `:`";
                throw conversionError(exMessage);
            }
        }

        private <I> Stringifier<I> getStringifier(Class<I> valueClass) {
            final Optional<Stringifier<I>> keyStringifierOpt =
                    stringifierRegistry.get(TypeToken.of(valueClass));

            if (!keyStringifierOpt.isPresent()) {
                final String exMessage =
                        format("Stringifier for the %s type is not provided", valueClass);
                throw conversionError(exMessage);
            }
            final Stringifier<I> stringifier = keyStringifierOpt.get();
            return stringifier;
        }

        private static IllegalConversionArgumentException conversionError(String exMessage) {
            return new IllegalConversionArgumentException(new ConversionError(exMessage));
        }
    }
}
