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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The strict value rules shared by every connector, engine and programmatic mutation. Conversion
 * never guesses and never maps an unrecognised token to a plausible value: {@code "yes"} is not a
 * boolean, {@code "NaN"} is not a finite double, {@code "99999999999"} is not an INT32, and a
 * missing date is never replaced with today's date.
 *
 * <p>{@link #normalize} applies the full pipeline to a source value, in this order:</p>
 * <ol>
 *   <li>MISSING takes the column default, or null when there is none.</li>
 *   <li>Text is trimmed when the column says so.</li>
 *   <li>Empty text becomes null when the column enables empty-as-null; the default is not
 *       applied to that null.</li>
 *   <li>Null is rejected for a non-nullable column.</li>
 *   <li>The value is converted to the column type, using the column's temporal pattern when one
 *       is configured.</li>
 *   <li>Overflow, non-finite doubles, unrecognised booleans and invalid temporal fields fail.</li>
 *   <li>Bounds, length bounds and permitted values are checked.</li>
 * </ol>
 *
 * <p>{@link #fromText} is the canonical text conversion alone: ISO forms, no trimming, no pattern
 * and no column settings. Engines use it to read back the canonical text they store; sources use
 * {@link #normalize}.</p>
 *
 * <p>Canonical text forms: INT32 {@code [+-]digits} within range; DOUBLE decimal or scientific
 * notation, finite only; BOOLEAN {@code true}/{@code false} in any letter case; DATE, TIME and
 * LOCAL_DATETIME in ISO-8601 local form. STRING accepts any text, including the empty string.
 * Errors name the column and the rule, never the value.</p>
 */
public final class ValueNormalizer {
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL =
            Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

    private ValueNormalizer() {
    }

    /**
     * Normalizes one source value for a column, applying every pipeline stage.
     *
     * @throws ValueConversionException when the value cannot be accepted
     */
    public static Object normalize(SourceValue source, ColumnDefinition column) {
        if (source instanceof SourceValue.Scalar scalar) {
            return scalar.value() instanceof String text
                    ? normalizeText(text, column)
                    : normalizeTyped(scalar, column);
        }
        if (source instanceof SourceValue.Missing) {
            return column.hasDefault() ? column.defaultValue() : requireNullable(column);
        }
        return requireNullable(column); // SourceValue.Null
    }

    /**
     * Normalizes a named record against a schema: each column takes its field, and a column whose
     * field is absent takes MISSING. A field the schema does not have is an error unless
     * {@code ignoreUnknown} is set (an explicit projection that excludes it). A null map value is
     * an explicit null. The whole record is checked before anything is returned.
     */
    public static Object[] normalizeRecord(TableSchema schema, Map<String, ? extends SourceValue> fields,
                                           boolean ignoreUnknown) {
        if (!ignoreUnknown) {
            for (String field : fields.keySet()) {
                if (schema.indexOf(field) < 0) {
                    throw new ValueConversionException(field, "the field is not in the schema");
                }
            }
        }
        Object[] values = new Object[schema.size()];
        for (int i = 0; i < values.length; i++) {
            ColumnDefinition column = schema.column(i);
            SourceValue source = fields.containsKey(column.name())
                    ? orNull(fields.get(column.name()))
                    : SourceValue.MISSING;
            values[i] = normalize(source, column);
        }
        return values;
    }

    /** Normalizes a positional record; it must have one value per column. */
    public static Object[] normalizeRecord(TableSchema schema, List<? extends SourceValue> fields) {
        if (fields.size() != schema.size()) {
            throw new IllegalArgumentException("Expected " + schema.size() + " values but got " + fields.size());
        }
        Object[] values = new Object[schema.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = normalize(orNull(fields.get(i)), schema.column(i));
        }
        return values;
    }

    /**
     * Converts canonical text to the column's logical type (stages 4 to 6 only). {@code null}
     * text means a null value and is rejected for a non-nullable column. Empty text stays empty
     * for STRING and is a conversion error for every other type. Column text settings and rules
     * are not applied: use {@link #normalize} for source input.
     */
    public static Object fromText(String text, ColumnDefinition column) {
        if (text == null) {
            return requireNullable(column);
        }
        return convertText(text, column, null);
    }

    /**
     * Checks an already-typed value against the column: null only when nullable, otherwise an
     * instance of the type's canonical Java class that satisfies the column rules. Returns the
     * value unchanged.
     */
    public static Object checkTyped(Object value, ColumnDefinition column) {
        if (value == null) {
            return requireNullable(column);
        }
        if (!column.type().isInstance(value)) {
            throw new ValueConversionException(column.name(),
                    "expected a " + column.type() + " value but got " + value.getClass().getSimpleName());
        }
        if (value instanceof Double d && !Double.isFinite(d)) {
            throw new ValueConversionException(column.name(), "decimal number outside the finite DOUBLE range");
        }
        column.rules().check(value, column.name());
        return value;
    }

    /**
     * The canonical text form of a typed value, independent of the platform locale: the inverse
     * of {@link #fromText} for every value it accepts. Null has no text form and returns null.
     */
    public static String toText(Object value) {
        // Double.toString is the shortest text that converts back to the same double, always with
        // '.' as the separator. Booleans, integers and java.time values use their ISO/Java forms.
        return value == null ? null : value.toString();
    }

    private static SourceValue orNull(SourceValue value) {
        return value == null ? SourceValue.NULL : value;
    }

    private static Object normalizeText(String text, ColumnDefinition column) {
        TextConversion settings = column.text();
        String value = settings.applyTrim(text);
        if (settings.emptyAsNull() && value.isEmpty()) {
            return requireNullable(column);
        }
        Object converted = convertText(value, column, settings.formatter());
        column.rules().check(converted, column.name());
        return converted;
    }

    private static Object normalizeTyped(SourceValue.Scalar scalar, ColumnDefinition column) {
        Object value = scalar.value();
        String name = column.name();
        Object converted;
        switch (column.type()) {
            // A typed value in a string column keeps its source spelling when it has one, so a
            // JSON integer too large for any numeric type survives exactly.
            case STRING: converted = scalar.lexical() != null ? scalar.lexical() : toText(value); break;
            case INT32: converted = toInt32(value, name); break;
            case DOUBLE: converted = toDouble(value, name); break;
            default: converted = value; // BOOLEAN and temporal types: checkTyped decides
        }
        return checkTyped(converted, column);
    }

    private static Integer toInt32(Object value, String name) {
        if (value instanceof Integer i) {
            return i;
        }
        BigInteger integer;
        if (value instanceof Long || value instanceof Short || value instanceof Byte) {
            integer = BigInteger.valueOf(((Number) value).longValue());
        } else if (value instanceof BigInteger b) {
            integer = b;
        } else if (value instanceof BigDecimal d) {
            try {
                integer = d.toBigIntegerExact();
            } catch (ArithmeticException e) {
                throw new ValueConversionException(name, "expected a whole number");
            }
        } else {
            throw new ValueConversionException(name,
                    "expected a whole number but got " + value.getClass().getSimpleName());
        }
        if (integer.compareTo(INT_MIN) < 0 || integer.compareTo(INT_MAX) > 0) {
            throw new ValueConversionException(name, "whole number outside the INT32 range");
        }
        return integer.intValue();
    }

    private static Double toDouble(Object value, String name) {
        if (!(value instanceof Double || value instanceof Float || value instanceof Integer
                || value instanceof Long || value instanceof Short || value instanceof Byte
                || value instanceof BigInteger || value instanceof BigDecimal)) {
            throw new ValueConversionException(name,
                    "expected a decimal number but got " + value.getClass().getSimpleName());
        }
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number)) {
            throw new ValueConversionException(name, "decimal number outside the finite DOUBLE range");
        }
        return number;
    }

    private static Object convertText(String text, ColumnDefinition column, DateTimeFormatter pattern) {
        String name = column.name();
        switch (column.type()) {
            case STRING:
                return text;
            case INT32:
                if (!INTEGER.matcher(text).matches()) {
                    throw new ValueConversionException(name, "expected a whole number");
                }
                try {
                    return Integer.valueOf(text);
                } catch (NumberFormatException e) {
                    throw new ValueConversionException(name, "whole number outside the INT32 range");
                }
            case DOUBLE:
                if (!DECIMAL.matcher(text).matches()) {
                    throw new ValueConversionException(name, "expected a decimal number");
                }
                double number = Double.parseDouble(text);
                if (!Double.isFinite(number)) {
                    throw new ValueConversionException(name, "decimal number outside the finite DOUBLE range");
                }
                return number;
            case BOOLEAN:
                if (text.equalsIgnoreCase("true")) {
                    return Boolean.TRUE;
                }
                if (text.equalsIgnoreCase("false")) {
                    return Boolean.FALSE;
                }
                throw new ValueConversionException(name, "expected true or false");
            case DATE:
                try {
                    return pattern == null ? LocalDate.parse(text) : LocalDate.parse(text, pattern);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, pattern == null
                            ? "expected an ISO date (yyyy-MM-dd)"
                            : "expected a date in the pattern " + column.text().pattern());
                }
            case TIME:
                try {
                    return pattern == null ? LocalTime.parse(text) : LocalTime.parse(text, pattern);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, pattern == null
                            ? "expected an ISO time (HH:mm[:ss[.fraction]])"
                            : "expected a time in the pattern " + column.text().pattern());
                }
            case LOCAL_DATETIME:
                try {
                    return pattern == null ? LocalDateTime.parse(text) : LocalDateTime.parse(text, pattern);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, pattern == null
                            ? "expected an ISO local date-time (yyyy-MM-ddTHH:mm[:ss])"
                            : "expected a date-time in the pattern " + column.text().pattern());
                }
            default:
                throw new IllegalStateException("Unhandled logical type " + column.type());
        }
    }

    private static Object requireNullable(ColumnDefinition column) {
        if (!column.nullable()) {
            throw new ValueConversionException(column.name(), "null is not allowed");
        }
        return null;
    }
}
