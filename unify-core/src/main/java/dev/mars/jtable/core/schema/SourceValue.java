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
 * One field value as a source delivered it, before normalization (FR004). The three states stay
 * distinct all the way to the normalizer: a field the record did not contain ({@link Missing}),
 * an explicit null ({@link Null}), and a present value ({@link Scalar}).
 *
 * <p>A scalar is usually text (CSV, XML), but a structured source may deliver a typed value such
 * as a JSON number or boolean. A typed scalar may also keep its original lexical token, so a
 * value that falls back to a string column keeps the exact source spelling, including JSON
 * integers too large for any numeric type.</p>
 */
public sealed interface SourceValue {

    /** The field is absent from the record. Resolved by the column default or nullability. */
    Missing MISSING = new Missing();

    /** The field is present and explicitly null. */
    Null NULL = new Null();

    /** The singleton for an absent field. */
    static SourceValue missing() {
        return MISSING;
    }

    /** The singleton for an explicit null. */
    static SourceValue nullValue() {
        return NULL;
    }

    /** A textual scalar; the text is the lexical token. {@code null} text is an explicit null. */
    static SourceValue text(String text) {
        return text == null ? NULL : new Scalar(text, text);
    }

    /** A typed scalar without a lexical token. {@code null} is an explicit null. */
    static SourceValue of(Object value) {
        return value == null ? NULL : new Scalar(value, null);
    }

    /** A typed scalar with its original lexical token, which may be null. */
    static SourceValue of(Object value, String lexical) {
        return value == null ? NULL : new Scalar(value, lexical);
    }

    /** An absent field. */
    final class Missing implements SourceValue {
        private Missing() {
        }

        @Override
        public String toString() {
            return "MISSING";
        }
    }

    /** An explicit null. */
    final class Null implements SourceValue {
        private Null() {
        }

        @Override
        public String toString() {
            return "NULL";
        }
    }

    /**
     * A present value. {@code value} is never null; {@code lexical} is the original token when the
     * source had one (always, for text).
     */
    record Scalar(Object value, String lexical) implements SourceValue {
        public Scalar {
            Objects.requireNonNull(value, "A scalar value cannot be null; use SourceValue.NULL");
        }

        /** Whether the value is text rather than an already-typed value. */
        public boolean isText() {
            return value instanceof String;
        }

        @Override
        public String toString() {
            // Never print the value: it may be sensitive business data.
            return "Scalar[" + value.getClass().getSimpleName() + "]";
        }
    }
}
