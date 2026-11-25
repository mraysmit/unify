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

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.core.schema.TableSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/** TD10: every source reports its size before any record is read (file, JDBC, supplied). */
class SizeProbesTest {

    @TempDir
    Path dir;

    @Test
    void aFileReportsItsExactByteSize() throws IOException {
        Path file = dir.resolve("in.csv");
        Files.writeString(file, "id\n1\n2\n");
        SourceSize size = SizeProbes.file(file);
        assertEquals(SourceSize.fileBytes(7), size);
        assertEquals("file-bytes", size.method().label());
        assertThrows(IOException.class, () -> SizeProbes.file(dir.resolve("missing.csv")));
    }

    @Test
    void aJdbcCountIsExactBelowTheLimitAndALowerBoundAtIt() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:probe;DB_CLOSE_DELAY=-1")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE t (id INT)");
                statement.execute("INSERT INTO t SELECT X FROM SYSTEM_RANGE(1, 250)");
            }
            SourceSize exact = SizeProbes.jdbc(connection, "SELECT id FROM t", 1000);
            assertEquals(SourceSize.exactCount(250), exact);
            assertTrue(exact.exact());

            SourceSize bounded = SizeProbes.jdbc(connection, "SELECT id FROM t", 100);
            assertEquals(SourceSize.Method.BOUNDED_COUNT, bounded.method());
            assertEquals(100, bounded.value());
            assertTrue(bounded.atLeast());

            SourceSize filtered = SizeProbes.jdbc(connection, "SELECT id FROM t WHERE id > 240", 100);
            assertEquals(SourceSize.exactCount(10), filtered);
            assertThrows(IOException.class, () -> SizeProbes.jdbc(connection, "SELECT nope FROM t", 10));
        }
    }

    @Test
    void aSuppliedTableReportsItsRecordCount() {
        ITable table = new TableCore();
        table.setColumns(new LinkedHashMap<>(Map.of("id", "int")));
        table.addRow(Map.of("id", "1"));
        table.addRow(Map.of("id", "2"));
        assertEquals(SourceSize.suppliedCount(2), SizeProbes.supplied(table));
    }

    @Test
    void sizesAreConsistentWithTheirMethod() {
        assertThrows(IllegalArgumentException.class,
                () -> new SourceSize(5, SourceSize.Unit.RECORDS, SourceSize.Method.FILE_BYTES, false));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceSize(5, SourceSize.Unit.RECORDS, SourceSize.Method.EXACT_COUNT, true));
        assertThrows(IllegalArgumentException.class, () -> SourceSize.fileBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> SourceSize.boundedCount(11, 10));
    }

    @Test
    void bytesNormaliseToAnUpperBoundOnRecords() {
        SizeEstimate estimate = SizeEstimate.fromBytes(SourceSize.fileBytes(1001), 10);
        assertEquals(101, estimate.records()); // rounded up: the conservative direction
        assertEquals(1001, estimate.bytes());
        assertTrue(estimate.bytesMeasured());
        assertFalse(estimate.recordsMeasured());
        assertThrows(IllegalArgumentException.class, () -> SizeEstimate.fromBytes(SourceSize.exactCount(5), 10));
    }

    @Test
    void recordsNormaliseToBytesFromDeclaredOrAverageWidths() {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("name", LogicalType.STRING));
        Map<LogicalType, Long> averages = Map.of(LogicalType.INT32, 4L, LogicalType.STRING, 20L);
        SizeEstimate estimate = SizeEstimate.fromRecords(SourceSize.exactCount(100), schema, Map.of(), averages);
        assertEquals(100, estimate.records());
        assertEquals(2400, estimate.bytes());
        SizeEstimate declared = SizeEstimate.fromRecords(SourceSize.exactCount(100), schema, Map.of("name", 50L), averages);
        assertEquals(5400, declared.bytes());
        assertThrows(IllegalArgumentException.class,
                () -> SizeEstimate.fromRecords(SourceSize.exactCount(1), schema, Map.of(), Map.of(LogicalType.INT32, 4L)));
    }

    @Test
    void estimatesAddUp() {
        SizeEstimate sum = SizeEstimate.fromBytes(SourceSize.fileBytes(100), 10)
                .plus(new SizeEstimate(5, 50, true, false));
        assertEquals(15, sum.records());
        assertEquals(150, sum.bytes());
        assertFalse(sum.recordsMeasured());
    }
}
