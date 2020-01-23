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

package io.spine.client;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Primitives;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.google.protobuf.Timestamp;
import io.spine.annotation.Internal;
import io.spine.base.EntityColumn;
import io.spine.base.EntityStateField;
import io.spine.base.EventContextField;
import io.spine.base.EventMessageField;
import io.spine.base.Field;
import io.spine.base.FieldPath;
import io.spine.client.CompositeFilter.CompositeOperator;
import io.spine.core.Event;
import io.spine.core.Version;

import java.util.Collection;
import java.util.function.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Lists.asList;
import static io.spine.client.CompositeFilter.CompositeOperator.ALL;
import static io.spine.client.CompositeFilter.CompositeOperator.EITHER;
import static io.spine.client.Filter.Operator;
import static io.spine.client.Filter.Operator.EQUAL;
import static io.spine.client.Filter.Operator.GREATER_OR_EQUAL;
import static io.spine.client.Filter.Operator.GREATER_THAN;
import static io.spine.client.Filter.Operator.LESS_OR_EQUAL;
import static io.spine.client.Filter.Operator.LESS_THAN;
import static io.spine.protobuf.TypeConverter.toAny;
import static java.util.Arrays.stream;

/**
 * A factory of {@link Filter} instances.
 *
 * <p>Public methods of this class represent the recommended way to create
 * a {@link Filter}.
 *
 * <a name="types"></a>
 * <h1>Comparison types</h1>
 *
 * <p>The filters support two generic kinds of comparison:
 * <ol>
 *     <li>equality comparison;
 *     <li>ordering comparison.
 * </ol>
 *
 * <p>The {@linkplain #eq equality comparison} supports any data type for the compared objects.
 *
 * <p>The ordering comparison ({@link #gt &gt;}, {@link #lt &lt;}, {@link #ge &gt;=},
 * {@link #le &lt;=}) supports only the following types:
 * <ul>
 *     <li>{@link Timestamp com.google.protobuf.Timestamp};
 *     <li>{@link Version io.spine.core.Version};
 *     <li>Java primitive number types;
 *     <li>{@code String}.
 * </ul>
 *
 * @see QueryBuilder for the application
 */
public final class Filters {

    /** Prevents this utility class instantiation. */
    private Filters() {
    }

    public static QueryFilter eq(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        return new QueryFilter(column, value, EQUAL);
    }

    public static EntityStateFilter eq(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return new EntityStateFilter(field, value, EQUAL);
    }

    public static EventFilter eq(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return new EventFilter(field, value, EQUAL);
    }

    public static EventFilter eq(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        return new EventFilter(field, value, EQUAL);
    }

    /**
     * Creates new equality {@link Filter}.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of Filter
     */
    public static Filter eq(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        return createFilter(fieldPath, value, EQUAL);
    }

    public static QueryFilter gt(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, GREATER_THAN);
    }

    public static EntityStateFilter gt(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EntityStateFilter(field, value, GREATER_THAN);
    }

    public static EventFilter gt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_THAN);
    }

    public static EventFilter gt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_THAN);
    }

    /**
     * Creates new "greater than" {@link Filter}.
     *
     * <p>For the supported types description see <a href="#types">Comparison types section</a>.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of Filter
     */
    public static Filter gt(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, GREATER_THAN);
    }

    public static QueryFilter lt(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, LESS_THAN);
    }

    public static EntityStateFilter lt(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EntityStateFilter(field, value, LESS_THAN);
    }

    public static EventFilter lt(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_THAN);
    }

    public static EventFilter lt(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_THAN);
    }

    /**
     * Creates new "less than" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of Filter
     */
    public static Filter lt(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, LESS_THAN);
    }

    public static QueryFilter ge(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, GREATER_OR_EQUAL);
    }

    public static EntityStateFilter ge(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EntityStateFilter(field, value, GREATER_OR_EQUAL);
    }

    public static EventFilter ge(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_OR_EQUAL);
    }

    public static EventFilter ge(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, GREATER_OR_EQUAL);
    }

    /**
     * Creates new "greater or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of Filter
     */
    public static Filter ge(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, GREATER_OR_EQUAL);
    }

    public static QueryFilter le(EntityColumn column, Object value) {
        checkNotNull(column);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new QueryFilter(column, value, LESS_OR_EQUAL);
    }

    public static EntityStateFilter le(EntityStateField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EntityStateFilter(field, value, LESS_OR_EQUAL);
    }

    public static EventFilter le(EventMessageField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_OR_EQUAL);
    }

    public static EventFilter le(EventContextField field, Object value) {
        checkNotNull(field);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return new EventFilter(field, value, LESS_OR_EQUAL);
    }

    /**
     * Creates new "less or equal" {@link Filter}.
     *
     * <p>See <a href="#types">Comparison types</a> section for the supported types description.
     *
     * @param fieldPath
     *         the field path or the entity column name for entity filters
     * @param value
     *         the requested value
     * @return new instance of Filter
     */
    public static Filter le(String fieldPath, Object value) {
        checkNotNull(fieldPath);
        checkNotNull(value);
        checkSupportedOrderingComparisonType(value.getClass());
        return createFilter(fieldPath, value, LESS_OR_EQUAL);
    }

    public static CompositeQueryFilter all(QueryFilter first, QueryFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeQueryFilter(asList(first, rest), ALL);
    }

    public static CompositeEntityStateFilter
    all(EntityStateFilter first, EntityStateFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeEntityStateFilter(asList(first, rest), ALL);
    }

    public static CompositeEventFilter all(EventFilter first, EventFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeEventFilter(asList(first, rest), ALL);
    }

    /**
     * Creates new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of the
     * aggregated filters.
     *
     * @param first
     *         the first {@link Filter}
     * @param rest
     *         the array of additional {@linkplain Filter filters}, possibly empty
     * @return new instance of {@link CompositeFilter}
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    // OK as the method is clearly distinguished by the first argument.
    public static CompositeFilter all(Filter first, Filter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return composeFilters(asList(first, rest), ALL);
    }

    public static CompositeQueryFilter either(QueryFilter first, QueryFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeQueryFilter(asList(first, rest), EITHER);
    }

    public static CompositeEntityStateFilter
    either(EntityStateFilter first, EntityStateFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeEntityStateFilter(asList(first, rest), EITHER);
    }

    public static CompositeEventFilter either(EventFilter first, EventFilter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return new CompositeEventFilter(asList(first, rest), EITHER);
    }

    /**
     * Creates new disjunction composite filter.
     *
     * <p>A record is considered matching this filter if it matches at least one of the aggregated
     * filters.
     *
     * @param first
     *         the first {@link Filter}
     * @param rest
     *         the array of additional {@linkplain Filter filters}, possibly empty
     * @return new instance of {@link CompositeFilter}
     */
    public static CompositeFilter either(Filter first, Filter... rest) {
        checkNotNull(first);
        checkNotNull(rest);
        return composeFilters(asList(first, rest), EITHER);
    }

    /**
     * Creates new conjunction composite filter.
     *
     * <p>A record is considered matching this filter if and only if it matches all of
     * the aggregated filters.
     *
     * <p>This method is used to create the default {@code ALL} filter if the user chooses to pass
     * instances of {@link Filter} directly to the {@link QueryBuilder}.
     *
     * @param filters
     *         the aggregated filters
     * @return new instance of {@link CompositeFilter}
     * @see #all(Filter, Filter...) for the public API equivalent
     */
    static CompositeFilter all(Collection<Filter> filters) {
        checkNotNull(filters);
        checkArgument(!filters.isEmpty(),
                      "Composite filter must contain at least one simple filter in it.");
        return composeFilters(filters, ALL);
    }

    static Filter createFilter(String fieldPath, Object value, Operator operator) {
        Field field = Field.parse(fieldPath);
        return createFilter(field, value, operator);
    }

    static Filter createFilter(Field field, Object value, Operator operator) {
        FieldPath fieldPath = field.path();
        return createFilter(fieldPath, value, operator);
    }

    static Filter createContextFilter(Field field, Object value, Operator operator) {
        FieldPath fieldPath = Event.Fields.context()
                                          .getField()
                                          .nested(field)
                                          .path();
        return createFilter(fieldPath, value, operator);
    }

    static Filter createFilter(FieldPath path, Object value, Operator operator) {
        Any wrappedValue = toAny(value);
        Filter filter = Filter
                .newBuilder()
                .setFieldPath(path)
                .setValue(wrappedValue)
                .setOperator(operator)
                .build();
        return filter;
    }

    static CompositeFilter composeFilters(Collection<Filter> filters, CompositeOperator operator) {
        CompositeFilter result = CompositeFilter
                .newBuilder()
                .addAllFilter(filters)
                .setOperator(operator)
                .build();
        return result;
    }

    static Filter[] extractFilters(FilterHolder<?>[] filters) {
        return stream(filters)
                .map(FilterHolder::filter)
                .toArray(Filter[]::new);
    }

    static <M extends Message> ImmutableList<Filter>
    extractFilters(Collection<? extends FilterHolder<M>> filters) {
        return filters.stream()
                      .map(FilterHolder::filter)
                      .collect(toImmutableList());
    }

    static CompositeFilter[] extractFilters(CompositeFilterHolder<?>[] filters) {
        return stream(filters)
                .map(CompositeFilterHolder::filter)
                .toArray(CompositeFilter[]::new);
    }

    private static void checkSupportedOrderingComparisonType(Class<?> cls) {
        Class<?> dataType = Primitives.wrap(cls);
        boolean supported = isSupportedNumber(dataType)
                || Timestamp.class.isAssignableFrom(dataType)
                || Version.class.isAssignableFrom(dataType)
                || String.class.isAssignableFrom(dataType);
        checkArgument(supported,
                      "The type `%s` is not supported for the ordering comparison.",
                      dataType.getCanonicalName());
    }

    private static boolean isSupportedNumber(Class<?> wrapperClass) {
        boolean result = (Number.class.isAssignableFrom(wrapperClass)
                && Comparable.class.isAssignableFrom(wrapperClass));
        return result;
    }

    /**
     * Creates a filter of events which can apply conditions from the passed
     * {@code CompositeFilter} to both event message and its context.
     *
     * // TODO:2019-12-20:dmytro.kuzmin:WIP: Update the doc here and all similar places.
     * <p>Please use the {@code "context."} prefix for referencing a field of the event context.
     */
    @Internal
    public static Predicate<Event> toEventFilter(CompositeFilter filterData) {
        checkNotNull(filterData);
        return new CompositeEventFilter(filterData);
    }
}
