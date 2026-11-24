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
package dev.mars.jtable.io.files.records;

import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.stream.RecordStreams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

class CsvRecordsTest {
    private static final TableSchema SCHEMA = TableSchema.of(
            required("id", LogicalType.INT32),
            nullable("note", LogicalType.STRING),
            nullable("amount", LogicalType.DOUBLE),
            nullable("day", LogicalType.DATE));

    @TempDir
    Path dir;

    private Path csv(String content) throws IOException {
        Path file = Files.createTempFile(dir, "in", ".csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file;
    }

    private List<Object[]> readAll(Path file, TableSchema schema, CsvFormat format) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        try (CsvRecordSource source = new CsvRecordSource(file, schema, format)) {
            for (RecordBatch b = source.nextBatch(); b != null; b = source.nextBatch()) {
                for (int r = 0; r < b.rowCount(); r++) {
                    rows.add(b.row(r));
                }
            }
        }
        return rows;
    }

    @Test
    void parsesQuotedDelimitersDoubledQuotesLineBreaksAndCrlf() throws IOException {
        Path file = csv("id,note,amount,day\r\n"
                + "1,\"a, b\",1.5,2026-11-24\r\n"
                + "2,\"say \"\"hi\"\"\",\\N,\\N\n"
                + "3,\"two\nlines\",-2e2,2026-01-02\r"
                + "4,plain,0.25,2026-03-04");
        List<Object[]> rows = readAll(file, SCHEMA, CsvFormat.DEFAULT.withBatchSize(2));
        assertEquals(4, rows.size());
        assertArrayEquals(new Object[]{1, "a, b", 1.5, LocalDate.of(2026, 11, 24)}, rows.get(0));
        assertArrayEquals(new Object[]{2, "say \"hi\"", null, null}, rows.get(1));
        assertArrayEquals(new Object[]{3, "two\nlines", -200.0, LocalDate.of(2026, 1, 2)}, rows.get(2));
        assertArrayEquals(new Object[]{4, "plain", 0.25, LocalDate.of(2026, 3, 4)}, rows.get(3));
    }

    @Test
    void nullTokenEmptyAndQuotedLiteralsStayDistinct() throws IOException {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("s", LogicalType.STRING));
        Path file = csv("id,s\n1,\\N\n2,\"\\N\"\n3,\n4,\"\"\n");
        List<Object[]> rows = readAll(file, schema, CsvFormat.DEFAULT);
        assertNull(rows.get(0)[1]);
        assertEquals("\\N", rows.get(1)[1]);
        assertEquals("", rows.get(2)[1]);
        assertEquals("", rows.get(3)[1]);
        List<Object[]> emptyAsNull = readAll(file, schema, CsvFormat.DEFAULT.withEmptyAsNull(true));
        assertNull(emptyAsNull.get(2)[1]);
        assertEquals("", emptyAsNull.get(3)[1], "a quoted empty field is always a literal");
    }

    @Test
    void emptyTextInANonStringColumnFailsUnlessEmptyAsNull() throws IOException {
        Path file = csv("id,note,amount,day\n1,x,,2026-11-24\n");
        IOException e = assertThrows(IOException.class, () -> readAll(file, SCHEMA, CsvFormat.DEFAULT));
        assertTrue(e.getMessage().startsWith("Line 2:"), e.getMessage());
        assertNull(readAll(file, SCHEMA, CsvFormat.DEFAULT.withEmptyAsNull(true)).get(0)[2]);
    }

    @Test
    void errorsAreLocatedByStartingLineAndDoNotEchoValues() throws IOException {
        Path file = csv("id,note,amount,day\n1,\"multi\nline\",1,2026-11-24\n2,ok,secret-value,2026-11-24\n");
        IOException e = assertThrows(IOException.class, () -> readAll(file, SCHEMA, CsvFormat.DEFAULT));
        assertTrue(e.getMessage().startsWith("Line 4:"), e.getMessage());
        assertTrue(e.getMessage().contains("amount"));
        assertFalse(e.getMessage().contains("secret-value"));
    }

    @Test
    void rejectsWrongWidthBlankRecordsBadHeaderAndBrokenQuoting() throws IOException {
        assertThrows(IOException.class, () -> readAll(csv("id,note,amount,day\n1,x,1\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv("id,note,amount,day\n\n1,x,1,2026-11-24\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv("id,Note,amount,day\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv("id,note,amount,day\n1,\"open,1,2026-11-24\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv("id,note,amount,day\n1,\"a\"b,1,2026-11-24\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv("id,note,amount,day\n1,a\"b,1,2026-11-24\n"), SCHEMA, CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> readAll(csv(""), SCHEMA, CsvFormat.DEFAULT));
    }

    @Test
    void headerlessFilesAndByteOrderMarksAreSupported() throws IOException {
        Path file = csv("\uFEFF7,x,1,2026-11-24\n");
        List<Object[]> rows = readAll(file, SCHEMA, CsvFormat.DEFAULT.withHeader(false));
        assertEquals(7, rows.get(0)[0]);
    }

    @Test
    void sinkRoundTripsNullsEmptiesAndSpecialCharacters() throws IOException {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("s", LogicalType.STRING),
                nullable("d", LogicalType.DOUBLE));
        RecordBatch batch = RecordBatch.builder(schema, 5)
                .add(1, null, null)
                .add(2, "", 1.0e10)
                .add(3, "\\N", -0.5)
                .add(4, "a,\"b\"\nc", 2.0)
                .build();
        Path out = dir.resolve("out.csv");
        try (CsvRecordSink sink = new CsvRecordSink(out, schema, CsvFormat.DEFAULT)) {
            sink.write(batch);
            sink.complete();
        }
        List<Object[]> back = readAll(out, schema, CsvFormat.DEFAULT);
        for (int r = 0; r < batch.rowCount(); r++) {
            assertArrayEquals(batch.row(r), back.get(r), "row " + r);
        }
    }

    @Test
    void sinkPublishesOnlyOnCompleteAndNeverOverwrites() throws IOException {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32));
        Path out = dir.resolve("partial.csv");
        try (CsvRecordSink sink = new CsvRecordSink(out, schema, CsvFormat.DEFAULT)) {
            sink.write(RecordBatch.builder(schema, 1).add(1).build());
            assertFalse(Files.exists(out), "nothing is visible before complete()");
        }
        assertFalse(Files.exists(out), "closing without complete() discards the output");
        try (var files = Files.list(dir)) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".partial")), "temporary file removed");
        }
        Files.writeString(out, "existing");
        assertThrows(FileAlreadyExistsException.class, () -> new CsvRecordSink(out, schema, CsvFormat.DEFAULT));
        assertEquals("existing", Files.readString(out));
    }

    @Test
    void copyStreamsSourceToSink() throws IOException {
        Path in = csv("id,note,amount,day\n1,a,1,2026-11-24\n2,b,2,\\N\n");
        Path out = dir.resolve("copy.csv");
        try (CsvRecordSource source = new CsvRecordSource(in, SCHEMA, CsvFormat.DEFAULT.withBatchSize(1));
             CsvRecordSink sink = new CsvRecordSink(out, SCHEMA, CsvFormat.DEFAULT)) {
            assertEquals(2, RecordStreams.copy(source, sink));
        }
        assertEquals("id,note,amount,day\n1,a,1.0,2026-11-24\n2,b,2.0,\\N\n", Files.readString(out));
    }
}
