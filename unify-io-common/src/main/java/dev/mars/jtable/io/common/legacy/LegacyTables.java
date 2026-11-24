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
package dev.mars.jtable.io.common.legacy;

import dev.mars.jtable.core.model.IColumn;
import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.schema.ColumnDefinition;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts between the frozen legacy {@link ITable} model and the record-stream contract, so
 * existing demos and callers can feed execution engines and read small results (ADR-001).
 *
 * <p>Legacy columns allow null, so every converted column is nullable. Only the seven Java types
 * that map to a Unify logical type are accepted. Converting back to a table is bounded: a legacy
 * table holds everything in Java memory, so it is only for previews and small results.</p>
 */
public final class LegacyTables {

    private static final Map<Class<?>, LogicalType> TYPES = Map.of(
            String.class, LogicalType.STRING,
            Integer.class, LogicalType.INT32,
            Double.class, LogicalType.DOUBLE,
            Boolean.class, LogicalType.BOOLEAN,
            java.time.LocalDate.class, LogicalType.DATE,
            java.time.LocalTime.class, LogicalType.TIME,
            java.time.LocalDateTime.class, LogicalType.LOCAL_DATETIME);

    private static final Map<LogicalType, String> LEGACY_TYPE_NAMES = Map.of(
            LogicalType.STRING, "string",
            LogicalType.INT32, "int",
            LogicalType.DOUBLE, "double",
            LogicalType.BOOLEAN, "boolean",
            LogicalType.DATE, "date",
            LogicalType.TIME, "time",
            LogicalType.LOCAL_DATETIME, "datetime");

    private LegacyTables() {
    }

    /** The Unify schema of a legacy table's columns, all nullable. */
    public static TableSchema schemaOf(ITable table) {
        List<ColumnDefinition> columns = new ArrayList<>();
        for (IColumn<?> column : table.getColumns()) {
            LogicalType type = TYPES.get(column.getType());
            if (type == null) {
                throw new IllegalArgumentException("Column '" + column.getName()
                        + "' has Java type " + column.getType().getName() + ", which has no Unify logical type");
            }
            columns.add(ColumnDefinition.nullable(column.getName(), type));
        }
        return TableSchema.of(columns);
    }

    /**
     * Streams a legacy table's rows as checked records. The table is read, never modified; it must
     * not change while the source is in use.
     */
    public static RecordSource toRecordSource(ITable table, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        TableSchema schema = schemaOf(table);
        return new RecordSource() {
            private int next = 0;

            @Override
            public TableSchema schema() {
                return schema;
            }

            @Override
            public RecordBatch nextBatch() {
                int end = Math.min(table.getRowCount(), next + batchSize);
                if (next >= end) {
                    return null;
                }
                RecordBatch.Builder builder = RecordBatch.builder(schema, end - next);
                for (; next < end; next++) {
                    Object[] values = new Object[schema.size()];
                    for (int c = 0; c < values.length; c++) {
                        values[c] = table.getValueObject(next, schema.column(c).name());
                    }
                    builder.add(values);
                }
                return builder.build();
            }

            @Override
            public void close() {
            }
        };
    }

    /**
     * Reads a whole source into a new legacy table, failing if it has more than {@code maxRows}
     * records. Does not close the source.
     */
    public static ITable toTable(RecordSource source, String tableName, int maxRows) throws IOException {
        TableSchema schema = source.schema();
        TableCore table = new TableCore(tableName);
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (ColumnDefinition column : schema.columns()) {
            columns.put(column.name(), LEGACY_TYPE_NAMES.get(column.type()));
        }
        table.setColumns(columns);
        for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
            if (table.getRowCount() + batch.rowCount() > maxRows) {
                throw new IOException("The result has more than " + maxRows
                        + " records; legacy tables are for previews and small results only");
            }
            for (int r = 0; r < batch.rowCount(); r++) {
                IRow row = table.createRow();
                for (int c = 0; c < schema.size(); c++) {
                    row.setValue(schema.column(c).name(), batch.value(r, c));
                }
                table.addRow(row);
            }
        }
        return table;
    }
}
