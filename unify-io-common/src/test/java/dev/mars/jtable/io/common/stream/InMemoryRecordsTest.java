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
package dev.mars.jtable.io.common.stream;

import dev.mars.jtable.core.schema.InMemoryTable;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ConcurrentModificationException;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** An in-memory table behaves as an ordinary record source and sink. */
class InMemoryRecordsTest {

    private static final TableSchema SCHEMA =
            TableSchema.of(required("id", LogicalType.INT32), nullable("name", LogicalType.STRING));

    private static InMemoryTable table(int rows) {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        for (int i = 1; i <= rows; i++) {
            table.append(i, "n" + i);
        }
        return table;
    }

    @Test
    void copiesATableThroughTheStreamContractInOrder() throws IOException {
        InMemoryTable from = table(2500);
        InMemoryTable to = new InMemoryTable(SCHEMA);
        try (RecordSource source = InMemoryRecords.source(from, 1000); RecordSink sink = InMemoryRecords.sink(to)) {
            assertEquals(2500, RecordStreams.copy(source, sink));
        }
        assertEquals(2500, to.rowCount());
        assertEquals(1, to.get(0, "id"));
        assertEquals("n2500", to.get(2499, "name"));
    }

    @Test
    void sourceBatchesAreBoundedAndEndWithNull() throws IOException {
        try (RecordSource source = InMemoryRecords.source(table(5), 2)) {
            assertEquals(2, source.nextBatch().rowCount());
            assertEquals(2, source.nextBatch().rowCount());
            assertEquals(1, source.nextBatch().rowCount());
            assertNull(source.nextBatch());
        }
        try (RecordSource empty = InMemoryRecords.source(new InMemoryTable(SCHEMA))) {
            assertNull(empty.nextBatch());
        }
    }

    @Test
    void changingTheTableDuringAReadFails() throws IOException {
        InMemoryTable table = table(3);
        try (RecordSource source = InMemoryRecords.source(table, 1)) {
            source.nextBatch();
            table.append(4, "late");
            assertThrows(ConcurrentModificationException.class, source::nextBatch);
        }
    }

    @Test
    void sinkPublishesNothingUntilCompleteAndNothingOnClose() throws IOException {
        InMemoryTable to = new InMemoryTable(SCHEMA);
        RecordBatch batch = RecordBatch.builder(SCHEMA, 1).add(1, "a").build();
        try (RecordSink sink = InMemoryRecords.sink(to)) {
            sink.write(batch);
            assertEquals(0, to.rowCount());
        }
        assertEquals(0, to.rowCount(), "closing without completing discards the output");
    }

    @Test
    void sinkRejectsAForeignSchemaAndOverflowWithoutChangingTheTable() throws IOException {
        InMemoryTable to = new InMemoryTable(SCHEMA, 1);
        try (RecordSink sink = InMemoryRecords.sink(to)) {
            TableSchema other = TableSchema.of(required("x", LogicalType.INT32));
            assertThrows(IOException.class, () -> sink.write(RecordBatch.builder(other, 1).add(1).build()));
            assertThrows(IOException.class,
                    () -> sink.write(RecordBatch.builder(SCHEMA, 2).add(1, "a").add(2, "b").build()));
        }
        assertEquals(0, to.rowCount());
    }
}
