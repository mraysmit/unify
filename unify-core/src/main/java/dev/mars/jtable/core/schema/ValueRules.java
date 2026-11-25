/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.core.schema;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * The record validation rules of one column (FR006, normalization stage 7): inclusive numeric
 * bounds, string length bounds measured in Unicode code points, and permitted values.
 *
 * <p>Rules are typed. Numeric bounds apply only to INT32 and DOUBLE columns and must be values of
 * the column's own Java class ({@code Integer} or {@code Double}); length bounds apply only to
 * STRING columns; permitted values apply to any type and must all be of the column's Java class.
 * {@link ColumnDefinition} checks this when the column is defined, so an inconsistent rule is a
 * configuration error rather than a per-record surprise. Null values are not subject to rules:
 * nullability decides them.</p>
 */
public final class ValueRules {

    /** No rules. */
    public static final ValueRules NONE = new ValueRules(null, null, null, null, null);

    private final Object minimum;
    private final Object maximum;
    private final Integer minLength;
    private final Integer maxLength;
    private final Set<Object> permitted;

    private ValueRules(Object minimum, Object maximum, Integer minLength, Integer maxLength, Set<Object> permitted) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.permitted = permitted;
    }

    public Object minimum() { return minimum; }
    public Object maximum() { return maximum; }
    public Integer minLength() { return minLength; }
    public Integer maxLength() { return maxLength; }

    /** The permitted values in configured order, or null when any value is permitted. */
    public Set<Object> permitted() { return permitted; }

    public boolean isEmpty() {
        return minimum == null && maximum == null && minLength == null && maxLength == null && permitted == null;
    }

    /** Inclusive lower and upper bounds; either may be null for an open end. */
    public ValueRules withBounds(Object min, Object max) {
        return new ValueRules(min, max, minLength, maxLength, permitted);
    }

    /** Inclusive length bounds in code points; either may be null for an open end. */
    public ValueRules withLength(Integer min, Integer max) {
        if ((min != null && min < 0) || (max != null && max < 0)) {
            throw new IllegalArgumentException("Length bounds cannot be negative");
        }
        return new ValueRules(minimum, maximum, min, max, permitted);
    }

    /** The only values accepted, or null to accept any value. */
    public ValueRules withPermitted(Collection<?> values) {
        Set<Object> copy = null;
        if (values != null) {
            if (values.isEmpty()) {
                throw new IllegalArgumentException("A permitted-value list cannot be empty");
            }
            copy = new LinkedHashSet<>();
            for (Object value : values) {
                copy.add(Objects.requireNonNull(value, "A permitted value cannot be null"));
            }
            copy = java.util.Collections.unmodifiableSet(copy);
        }
        return new ValueRules(minimum, maximum, minLength, maxLength, copy);
    }

    /**
     * Checks that the rules are consistent and apply to the column's type. Called when a column is
     * defined.
     *
     * @throws IllegalArgumentException when a rule does not fit the type or minimum exceeds maximum
     */
    void checkApplies(String column, LogicalType type) {
        if (minimum != null || maximum != null) {
            if (type != LogicalType.INT32 && type != LogicalType.DOUBLE) {
                throw new IllegalArgumentException("Column '" + column + "': numeric bounds apply only to INT32 and DOUBLE");
            }
            checkBound(column, type, minimum, "minimum");
            checkBound(column, type, maximum, "maximum");
            if (minimum != null && maximum != null && compare(minimum, maximum) > 0) {
                throw new IllegalArgumentException("Column '" + column + "': the minimum exceeds the maximum");
            }
        }
        if (minLength != null || maxLength != null) {
            if (type != LogicalType.STRING) {
                throw new IllegalArgumentException("Column '" + column + "': length bounds apply only to STRING");
            }
            if (minLength != null && maxLength != null && minLength > maxLength) {
                throw new IllegalArgumentException("Column '" + column + "': the minimum length exceeds the maximum length");
            }
        }
        if (permitted != null) {
            for (Object value : permitted) {
                if (!type.isInstance(value)) {
                    throw new IllegalArgumentException("Column '" + column + "': permitted values must be "
                            + type + " values, not " + value.getClass().getSimpleName());
                }
                if (value instanceof Double d && !Double.isFinite(d)) {
                    throw new IllegalArgumentException("Column '" + column + "': a permitted value must be finite");
                }
            }
        }
    }

    private static void checkBound(String column, LogicalType type, Object bound, String which) {
        if (bound == null) {
            return;
        }
        if (!type.isInstance(bound)) {
            throw new IllegalArgumentException("Column '" + column + "': the " + which + " must be a "
                    + type + " value, not " + bound.getClass().getSimpleName());
        }
        if (bound instanceof Double d && !Double.isFinite(d)) {
            throw new IllegalArgumentException("Column '" + column + "': the " + which + " must be finite");
        }
    }

    /**
     * Applies the rules to a non-null typed value of the column's type.
     *
     * @throws ValueConversionException naming the column and the rule, never the value
     */
    void check(Object value, String column) {
        if (minimum != null && compare(value, minimum) < 0) {
            throw new ValueConversionException(column, "below the minimum " + minimum);
        }
        if (maximum != null && compare(value, maximum) > 0) {
            throw new ValueConversionException(column, "above the maximum " + maximum);
        }
        if ((minLength != null || maxLength != null) && value instanceof String text) {
            int length = text.codePointCount(0, text.length());
            if (minLength != null && length < minLength) {
                throw new ValueConversionException(column, "shorter than the minimum length " + minLength);
            }
            if (maxLength != null && length > maxLength) {
                throw new ValueConversionException(column, "longer than the maximum length " + maxLength);
            }
        }
        if (permitted != null && !permitted.contains(value)) {
            throw new ValueConversionException(column, "not one of the permitted values");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(Object a, Object b) {
        return ((Comparable) a).compareTo(b);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValueRules that && Objects.equals(minimum, that.minimum)
                && Objects.equals(maximum, that.maximum) && Objects.equals(minLength, that.minLength)
                && Objects.equals(maxLength, that.maxLength) && Objects.equals(permitted, that.permitted);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimum, maximum, minLength, maxLength, permitted);
    }

    @Override
    public String toString() {
        return isEmpty() ? "ValueRules[]" : "ValueRules[min=" + minimum + ", max=" + maximum
                + ", minLength=" + minLength + ", maxLength=" + maxLength + ", permitted=" + permitted + "]";
    }
}
