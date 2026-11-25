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
package dev.mars.jtable.io.files.engine;

import dev.mars.jtable.engine.duckdb.DuckDbEngine;
import dev.mars.jtable.engine.sqlite.SqliteEngine;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.io.common.engine.DatasetHandle;
import dev.mars.jtable.io.common.engine.ExecutionEngine;
import dev.mars.jtable.io.common.legacy.LegacyTables;
import dev.mars.jtable.io.common.stream.RecordSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** The same contract checks run against every execution engine. */
class ExecutionEngineContractTest {
    private static final TableSchema ALL_TYPES = TableSchema.of(
            required("id", LogicalType.INT32),
            nullable("name", LogicalType.STRING),
            nullable("amount", LogicalType.DOUBLE),
            nullable("active", LogicalType.BOOLEAN),
            nullable("day", LogicalType.DATE),
            nullable("at", LogicalType.TIME),
            nullable("stamp", LogicalType.LOCAL_DATETIME));

    private static ExecutionEngine open(String engine) throws IOException {
        return engine.equals("duckdb") ? DuckDbEngine.inMemory() : SqliteEngine.inMemory();
    }

    private static RecordSource sourceOf(TableSchema schema, List<Object[]> rows, int batchSize) {
        Iterator<Object[]> it = rows.iterator();
        return new RecordSource() {
            @Override
            public TableSchema schema() {
                return schema;
            }

            @Override
            public RecordBatch nextBatch() {
                if (!it.hasNext()) {
                    return null;
                }
                RecordBatch.Builder builder = RecordBatch.builder(schema, batchSize);
                while (it.hasNext() && builder.rowCount() < batchSize) {
                    builder.add(it.next());
                }
                return builder.build();
            }

            @Override
            public void close() {
            }
        };
    }

    private static List<Object[]> drain(RecordSource source) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        try (source) {
            for (RecordBatch b = source.nextBatch(); b != null; b = source.nextBatch()) {
                for (int r = 0; r < b.rowCount(); r++) {
                    rows.add(b.row(r));
                }
            }
        }
        return rows;
    }

    private static List<Object[]> sampleRows() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1, "Alice", 10.5, true, LocalDate.of(2026, 11, 24), LocalTime.of(9, 30, 15),
                LocalDateTime.of(2026, 11, 24, 9, 30, 15)});
        rows.add(new Object[]{2, "", -0.25, false, LocalDate.of(1999, 12, 31), LocalTime.MIDNIGHT,
                LocalDateTime.of(2000, 1, 1, 0, 0)});
        rows.add(new Object[]{3, null, null, null, null, null, null});
        rows.add(new Object[]{4, "quote \" and ' marks", 1.0e10, true, LocalDate.of(2026, 1, 2), LocalTime.of(23, 59, 59),
                LocalDateTime.of(2026, 1, 2, 3, 4, 5)});
        return rows;
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void loadAndScanRoundTripEveryLogicalTypeAndNull(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            List<Object[]> rows = sampleRows();
            DatasetHandle handle = engine.load("people", sourceOf(ALL_TYPES, rows, 3));
            assertEquals(4, handle.rowCount());
            assertEquals(engineName, handle.engine());
            assertFalse(engine.version().isBlank());
            List<Object[]> back = drain(engine.scan(handle, 2));
            assertEquals(rows.size(), back.size());
            for (int i = 0; i < rows.size(); i++) {
                assertArrayEquals(rows.get(i), back.get(i), engineName + " row " + i);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void aFailedLoadLeavesNoDatasetBehind(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            RecordSource failing = new RecordSource() {
                private int calls = 0;

                @Override
                public TableSchema schema() {
                    return ALL_TYPES;
                }

                @Override
                public RecordBatch nextBatch() throws IOException {
                    if (calls++ == 0) {
                        return RecordBatch.builder(ALL_TYPES, 1).add(sampleRows().get(0)).build();
                    }
                    throw new IOException("source failed part-way");
                }

                @Override
                public void close() {
                }
            };
            assertThrows(IOException.class, () -> engine.load("broken", failing));
            DatasetHandle retry = engine.load("broken", sourceOf(ALL_TYPES, sampleRows(), 10));
            assertEquals(4, drain(engine.scan(retry, 10)).size(), "the failed load left no table or rows");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void queriesStreamResultsCheckedAgainstTheDeclaredSchema(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            DatasetHandle people = engine.load("people", sourceOf(ALL_TYPES, sampleRows(), 10));
            String id = people.column("id"), name = people.column("name"), active = people.column("active");
            TableSchema result = TableSchema.of(required("id", LogicalType.INT32), nullable("name", LogicalType.STRING),
                    nullable("active", LogicalType.BOOLEAN));
            List<Object[]> rows = drain(engine.query("SELECT " + id + ", " + name + ", " + active + " FROM "
                    + people.table() + " WHERE " + active + " ORDER BY " + id, result, 1));
            assertEquals(2, rows.size());
            assertArrayEquals(new Object[]{1, "Alice", true}, rows.get(0));
            assertArrayEquals(new Object[]{4, "quote \" and ' marks", true}, rows.get(1));

            // A COUNT is a 64-bit integer in both engines; it fits INT32, so it converts.
            List<Object[]> count = drain(engine.query("SELECT COUNT(*) FROM " + people.table(),
                    TableSchema.of(required("n", LogicalType.INT32)), 10));
            assertEquals(4, count.get(0)[0]);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void resultsThatDoNotMatchTheDeclaredSchemaFail(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            DatasetHandle people = engine.load("people", sourceOf(ALL_TYPES, sampleRows(), 10));
            String table = people.table(), id = people.column("id"), name = people.column("name");
            TableSchema wrongCount = TableSchema.of(required("id", LogicalType.INT32));
            assertThrows(IOException.class, () -> engine.query("SELECT " + id + ", " + name + " FROM " + table, wrongCount, 10));
            // Text is never coerced into a number.
            TableSchema wrongType = TableSchema.of(nullable("name", LogicalType.INT32));
            assertThrows(IOException.class, () -> drain(engine.query("SELECT " + name + " FROM " + table, wrongType, 10)));
            // NULL in a column declared non-nullable is rejected.
            TableSchema notNull = TableSchema.of(required("name", LogicalType.STRING));
            assertThrows(IOException.class, () -> drain(engine.query(
                    "SELECT " + name + " FROM " + table + " ORDER BY " + id, notNull, 10)));
            // Loading a second dataset with the same name is refused.
            assertThrows(IOException.class, () -> engine.load("people", sourceOf(ALL_TYPES, sampleRows(), 10)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void namesThatDifferOnlyInCaseStayDistinct(String engineName) throws IOException {
        // FR001: column names are case-sensitive, but both engines match identifiers case-insensitively.
        try (ExecutionEngine engine = open(engineName)) {
            TableSchema schema = TableSchema.of(nullable("Name", LogicalType.STRING), nullable("name", LogicalType.STRING));
            DatasetHandle upper = engine.load("Data", sourceOf(schema, List.<Object[]>of(new Object[]{"A", "a"}), 10));
            DatasetHandle lower = engine.load("data", sourceOf(schema, List.<Object[]>of(new Object[]{"B", "b"}), 10));
            assertNotEquals(upper.column("Name"), upper.column("name"));
            assertNotEquals(upper.table(), lower.table());
            assertArrayEquals(new Object[]{"A", "a"}, drain(engine.scan(upper, 10)).get(0));
            assertArrayEquals(new Object[]{"B", "b"}, drain(engine.scan(lower, 10)).get(0));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void scansReturnRecordsInLoadOrder(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            TableSchema schema = TableSchema.of(required("n", LogicalType.INT32));
            List<Object[]> rows = new java.util.ArrayList<>();
            for (int i = 0; i < 5000; i++) {
                rows.add(new Object[]{(i * 7919) % 5000});
            }
            DatasetHandle handle = engine.load("shuffled", sourceOf(schema, rows, 333));
            List<Object[]> back = drain(engine.scan(handle, 500));
            for (int i = 0; i < rows.size(); i++) {
                assertEquals(rows.get(i)[0], back.get(i)[0], "record " + (i + 1));
            }
        }
    }

    @org.junit.jupiter.api.Test
    void sqliteStoresTemporalValuesAsFixedWidthText() throws IOException {
        try (ExecutionEngine engine = SqliteEngine.inMemory()) {
            TableSchema schema = TableSchema.of(nullable("at", LogicalType.TIME), nullable("stamp", LogicalType.LOCAL_DATETIME),
                    nullable("day", LogicalType.DATE));
            DatasetHandle handle = engine.load("times", sourceOf(schema, List.<Object[]>of(
                    new Object[]{LocalTime.of(10, 15), LocalDateTime.of(2026, 1, 2, 3, 4), LocalDate.of(2026, 1, 2)}), 10));
            // Read the stored text directly: zero seconds and fractions are always written out.
            TableSchema asText = TableSchema.of(nullable("at", LogicalType.STRING), nullable("stamp", LogicalType.STRING),
                    nullable("day", LogicalType.STRING));
            Object[] stored = drain(engine.query("SELECT " + String.join(", ", handle.columns()) + " FROM " + handle.table(),
                    asText, 10)).get(0);
            assertArrayEquals(new Object[]{"10:15:00.000000000", "2026-01-02T03:04:00.000000000", "2026-01-02"}, stored);
            // A literal written the same way matches: equality works in SQL, not just in Java.
            List<Object[]> match = drain(engine.query("SELECT COUNT(*) FROM " + handle.table() + " WHERE "
                    + handle.column("at") + " = '10:15:00.000000000'", TableSchema.of(required("n", LogicalType.INT32)), 10));
            assertEquals(1, match.get(0)[0]);
            assertArrayEquals(new Object[]{LocalTime.of(10, 15), LocalDateTime.of(2026, 1, 2, 3, 4), LocalDate.of(2026, 1, 2)},
                    drain(engine.scan(handle, 10)).get(0));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void namesAreQuotedSafelyAndDroppingIsIdempotent(String engineName) throws IOException {
        try (ExecutionEngine engine = open(engineName)) {
            TableSchema odd = TableSchema.of(required("select", LogicalType.INT32), nullable("my \"col\"", LogicalType.STRING));
            List<Object[]> rows = List.<Object[]>of(new Object[]{1, "x"});
            DatasetHandle handle = engine.load("order by", sourceOf(odd, rows, 10));
            assertArrayEquals(new Object[]{1, "x"}, drain(engine.scan(handle, 10)).get(0));
            engine.drop("order by");
            engine.drop("order by");
            assertThrows(IOException.class, () -> drain(engine.scan(handle, 10)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"duckdb", "sqlite"})
    void legacyTablesConvertToRecordsAndBackWithABound(String engineName) throws IOException {
        ITable legacy = new TableBuilder()
                .addStringColumn("name").addIntColumn("age").addDoubleColumn("score").addBooleanColumn("member")
                .addRow("name", "Alice", "age", "30", "score", "1.5", "member", "true")
                .addRow("name", "Bob", "age", "41", "score", "2.25", "member", "false")
                .build();
        try (ExecutionEngine engine = open(engineName)) {
            DatasetHandle handle = engine.load("legacy", LegacyTables.toRecordSource(legacy, 1));
            assertEquals(2, handle.rowCount());
            try (RecordSource scan = engine.scan(handle, 10)) {
                ITable back = LegacyTables.toTable(scan, "back", 10);
                assertEquals(2, back.getRowCount());
                assertEquals("Bob", back.getValueObject(1, "name"));
                assertEquals(41, back.getValueObject(1, "age"));
                assertEquals(2.25, back.getValueObject(1, "score"));
                assertEquals(false, back.getValueObject(1, "member"));
            }
            try (RecordSource scan = engine.scan(handle, 10)) {
                assertThrows(IOException.class, () -> LegacyTables.toTable(scan, "too-big", 1));
            }
        }
    }
}
