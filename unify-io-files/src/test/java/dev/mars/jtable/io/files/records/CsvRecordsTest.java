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

import dev.mars.jtable.core.schema.ColumnDefinition;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.schema.TextConversion;
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

    // ---- Stage 2b: the full normalization pipeline and schema inference ----

    private List<Object[]> drain(CsvRecordSource source) throws IOException {
        List<Object[]> rows = new ArrayList<>();
        for (RecordBatch b = source.nextBatch(); b != null; b = source.nextBatch()) {
            for (int r = 0; r < b.rowCount(); r++) {
                rows.add(b.row(r));
            }
        }
        return rows;
    }

    @Test
    void columnTextSettingsAndRulesApplyToCsvInput() throws IOException {
        TableSchema schema = TableSchema.of(
                ColumnDefinition.builder("qty", LogicalType.INT32).required().trim(TextConversion.Trim.BOTH)
                        .bounds(1, 10).build(),
                ColumnDefinition.builder("day", LogicalType.DATE).pattern("dd/MM/uuuu").build());
        Path ok = csv("qty,day\n 3 ,25/11/2026\n");
        assertArrayEquals(new Object[]{3, LocalDate.of(2026, 11, 25)}, readAll(ok, schema, CsvFormat.DEFAULT).get(0));
        Path tooBig = csv("qty,day\n3,25/11/2026\n11,25/11/2026\n");
        IOException e = assertThrows(IOException.class, () -> readAll(tooBig, schema, CsvFormat.DEFAULT));
        assertTrue(e.getMessage().startsWith("Line 3: Column 'qty'"), e.getMessage());
    }

    @Test
    void infersTheSchemaFromABoundedSampleAndDeliversTheSampleFirst() throws IOException {
        StringBuilder content = new StringBuilder("id,amount,code,flag\n");
        content.append("1,10,007,\\N\n").append("2,2.5,008,\\N\n");
        for (int i = 3; i <= 2500; i++) {
            content.append(i).append(',').append(i).append(",0").append(i).append(",\\N\n");
        }
        Path file = csv(content.toString());
        try (CsvRecordSource source = CsvRecordSource.inferring(file, CsvFormat.DEFAULT.withBatchSize(700), 1000)) {
            assertEquals(TableSchema.of(required("id", LogicalType.INT32), required("amount", LogicalType.DOUBLE),
                    required("code", LogicalType.STRING), nullable("flag", LogicalType.STRING)), source.schema());
            List<Object[]> rows = drain(source);
            assertEquals(2500, rows.size());
            assertArrayEquals(new Object[]{1, 10.0, "007", null}, rows.get(0));
            assertArrayEquals(new Object[]{1000, 1000.0, "01000", null}, rows.get(999));
            assertArrayEquals(new Object[]{2500, 2500.0, "02500", null}, rows.get(2499));
        }
    }

    @Test
    void aValueAfterTheSampleThatBreaksTheFrozenSchemaIsALocatedError() throws IOException {
        Path file = csv("id,amount\n1,1\n2,2\n3,x\n");
        try (CsvRecordSource source = CsvRecordSource.inferring(file, CsvFormat.DEFAULT, 2)) {
            assertEquals(LogicalType.INT32, source.schema().column("amount").type());
            IOException e = assertThrows(IOException.class, () -> drain(source));
            assertTrue(e.getMessage().startsWith("Line 4: Column 'amount'"), e.getMessage());
        }
    }

    @Test
    void aHeaderAloneInfersNullableStringsAndEmptyOrHeaderlessFilesNeedASchema() throws IOException {
        try (CsvRecordSource source = CsvRecordSource.inferring(csv("a,b\n"), CsvFormat.DEFAULT)) {
            assertEquals(TableSchema.of(nullable("a", LogicalType.STRING), nullable("b", LogicalType.STRING)),
                    source.schema());
            assertNull(source.nextBatch());
        }
        assertThrows(IOException.class, () -> CsvRecordSource.inferring(csv(""), CsvFormat.DEFAULT));
        assertThrows(IOException.class, () -> CsvRecordSource.inferring(csv("a,a\n1,2\n"), CsvFormat.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecordSource.inferring(csv("1,2\n"), CsvFormat.DEFAULT.withHeader(false)));
    }
    @Test
    void theMinimumRecordWidthGivesAnUpperBoundOnRecordsFromFileBytes() throws IOException {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("note", LogicalType.STRING),
                required("flag", LogicalType.BOOLEAN), nullable("day", LogicalType.DATE));
        // id >= 1, note >= 0, flag >= 4 ("true"), day: null token \N (2 bytes); 3 delimiters; 1 line break.
        assertEquals(1 + 0 + 4 + 2 + 3 + 1, CsvFormat.DEFAULT.minimumRecordWidth(schema));
        assertEquals(1 + 0 + 4 + 0 + 3 + 1, CsvFormat.DEFAULT.withEmptyAsNull(true).minimumRecordWidth(schema));

        Path file = csv("id,note,flag,day\n1,,true,\\N\n22,abc,false,2026-11-25\n3,,true,\\N");
        long bytes = Files.size(file);
        long bound = dev.mars.jtable.io.common.size.SizeEstimate.fromBytes(
                dev.mars.jtable.io.common.size.SourceSize.fileBytes(bytes), CsvFormat.DEFAULT.minimumRecordWidth(schema)).records();
        assertTrue(bound >= readAll(file, schema, CsvFormat.DEFAULT).size(), "the estimate must not undercount");
    }}
