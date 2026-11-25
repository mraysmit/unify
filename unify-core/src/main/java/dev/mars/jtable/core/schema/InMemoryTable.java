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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The programmatic table of UC01: a fixed schema, a bounded number of records, whole-record
 * append and validated cell update (FR001). It is an in-memory source and sink that is loaded into
 * an execution engine like any other source, not working storage (ADR-001).
 *
 * <p>Every mutation is all-or-nothing: the whole record, or the new cell value, is checked by the
 * {@link ValueNormalizer} before anything changes, so a rejected mutation leaves the values and
 * the record count as they were. Records are copied on the way in and out, and views are
 * read-only. The schema is fixed for the life of the table: converting the columns of populated
 * data means building a new table. Zero-record tables are valid.</p>
 *
 * <p>Not thread-safe: "atomic" means no partial effect within one thread.</p>
 */
public final class InMemoryTable {

    /** The default record bound, matching the default operation limit of 100,000 records. */
    public static final int DEFAULT_MAX_RECORDS = 100_000;

    private final TableSchema schema;
    private final int maxRecords;
    private final List<Object[]> rows = new ArrayList<>();

    /** An empty table with the default record bound. */
    public InMemoryTable(TableSchema schema) {
        this(schema, DEFAULT_MAX_RECORDS);
    }

    /** An empty table holding at most {@code maxRecords} records. */
    public InMemoryTable(TableSchema schema, int maxRecords) {
        this.schema = Objects.requireNonNull(schema, "schema");
        if (maxRecords < 1) {
            throw new IllegalArgumentException("The record bound must be at least 1");
        }
        this.maxRecords = maxRecords;
    }

    public TableSchema schema() {
        return schema;
    }

    public int rowCount() {
        return rows.size();
    }

    public int maxRecords() {
        return maxRecords;
    }

    /**
     * Appends one record of typed values in schema order and returns its row index.
     *
     * @throws ValueConversionException when a value breaks its column's rules
     * @throws IllegalArgumentException when the value count is wrong
     * @throws IllegalStateException when the table is full
     */
    public int append(Object... values) {
        if (values.length != schema.size()) {
            throw new IllegalArgumentException("Expected " + schema.size() + " values but got " + values.length);
        }
        Object[] record = values.clone();
        for (int i = 0; i < record.length; i++) {
            ValueNormalizer.checkTyped(record[i], schema.column(i));
        }
        return add(record);
    }

    /**
     * Appends one positional record of source values, normalized through the full pipeline
     * (defaults, trimming, empty-as-null, conversion and rules). Returns the row index.
     */
    public int appendSource(List<? extends SourceValue> values) {
        return add(ValueNormalizer.normalizeRecord(schema, values));
    }

    /**
     * Appends one named record of source values; absent columns take MISSING and a field the
     * schema does not have is an error. Returns the row index.
     */
    public int appendSource(Map<String, ? extends SourceValue> fields) {
        return add(ValueNormalizer.normalizeRecord(schema, fields, false));
    }

    /**
     * Appends every record of a batch, or none: the batch must have this table's schema and fit
     * in the remaining capacity. Batch values are already checked against the schema.
     */
    public void appendAll(RecordBatch batch) {
        if (!batch.schema().equals(schema)) {
            throw new IllegalArgumentException("The batch schema does not match the table schema");
        }
        if ((long) rows.size() + batch.rowCount() > maxRecords) {
            throw new IllegalStateException("The table holds at most " + maxRecords + " records");
        }
        for (int r = 0; r < batch.rowCount(); r++) {
            rows.add(batch.row(r));
        }
    }

    /** The value at a row and column position. */
    public Object get(int row, int column) {
        return rows.get(row)[checkColumn(column)];
    }

    /** The value at a row in the named column. */
    public Object get(int row, String column) {
        return rows.get(row)[position(column)];
    }

    /** A copy of one record's values in schema order. */
    public Object[] row(int row) {
        return rows.get(row).clone();
    }

    /**
     * Replaces one cell after checking the new value against its column. A rejected value leaves
     * the cell unchanged. Returns the previous value.
     */
    public Object set(int row, int column, Object value) {
        Object[] record = rows.get(row);
        ValueNormalizer.checkTyped(value, schema.column(checkColumn(column)));
        Object previous = record[column];
        record[column] = value;
        return previous;
    }

    /** Replaces one cell in the named column; see {@link #set(int, int, Object)}. */
    public Object set(int row, String column, Object value) {
        return set(row, position(column), value);
    }

    /** A read-only view of the records; each record is a read-only list in schema order. */
    public List<List<Object>> rows() {
        return new AbstractList<>() {
            @Override
            public List<Object> get(int index) {
                return Collections.unmodifiableList(Arrays.asList(rows.get(index)));
            }

            @Override
            public int size() {
                return rows.size();
            }
        };
    }

    /**
     * Records {@code from} (inclusive) to {@code to} (exclusive) as one checked batch. This is how
     * the table acts as a source: an adapter hands these batches to an engine loader or a sink.
     */
    public RecordBatch batch(int from, int to) {
        Objects.checkFromToIndex(from, to, rows.size());
        RecordBatch.Builder builder = RecordBatch.builder(schema, to - from);
        for (int r = from; r < to; r++) {
            builder.add(rows.get(r));
        }
        return builder.build();
    }

    private int add(Object[] record) {
        if (rows.size() >= maxRecords) {
            throw new IllegalStateException("The table holds at most " + maxRecords + " records");
        }
        rows.add(record);
        return rows.size() - 1;
    }

    private int checkColumn(int column) {
        return Objects.checkIndex(column, schema.size());
    }

    private int position(String column) {
        int position = schema.indexOf(column);
        if (position < 0) {
            throw new IllegalArgumentException("Unknown column: " + column);
        }
        return position;
    }
}
