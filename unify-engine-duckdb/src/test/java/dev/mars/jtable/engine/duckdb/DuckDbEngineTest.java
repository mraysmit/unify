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
package dev.mars.jtable.engine.duckdb;

import dev.mars.jtable.core.schema.InMemoryTable;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.engine.DatasetHandle;
import dev.mars.jtable.io.common.stream.InMemoryRecords;
import dev.mars.jtable.io.common.stream.RecordSource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** The DuckDB engine works from its own module; the full contract suite runs in unify-io-files. */
class DuckDbEngineTest {

    @Test
    void loadsAndScansAnInMemoryTableInOrder() throws IOException {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("day", LogicalType.DATE));
        InMemoryTable table = new InMemoryTable(schema);
        table.append(2, LocalDate.of(2026, 11, 25));
        table.append(1, null);
        try (DuckDbEngine engine = DuckDbEngine.inMemory()) {
            assertEquals("duckdb", engine.name());
            DatasetHandle handle = engine.load("t", InMemoryRecords.source(table));
            try (RecordSource scan = engine.scan(handle, 10)) {
                RecordBatch batch = scan.nextBatch();
                assertEquals(2, batch.rowCount());
                assertArrayEquals(new Object[]{2, LocalDate.of(2026, 11, 25)}, batch.row(0));
                assertArrayEquals(new Object[]{1, null}, batch.row(1));
                assertNull(scan.nextBatch());
            }
        }
    }
}
