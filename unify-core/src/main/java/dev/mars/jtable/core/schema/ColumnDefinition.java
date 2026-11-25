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

import java.util.Objects;

/**
 * One column of a {@link TableSchema}: a name, a logical type, whether null is allowed, and the
 * settings the normalizer applies to its values.
 *
 * <ul>
 *   <li>{@code defaultValue}: the typed value used when a record does not contain the field
 *       (FR004), or null for no default. It never replaces an explicit null, an empty value or an
 *       invalid value.</li>
 *   <li>{@code rules}: inclusive bounds, length bounds and permitted values (FR006).</li>
 *   <li>{@code text}: trimming, empty-as-null and a temporal input pattern for textual input.</li>
 * </ul>
 *
 * <p>Names follow FR001: nonblank and compared case-sensitively, exactly as given (no trimming).
 * Everything is checked here, when the column is defined: rules must fit the type, a pattern is
 * allowed only for temporal types, and the default must be a value of the type that satisfies the
 * rules. A column that is defined is therefore internally consistent.</p>
 */
public record ColumnDefinition(String name, LogicalType type, boolean nullable, Object defaultValue,
                               ValueRules rules, TextConversion text) {

    public ColumnDefinition {
        Objects.requireNonNull(name, "Column name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be blank");
        }
        Objects.requireNonNull(type, "Column type cannot be null: " + name);
        rules = rules == null ? ValueRules.NONE : rules;
        text = text == null ? TextConversion.DEFAULT : text;
        rules.checkApplies(name, type);
        if (text.pattern() != null && !isTemporal(type)) {
            throw new IllegalArgumentException("Column '" + name + "': a pattern applies only to DATE, TIME and LOCAL_DATETIME");
        }
        if (defaultValue != null) {
            if (!type.isInstance(defaultValue)) {
                throw new IllegalArgumentException("Column '" + name + "': the default must be a " + type
                        + " value, not " + defaultValue.getClass().getSimpleName());
            }
            if (defaultValue instanceof Double d && !Double.isFinite(d)) {
                throw new IllegalArgumentException("Column '" + name + "': the default must be finite");
            }
            try {
                rules.check(defaultValue, name);
            } catch (ValueConversionException e) {
                throw new IllegalArgumentException("Column '" + name + "': the default breaks a rule: "
                        + e.reason(), e);
            }
        }
    }

    /** A column with no default, rules or text settings. */
    public ColumnDefinition(String name, LogicalType type, boolean nullable) {
        this(name, type, nullable, null, ValueRules.NONE, TextConversion.DEFAULT);
    }

    /** A column that accepts null. */
    public static ColumnDefinition nullable(String name, LogicalType type) {
        return new ColumnDefinition(name, type, true);
    }

    /** A column that rejects null. */
    public static ColumnDefinition required(String name, LogicalType type) {
        return new ColumnDefinition(name, type, false);
    }

    /** A builder for a column with a default, rules or text settings. Columns start nullable. */
    public static Builder builder(String name, LogicalType type) {
        return new Builder(name, type);
    }

    /** Whether a default is configured. */
    public boolean hasDefault() {
        return defaultValue != null;
    }

    /** A copy of this column with a different name, keeping every other setting. */
    public ColumnDefinition withName(String newName) {
        return new ColumnDefinition(newName, type, nullable, defaultValue, rules, text);
    }

    /** A copy of this column with different nullability. */
    public ColumnDefinition withNullable(boolean value) {
        return new ColumnDefinition(name, type, value, defaultValue, rules, text);
    }

    private static boolean isTemporal(LogicalType type) {
        return type == LogicalType.DATE || type == LogicalType.TIME || type == LogicalType.LOCAL_DATETIME;
    }

    /** Builds a {@link ColumnDefinition}; every check runs in {@link #build()}. */
    public static final class Builder {
        private final String name;
        private final LogicalType type;
        private boolean nullable = true;
        private Object defaultValue;
        private ValueRules rules = ValueRules.NONE;
        private TextConversion text = TextConversion.DEFAULT;

        private Builder(String name, LogicalType type) {
            this.name = name;
            this.type = type;
        }

        public Builder nullable(boolean value) { this.nullable = value; return this; }
        public Builder required() { this.nullable = false; return this; }
        public Builder defaultValue(Object value) { this.defaultValue = value; return this; }
        public Builder bounds(Object min, Object max) { this.rules = rules.withBounds(min, max); return this; }
        public Builder length(Integer min, Integer max) { this.rules = rules.withLength(min, max); return this; }
        public Builder permitted(java.util.Collection<?> values) { this.rules = rules.withPermitted(values); return this; }
        public Builder rules(ValueRules value) { this.rules = value; return this; }
        public Builder trim(TextConversion.Trim value) { this.text = text.withTrim(value); return this; }
        public Builder emptyAsNull(boolean value) { this.text = text.withEmptyAsNull(value); return this; }
        public Builder pattern(String value) { this.text = text.withPattern(value); return this; }
        public Builder text(TextConversion value) { this.text = value; return this; }

        public ColumnDefinition build() {
            return new ColumnDefinition(name, type, nullable, defaultValue, rules, text);
        }
    }
}
