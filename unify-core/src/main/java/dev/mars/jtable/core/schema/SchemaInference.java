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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Infers a schema from a bounded sample of records, then freezes it (FR002).
 *
 * <p>Use: offer records with {@link #add} until {@link #isFull()} or the source ends, call
 * {@link #freeze()}, then replay {@link #sample()} through
 * {@link ValueNormalizer#normalizeRecord(TableSchema, Map, boolean)} before reading on. Sampled
 * records are kept as raw {@link SourceValue}s, so replay is not a second read of the source.</p>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Columns appear in encounter order: declared fields first (a CSV header), then fields in
 *       the order the sample first shows them.</li>
 *   <li>A field that is missing or null in any sampled record is nullable; otherwise it is
 *       required, so a later null is a validation error rather than a silent schema change.</li>
 *   <li>Non-null scalars classify as BOOLEAN ({@code true}/{@code false} in any case), INT32,
 *       finite DOUBLE, DATE, TIME or LOCAL_DATETIME (ISO forms only), or STRING. Integer tokens
 *       outside INT32 and tokens with a significant leading zero ({@code 007}) are strings.
 *       Text is not trimmed, so {@code " 5"} is a string.</li>
 *   <li>Identical types stay; INT32 with DOUBLE becomes DOUBLE; any other mix becomes STRING; a
 *       column with no non-null value becomes nullable STRING.</li>
 *   <li>An empty sample with no declared fields has no schema: the caller must supply one.</li>
 * </ul>
 *
 * <p>Not thread-safe; one instance serves one read.</p>
 */
public final class SchemaInference {

    /** The default sample: the first 1,000 records (FR002). */
    public static final int DEFAULT_SAMPLE_SIZE = 1000;

    private static final Pattern INTEGER = Pattern.compile("[+-]?\\d+");
    private static final Pattern DECIMAL =
            Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d+)?");
    private static final BigInteger INT_MIN = BigInteger.valueOf(Integer.MIN_VALUE);
    private static final BigInteger INT_MAX = BigInteger.valueOf(Integer.MAX_VALUE);

    private final int sampleSize;
    private final Map<String, Field> fields = new LinkedHashMap<>();
    private final List<Map<String, SourceValue>> sample = new ArrayList<>();
    private TableSchema frozen;

    /** Inference over at most {@code sampleSize} records; the operation limit may lower it. */
    public SchemaInference(int sampleSize) {
        if (sampleSize < 1) {
            throw new IllegalArgumentException("The sample size must be at least 1");
        }
        this.sampleSize = sampleSize;
    }

    /** Inference over the default sample of 1,000 records. */
    public static SchemaInference withDefaultSample() {
        return new SchemaInference(DEFAULT_SAMPLE_SIZE);
    }

    public int sampleSize() {
        return sampleSize;
    }

    /** The number of records sampled so far. */
    public int sampledRecords() {
        return sample.size();
    }

    /** Whether the sample has reached its size; the caller should then freeze. */
    public boolean isFull() {
        return sample.size() >= sampleSize;
    }

    /**
     * Declares a field before any record, fixing its position. A CSV header declares its names
     * this way, so a header alone establishes nullable STRING columns.
     */
    public void declareField(String name) {
        checkOpen();
        if (!sample.isEmpty()) {
            throw new IllegalStateException("Fields must be declared before the first record");
        }
        if (fields.putIfAbsent(checkName(name), new Field()) != null) {
            throw new IllegalArgumentException("Duplicate field name: " + name);
        }
    }

    /**
     * Adds one sampled record. Field order in the map is the record's encounter order; a null map
     * value is an explicit null. The record is copied.
     *
     * @throws IllegalStateException when the sample is full or the schema is frozen
     */
    public void add(Map<String, ? extends SourceValue> record) {
        checkOpen();
        if (isFull()) {
            throw new IllegalStateException("The sample is full; freeze the schema");
        }
        Map<String, SourceValue> copy = new LinkedHashMap<>();
        for (Map.Entry<String, ? extends SourceValue> entry : record.entrySet()) {
            SourceValue value = entry.getValue() == null ? SourceValue.NULL : entry.getValue();
            copy.put(checkName(entry.getKey()), value);
        }
        boolean first = sample.isEmpty();
        for (Map.Entry<String, SourceValue> entry : copy.entrySet()) {
            Field field = fields.get(entry.getKey());
            if (field == null) {
                field = new Field();
                // A field first seen after other records was missing from them.
                field.nullable = !first;
                fields.put(entry.getKey(), field);
            }
            field.observe(entry.getValue());
        }
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            if (!copy.containsKey(entry.getKey())) {
                entry.getValue().nullable = true;
            }
        }
        sample.add(Collections.unmodifiableMap(copy));
    }

    /**
     * Freezes and returns the inferred schema. Further records are rejected; later reads validate
     * against this schema.
     *
     * @throws IllegalStateException when no field was declared or sampled
     */
    public TableSchema freeze() {
        if (frozen != null) {
            return frozen;
        }
        if (fields.isEmpty()) {
            throw new IllegalStateException("The source is empty: an explicit schema is required");
        }
        List<ColumnDefinition> columns = new ArrayList<>(fields.size());
        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            Field field = entry.getValue();
            boolean noValue = field.type == null;
            LogicalType type = noValue ? LogicalType.STRING : field.type;
            columns.add(new ColumnDefinition(entry.getKey(), type, field.nullable || noValue));
        }
        frozen = TableSchema.of(columns);
        return frozen;
    }

    /** The sampled records in order, for replay after {@link #freeze()}. Unmodifiable. */
    public List<Map<String, SourceValue>> sample() {
        return Collections.unmodifiableList(sample);
    }

    /**
     * The type a single non-null scalar suggests.
     */
    public static LogicalType classify(SourceValue.Scalar scalar) {
        Object value = scalar.value();
        if (value instanceof String text) {
            return classifyText(text);
        }
        if (value instanceof Boolean) {
            return LogicalType.BOOLEAN;
        }
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return LogicalType.INT32;
        }
        if (value instanceof Long || value instanceof BigInteger) {
            return fitsInt32(new BigInteger(value.toString())) ? LogicalType.INT32 : LogicalType.STRING;
        }
        if (value instanceof BigDecimal decimal) {
            if (decimal.scale() <= 0) {
                return fitsInt32(decimal.toBigInteger()) ? LogicalType.INT32 : LogicalType.STRING;
            }
            return Double.isFinite(decimal.doubleValue()) ? LogicalType.DOUBLE : LogicalType.STRING;
        }
        if (value instanceof Double || value instanceof Float) {
            return Double.isFinite(((Number) value).doubleValue()) ? LogicalType.DOUBLE : LogicalType.STRING;
        }
        if (value instanceof LocalDate) {
            return LogicalType.DATE;
        }
        if (value instanceof LocalTime) {
            return LogicalType.TIME;
        }
        if (value instanceof LocalDateTime) {
            return LogicalType.LOCAL_DATETIME;
        }
        return LogicalType.STRING;
    }

    /** The combination rule: identical stays, INT32 with DOUBLE is DOUBLE, anything else STRING. */
    public static LogicalType combine(LogicalType a, LogicalType b) {
        if (a == null) {
            return b;
        }
        if (b == null || a == b) {
            return a;
        }
        if ((a == LogicalType.INT32 && b == LogicalType.DOUBLE) || (a == LogicalType.DOUBLE && b == LogicalType.INT32)) {
            return LogicalType.DOUBLE;
        }
        return LogicalType.STRING;
    }

    private static LogicalType classifyText(String text) {
        if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
            return LogicalType.BOOLEAN;
        }
        if (INTEGER.matcher(text).matches()) {
            String digits = text.charAt(0) == '+' || text.charAt(0) == '-' ? text.substring(1) : text;
            if (digits.length() > 1 && digits.charAt(0) == '0') {
                return LogicalType.STRING; // a significant leading zero: an identifier, not a number
            }
            return fitsInt32(new BigInteger(text)) ? LogicalType.INT32 : LogicalType.STRING;
        }
        if (DECIMAL.matcher(text).matches()) {
            return Double.isFinite(Double.parseDouble(text)) ? LogicalType.DOUBLE : LogicalType.STRING;
        }
        if (parses(() -> LocalDate.parse(text))) {
            return LogicalType.DATE;
        }
        if (parses(() -> LocalTime.parse(text))) {
            return LogicalType.TIME;
        }
        if (parses(() -> LocalDateTime.parse(text))) {
            return LogicalType.LOCAL_DATETIME;
        }
        return LogicalType.STRING;
    }

    private static boolean fitsInt32(BigInteger value) {
        return value.compareTo(INT_MIN) >= 0 && value.compareTo(INT_MAX) <= 0;
    }

    private static boolean parses(Runnable parse) {
        try {
            parse.run();
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void checkOpen() {
        if (frozen != null) {
            throw new IllegalStateException("The schema is frozen");
        }
    }

    private static String checkName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A field name cannot be null or blank");
        }
        return name;
    }

    /** What the sample has shown about one field. */
    private static final class Field {
        private LogicalType type;
        private boolean nullable;

        void observe(SourceValue value) {
            if (value instanceof SourceValue.Scalar scalar) {
                type = combine(type, classify(scalar));
            } else {
                nullable = true; // NULL (MISSING is never stored in a sampled record)
            }
        }
    }
}
