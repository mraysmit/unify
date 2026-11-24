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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A bounded group of records that have already been checked against a schema. Every value is null
 * (only in a nullable column) or an instance of its column's canonical Java class, so consumers
 * never re-validate. Batches are immutable once built and are the unit in which records move
 * between connectors and execution engines; no component holds a whole dataset of them.
 */
public final class RecordBatch {
    private final TableSchema schema;
    private final List<Object[]> rows;

    private RecordBatch(TableSchema schema, List<Object[]> rows) {
        this.schema = schema;
        this.rows = rows;
    }

    public static Builder builder(TableSchema schema, int expectedRows) {
        return new Builder(schema, expectedRows);
    }

    public TableSchema schema() {
        return schema;
    }

    public int rowCount() {
        return rows.size();
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /** The value at a row and column position. */
    public Object value(int row, int column) {
        return rows.get(row)[column];
    }

    /** A copy of one record's values in schema order. */
    public Object[] row(int row) {
        return rows.get(row).clone();
    }

    /** Builds a batch, checking each record against the schema as it is added. */
    public static final class Builder {
        private final TableSchema schema;
        private List<Object[]> rows;

        private Builder(TableSchema schema, int expectedRows) {
            this.schema = Objects.requireNonNull(schema, "schema");
            this.rows = new ArrayList<>(Math.max(1, expectedRows));
        }

        /**
         * Adds one record of typed values in schema order. The whole record is checked before it
         * is added, so a rejected record leaves the batch unchanged.
         */
        public Builder add(Object... values) {
            if (rows == null) {
                throw new IllegalStateException("The batch has already been built");
            }
            if (values.length != schema.size()) {
                throw new IllegalArgumentException(
                        "Expected " + schema.size() + " values but got " + values.length);
            }
            Object[] record = values.clone();
            for (int i = 0; i < record.length; i++) {
                ValueNormalizer.checkTyped(record[i], schema.column(i));
            }
            rows.add(record);
            return this;
        }

        public int rowCount() {
            return rows == null ? 0 : rows.size();
        }

        public RecordBatch build() {
            if (rows == null) {
                throw new IllegalStateException("The batch has already been built");
            }
            RecordBatch batch = new RecordBatch(schema, java.util.Collections.unmodifiableList(rows));
            rows = null;
            return batch;
        }
    }
}
