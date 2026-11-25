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
package dev.mars.jtable.engine.sqlite;

import dev.mars.jtable.core.schema.InMemoryTable;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.engine.DatasetHandle;
import dev.mars.jtable.io.common.stream.InMemoryRecords;
import dev.mars.jtable.io.common.stream.RecordSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** The SQLite engine requirements of stage 2c: minimum version, memory cap, packaging. */
class SqliteEngineTest {

    private static final TableSchema SCHEMA =
            TableSchema.of(required("id", LogicalType.INT32), required("text", LogicalType.STRING));

    private static InMemoryTable rows(int count, int textLength) {
        InMemoryTable table = new InMemoryTable(SCHEMA, Math.max(1, count));
        String text = "x".repeat(textLength);
        for (int i = 1; i <= count; i++) {
            table.append(i, text);
        }
        return table;
    }

    @ParameterizedTest
    @CsvSource({"3.39.0,true", "3.39,true", "3.42.0,true", "3.100.0,true", "4.0.0,true",
            "3.38.5,false", "3.37.2,false", "2.99.99,false"})
    void versionsCompareByNumericParts(String version, boolean supported) {
        assertEquals(supported, SqliteEngine.isSupported(version));
    }

    @Test
    void anUnparseableVersionIsAnError() {
        assertThrows(IllegalArgumentException.class, () -> SqliteEngine.isSupported("3.x"));
        assertThrows(IllegalArgumentException.class, () -> SqliteEngine.isSupported(null));
    }

    /** Release verification: the bundled SQLite meets the minimum. */
    @Test
    void theBundledLibraryMeetsTheMinimumVersion() throws IOException {
        try (SqliteEngine engine = SqliteEngine.inMemory()) {
            assertTrue(SqliteEngine.isSupported(engine.version()),
                    "bundled SQLite " + engine.version() + " is older than " + SqliteEngine.MINIMUM_VERSION);
        }
    }

    @Test
    void inMemoryEnginesAreCappedByDefault() throws IOException {
        try (SqliteEngine engine = SqliteEngine.inMemory()) {
            assertEquals(SqliteEngine.DEFAULT_MEMORY_CAP, engine.memoryCap());
        }
        assertThrows(IllegalArgumentException.class, () -> SqliteEngine.inMemory(0));
    }

    @Test
    void aLoadThatReachesTheCapFailsClearlyAndLeavesTheEngineUsable() throws IOException {
        try (SqliteEngine engine = SqliteEngine.inMemory(1024 * 1024)) {
            // About 4 MB of text against a 1 MiB cap.
            IOException e = assertThrows(IOException.class,
                    () -> engine.load("big", InMemoryRecords.source(rows(20_000, 200))));
            assertTrue(e.getMessage().contains("memory cap"), e.getMessage());

            // The failed load was rolled back; a small dataset still fits.
            DatasetHandle small = engine.load("small", InMemoryRecords.source(rows(10, 10)));
            assertEquals(10, small.rowCount());
            try (RecordSource scan = engine.scan(small, 100)) {
                RecordBatch batch = scan.nextBatch();
                assertEquals(10, batch.rowCount());
                assertEquals(1, batch.value(0, 0));
            }
        }
    }

    /** Packaging evidence: this module ships SQLite without the DuckDB driver. */
    @Test
    void theDuckDbDriverIsNotOnTheClassPath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.duckdb.DuckDBDriver"));
    }
}
