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

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Objects;

/**
 * How a column converts textual source values: optional trimming, optional empty-as-null, and an
 * optional temporal input pattern (normalization stages 2, 3 and 5). Nothing here is applied
 * unless it is configured: by default text is not trimmed, empty text is a value, and temporal
 * values use the ISO-8601 local forms (FR003, FR004).
 *
 * <p>A temporal pattern uses {@link DateTimeFormatter} letters and is resolved strictly, so
 * invalid dates such as 31 February fail rather than roll over. Use {@code uuuu} for the year:
 * strict resolution of {@code yyyy} (year of era) also requires an era field. Patterns are
 * resolved with {@link Locale#ROOT}, so they do not depend on the platform locale.</p>
 */
public final class TextConversion {

    /** Which ends of a text value to strip of Unicode whitespace. */
    public enum Trim { NONE, LEADING, TRAILING, BOTH }

    /** No trimming, empty text is a value, ISO temporal forms. */
    public static final TextConversion DEFAULT = new TextConversion(Trim.NONE, false, null);

    private final Trim trim;
    private final boolean emptyAsNull;
    private final String pattern;
    private final DateTimeFormatter formatter;

    private TextConversion(Trim trim, boolean emptyAsNull, String pattern) {
        this.trim = Objects.requireNonNull(trim, "trim");
        this.emptyAsNull = emptyAsNull;
        this.pattern = pattern;
        if (pattern == null) {
            this.formatter = null;
        } else {
            if (pattern.isBlank()) {
                throw new IllegalArgumentException("A temporal pattern cannot be blank");
            }
            try {
                this.formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
                        .withResolverStyle(ResolverStyle.STRICT);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid temporal pattern: " + e.getMessage(), e);
            }
        }
    }

    public static TextConversion of(Trim trim, boolean emptyAsNull, String pattern) {
        TextConversion conversion = new TextConversion(trim, emptyAsNull, pattern);
        return conversion.equals(DEFAULT) ? DEFAULT : conversion;
    }

    public Trim trim() { return trim; }
    public boolean emptyAsNull() { return emptyAsNull; }

    /** The configured temporal input pattern, or null for the ISO forms. */
    public String pattern() { return pattern; }

    /** The strict formatter for {@link #pattern()}, or null for the ISO forms. */
    DateTimeFormatter formatter() { return formatter; }

    public TextConversion withTrim(Trim value) { return of(value, emptyAsNull, pattern); }
    public TextConversion withEmptyAsNull(boolean value) { return of(trim, value, pattern); }
    public TextConversion withPattern(String value) { return of(trim, emptyAsNull, value); }

    /** Applies the configured trim to a text value. */
    String applyTrim(String text) {
        switch (trim) {
            case LEADING: return text.stripLeading();
            case TRAILING: return text.stripTrailing();
            case BOTH: return text.strip();
            default: return text;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TextConversion that && trim == that.trim && emptyAsNull == that.emptyAsNull
                && Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(trim, emptyAsNull, pattern);
    }

    @Override
    public String toString() {
        return "TextConversion[trim=" + trim + ", emptyAsNull=" + emptyAsNull
                + (pattern == null ? "" : ", pattern=" + pattern) + "]";
    }
}
