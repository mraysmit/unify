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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static dev.mars.jtable.core.schema.ColumnDefinition.builder;
import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** FR001 and AC01: the programmatic table checks every mutation and changes nothing on failure. */
class InMemoryTableTest {

    private static final TableSchema SCHEMA = TableSchema.of(
            required("id", LogicalType.INT32),
            nullable("name", LogicalType.STRING),
            builder("score", LogicalType.INT32).bounds(0, 100).defaultValue(0).build(),
            nullable("joined", LogicalType.DATE));

    /** AC01. */
    @Test
    void anInvalidUpdateLeavesTheValueAndCountUnchanged() {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        assertEquals(0, table.append(1, "Ada", 90, LocalDate.of(2026, 1, 5)));

        assertThrows(ValueConversionException.class, () -> table.set(0, "score", 101));
        assertThrows(ValueConversionException.class, () -> table.set(0, "id", null));
        assertThrows(ValueConversionException.class, () -> table.set(0, "id", "1"));
        assertEquals(90, table.get(0, "score"));
        assertEquals(1, table.get(0, "id"));
        assertEquals(1, table.rowCount());

        assertEquals(90, table.set(0, "score", 95));
        assertEquals(95, table.get(0, 2));
    }

    @Test
    void anInvalidAppendAddsNothing() {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        assertThrows(ValueConversionException.class, () -> table.append(1, "Ada", 200, null));
        assertThrows(ValueConversionException.class, () -> table.append(null, "Ada", 1, null));
        assertThrows(IllegalArgumentException.class, () -> table.append(1, "Ada"));
        assertEquals(0, table.rowCount());
    }

    @Test
    void sourceRecordsGoThroughTheFullPipeline() {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        table.appendSource(Map.of("id", SourceValue.text("7"), "name", SourceValue.text("Grace")));
        assertArrayEquals(new Object[]{7, "Grace", 0, null}, table.row(0)); // score default, joined missing
        table.appendSource(List.of(SourceValue.text("8"), SourceValue.NULL, SourceValue.text("50"),
                SourceValue.text("2026-11-25")));
        assertEquals(LocalDate.of(2026, 11, 25), table.get(1, "joined"));

        assertThrows(ValueConversionException.class,
                () -> table.appendSource(Map.of("id", SourceValue.text("9"), "unknown", SourceValue.text("x"))));
        assertThrows(ValueConversionException.class,
                () -> table.appendSource(Map.of("id", SourceValue.text("9"), "score", SourceValue.text("-1"))));
        assertEquals(2, table.rowCount());
    }

    @Test
    void recordsAreCopiedAndViewsAreReadOnly() {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        Object[] values = {1, "Ada", 1, null};
        table.append(values);
        values[1] = "changed";
        assertEquals("Ada", table.get(0, "name"));

        Object[] copy = table.row(0);
        copy[1] = "changed";
        assertEquals("Ada", table.get(0, "name"));

        assertThrows(UnsupportedOperationException.class, () -> table.rows().get(0).set(1, "changed"));
        assertThrows(UnsupportedOperationException.class, () -> table.rows().add(List.of()));
        assertEquals(List.of(1, "Ada", 1), table.rows().get(0).subList(0, 3));
    }

    @Test
    void theRecordBoundIsEnforced() {
        InMemoryTable table = new InMemoryTable(SCHEMA, 2);
        table.append(1, null, 0, null);
        table.append(2, null, 0, null);
        assertThrows(IllegalStateException.class, () -> table.append(3, null, 0, null));
        assertEquals(2, table.rowCount());
    }

    @Test
    void batchesInAndOutKeepOrderAndAreAllOrNothing() {
        InMemoryTable table = new InMemoryTable(SCHEMA, 3);
        RecordBatch batch = RecordBatch.builder(SCHEMA, 2).add(1, "a", 1, null).add(2, "b", 2, null).build();
        table.appendAll(batch);
        assertEquals(2, table.rowCount());

        assertThrows(IllegalStateException.class, () -> table.appendAll(batch)); // would exceed the bound
        assertEquals(2, table.rowCount());
        TableSchema other = TableSchema.of(required("x", LogicalType.INT32));
        assertThrows(IllegalArgumentException.class,
                () -> table.appendAll(RecordBatch.builder(other, 1).add(1).build()));

        RecordBatch out = table.batch(0, 2);
        assertEquals(2, out.rowCount());
        assertEquals("b", out.value(1, 1));
        assertEquals(0, table.batch(2, 2).rowCount());
        assertThrows(IndexOutOfBoundsException.class, () -> table.batch(1, 3));
    }

    @Test
    void zeroRecordTablesAreValidAndUnknownColumnsFail() {
        InMemoryTable table = new InMemoryTable(SCHEMA);
        assertEquals(0, table.rowCount());
        assertTrue(table.rows().isEmpty());
        table.append(1, null, 0, null);
        assertThrows(IllegalArgumentException.class, () -> table.get(0, "ID"));
        assertThrows(IndexOutOfBoundsException.class, () -> table.get(0, 4));
        assertThrows(IllegalArgumentException.class, () -> new InMemoryTable(SCHEMA, 0));
    }
}
