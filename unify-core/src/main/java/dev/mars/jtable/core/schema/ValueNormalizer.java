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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * The strict value rules shared by every connector and engine. Text conversion never trims,
 * never guesses, and never maps an unrecognised token to a plausible value: {@code "yes"} is not
 * a boolean, {@code "NaN"} is not a finite double, {@code "99999999999"} is not an INT32, and a
 * missing date is never replaced with today's date.
 *
 * <p>Text forms accepted: INT32 {@code [+-]digits} within range; DOUBLE decimal or scientific
 * notation, finite only; BOOLEAN {@code true}/{@code false} in any letter case; DATE, TIME and
 * LOCAL_DATETIME in ISO-8601 local form. STRING accepts any text, including the empty string.</p>
 */
public final class ValueNormalizer {
    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL =
            Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");

    private ValueNormalizer() {
    }

    /**
     * Converts source text to the column's logical type. {@code null} text means a null value and
     * is rejected for a non-nullable column. Empty text is a value: it stays empty for STRING and
     * is a conversion error for every other type (a source that treats empty as null must say so
     * before calling this).
     */
    public static Object fromText(String text, ColumnDefinition column) {
        if (text == null) {
            return requireNullable(column);
        }
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
                    return LocalDate.parse(text);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, "expected an ISO date (yyyy-MM-dd)");
                }
            case TIME:
                try {
                    return LocalTime.parse(text);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, "expected an ISO time (HH:mm[:ss[.fraction]])");
                }
            case LOCAL_DATETIME:
                try {
                    return LocalDateTime.parse(text);
                } catch (DateTimeParseException e) {
                    throw new ValueConversionException(name, "expected an ISO local date-time (yyyy-MM-ddTHH:mm[:ss])");
                }
            default:
                throw new IllegalStateException("Unhandled logical type " + column.type());
        }
    }

    /**
     * Checks an already-typed value against the column: null only when nullable, otherwise an
     * instance of the type's canonical Java class. Returns the value unchanged.
     */
    public static Object checkTyped(Object value, ColumnDefinition column) {
        if (value == null) {
            return requireNullable(column);
        }
        if (!column.type().isInstance(value)) {
            throw new ValueConversionException(column.name(),
                    "expected a " + column.type() + " value but got " + value.getClass().getSimpleName());
        }
        if (value instanceof Double && !Double.isFinite((Double) value)) {
            throw new ValueConversionException(column.name(), "decimal number outside the finite DOUBLE range");
        }
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

    private static Object requireNullable(ColumnDefinition column) {
        if (!column.nullable()) {
            throw new ValueConversionException(column.name(), "null is not allowed");
        }
        return null;
    }
}
