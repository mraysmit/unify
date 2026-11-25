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
package dev.mars.jtable.io.common.size;

import java.util.Objects;

/**
 * The size a source reports before any of its records are read (ADR-003, FR031). Source size is
 * never unknown: every connector measures bytes, counts records, or stages its data first.
 *
 * <p>A bounded count stops counting at a limit; when it reaches the limit, {@link #atLeast()} is
 * true and the value is a lower bound ("more than the largest boundary that matters").</p>
 *
 * @param value   the measured bytes or records
 * @param unit    what {@code value} counts
 * @param method  how it was measured, recorded in the operation result
 * @param atLeast whether {@code value} is only a lower bound (bounded counts that hit the limit)
 */
public record SourceSize(long value, Unit unit, Method method, boolean atLeast) {

    /** What a size counts. */
    public enum Unit { BYTES, RECORDS }

    /** How a size was determined; {@link #label()} is the name recorded in results. */
    public enum Method {
        FILE_BYTES("file-bytes", Unit.BYTES),
        STAGED_BYTES("staged-bytes", Unit.BYTES),
        EXACT_COUNT("exact-count", Unit.RECORDS),
        BOUNDED_COUNT("bounded-count", Unit.RECORDS),
        SUPPLIED_COUNT("supplied-count", Unit.RECORDS);

        private final String label;
        private final Unit unit;

        Method(String label, Unit unit) {
            this.label = label;
            this.unit = unit;
        }

        public String label() {
            return label;
        }

        public Unit unit() {
            return unit;
        }
    }

    public SourceSize {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(method, "method");
        if (value < 0) {
            throw new IllegalArgumentException("A size cannot be negative");
        }
        if (method.unit() != unit) {
            throw new IllegalArgumentException(method.label() + " measures " + method.unit() + ", not " + unit);
        }
        if (atLeast && method != Method.BOUNDED_COUNT) {
            throw new IllegalArgumentException("Only a bounded count can be a lower bound");
        }
    }

    /** The exact byte size of a local file. */
    public static SourceSize fileBytes(long bytes) {
        return new SourceSize(bytes, Unit.BYTES, Method.FILE_BYTES, false);
    }

    /** The exact byte size of a staged copy, for a source that cannot report its size. */
    public static SourceSize stagedBytes(long bytes) {
        return new SourceSize(bytes, Unit.BYTES, Method.STAGED_BYTES, false);
    }

    /** An exact record count. */
    public static SourceSize exactCount(long records) {
        return new SourceSize(records, Unit.RECORDS, Method.EXACT_COUNT, false);
    }

    /**
     * The result of counting at most {@code limit} records: exact below the limit, and a lower
     * bound when the count reached it.
     */
    public static SourceSize boundedCount(long counted, long limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("The count limit must be at least 1");
        }
        if (counted > limit) {
            throw new IllegalArgumentException("A bounded count cannot exceed its limit");
        }
        return counted < limit
                ? new SourceSize(counted, Unit.RECORDS, Method.EXACT_COUNT, false)
                : new SourceSize(limit, Unit.RECORDS, Method.BOUNDED_COUNT, true);
    }

    /** The exact record count of a supplied in-memory table. */
    public static SourceSize suppliedCount(long records) {
        return new SourceSize(records, Unit.RECORDS, Method.SUPPLIED_COUNT, false);
    }

    /** Whether the value is exact rather than a lower bound. */
    public boolean exact() {
        return !atLeast;
    }

    @Override
    public String toString() {
        return (atLeast ? ">=" : "") + value + " " + unit.name().toLowerCase() + " (" + method.label() + ")";
    }
}
