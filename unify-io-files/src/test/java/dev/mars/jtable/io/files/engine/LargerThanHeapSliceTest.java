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

import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.schema.ValueNormalizer;
import dev.mars.jtable.io.common.engine.DatasetHandle;
import dev.mars.jtable.io.common.stream.RecordSource;
import dev.mars.jtable.io.common.stream.RecordStreams;
import dev.mars.jtable.io.files.records.CsvFormat;
import dev.mars.jtable.io.files.records.CsvRecordSink;
import dev.mars.jtable.io.files.records.CsvRecordSource;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Stream;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The ADR-001 proof slice: CSV file -> DuckDB (appender) -> filter query -> CSV file, with Java
 * heap use sampled after garbage collection while records stream through.
 *
 * <p>By default the file is small so ordinary builds stay fast. The real proof uses a file several
 * times larger than the heap:</p>
 * <pre>
 * mvn -o test -pl unify-io-files -Dtest=LargerThanHeapSliceTest \
 *     -DargLine="-Xmx64m -Dunify.slice.megabytes=1024"
 * </pre>
 * <p>When {@code unify.slice.megabytes} is set, the test also requires the file to be at least
 * four times the maximum heap, so a run with too large a heap cannot pass as a proof.</p>
 */
class LargerThanHeapSliceTest {
    private static final long MB = 1024 * 1024;
    private static final String SIZE_PROPERTY = "unify.slice.megabytes";

    private static final TableSchema TRADES = TableSchema.of(
            required("id", LogicalType.INT32),
            required("account", LogicalType.STRING),
            nullable("amount", LogicalType.DOUBLE),
            required("active", LogicalType.BOOLEAN),
            required("trade_date", LogicalType.DATE),
            nullable("note", LogicalType.STRING));

    private static final TableSchema RESULT = TableSchema.of(
            required("id", LogicalType.INT32),
            required("account", LogicalType.STRING),
            nullable("amount", LogicalType.DOUBLE),
            required("trade_date", LogicalType.DATE));

    /** The filter query, written against the dataset's generated engine names. */
    private static String filter(DatasetHandle trades) {
        return "SELECT " + trades.column("id") + ", " + trades.column("account") + ", " + trades.column("amount")
                + ", " + trades.column("trade_date") + " FROM " + trades.table()
                + " WHERE " + trades.column("active") + " AND " + trades.column("amount") + " >= 900"
                + " ORDER BY " + trades.column("id");
    }

    private final List<Long> heapAfterGc = new ArrayList<>();

    @Test
    void csvLargerThanTheHeapFlowsThroughDuckDbWithFlatHeapUse() throws IOException {
        boolean proofRun = System.getProperty(SIZE_PROPERTY) != null;
        long targetBytes = Long.getLong(SIZE_PROPERTY, 4) * MB;
        long maxHeap = Runtime.getRuntime().maxMemory();
        Path work = Files.createDirectories(Path.of("target", "slice"));
        Path input = work.resolve("trades.csv");
        Path output = work.resolve("filtered.csv");
        Path database = work.resolve("slice.duckdb");
        deleteAll(work);
        Files.createDirectories(work);
        try {
            long started = System.nanoTime();
            Expected expected = generate(input, targetBytes);
            long csvBytes = Files.size(input);
            double generateSeconds = seconds(started);
            if (proofRun) {
                assertTrue(csvBytes >= 4 * maxHeap, "A proof run needs a CSV at least 4x the max heap: "
                        + csvBytes / MB + " MB file vs " + maxHeap / MB + " MB heap; lower -Xmx or raise " + SIZE_PROPERTY);
            }

            checkpoint();
            long loadStarted = System.nanoTime();
            DatasetHandle trades;
            long resultRows;
            double loadSeconds;
            double querySeconds;
            String engineVersion;
            try (DuckDbEngine engine = DuckDbEngine.open(database)) {
                engineVersion = engine.version();
                try (RecordSource csv = monitored(new CsvRecordSource(input, TRADES, CsvFormat.DEFAULT))) {
                    trades = engine.load("trades", csv);
                }
                loadSeconds = seconds(loadStarted);
                long queryStarted = System.nanoTime();
                try (RecordSource result = monitored(engine.query(filter(trades), RESULT, 1024));
                     CsvRecordSink sink = new CsvRecordSink(output, RESULT, CsvFormat.DEFAULT)) {
                    resultRows = RecordStreams.copy(result, sink);
                }
                querySeconds = seconds(queryStarted);
            }
            checkpoint();

            assertEquals(expected.rows, trades.rowCount(), "every generated record was loaded");
            assertEquals(expected.matchingRows, resultRows, "the filter returned the expected records");
            Expected written = verifyOutput(output);
            assertEquals(expected.matchingRows, written.rows, "output record count");
            assertEquals(expected.matchingIdSum, written.matchingIdSum, "output id checksum");

            long low = heapAfterGc.stream().min(Long::compare).orElseThrow();
            long high = heapAfterGc.stream().max(Long::compare).orElseThrow();
            System.out.printf("%n=== ADR-001 proof slice ===%n"
                            + "CSV input        %,d MB (%,d records), generated in %.1f s%n"
                            + "Max Java heap    %,d MB  -> input is %.1fx the heap%n"
                            + "Load into DuckDB %.1f s (appender, %s %s)%n"
                            + "Filter -> CSV    %.1f s, %,d records written (%,d MB)%n"
                            + "Heap after GC    min %.1f MB, max %.1f MB, spread %.1f MB over %d checkpoints%n",
                    csvBytes / MB, expected.rows, generateSeconds,
                    maxHeap / MB, (double) csvBytes / maxHeap,
                    loadSeconds, "duckdb", engineVersion,
                    querySeconds, resultRows, Files.size(output) / MB,
                    low / (double) MB, high / (double) MB, (high - low) / (double) MB, heapAfterGc.size());

            // Flat: heap retained after GC must not grow with the amount of data moved.
            assertTrue(high - low < 16 * MB, "retained heap grew by " + (high - low) / MB + " MB while streaming");
        } finally {
            deleteAll(work);
        }
    }

    /** Wraps a source so heap use is sampled after GC every 256 batches as records flow. */
    private RecordSource monitored(RecordSource source) {
        return new RecordSource() {
            private long batches = 0;

            @Override
            public TableSchema schema() {
                return source.schema();
            }

            @Override
            public RecordBatch nextBatch() throws IOException {
                RecordBatch batch = source.nextBatch();
                if (batch != null && ++batches % 256 == 0) {
                    checkpoint();
                }
                return batch;
            }

            @Override
            public void close() throws IOException {
                source.close();
            }
        };
    }

    private void checkpoint() {
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        heapAfterGc.add(runtime.totalMemory() - runtime.freeMemory());
    }

    private static final class Expected {
        long rows;
        long matchingRows;
        long matchingIdSum;
    }

    /** Writes a deterministic CSV of at least {@code targetBytes}, tracking what the filter should return. */
    private static Expected generate(Path file, long targetBytes) throws IOException {
        Expected expected = new Expected();
        SplittableRandom random = new SplittableRandom(42);
        LocalDate firstDay = LocalDate.of(2020, 1, 1);
        long written = 0;
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            out.write("id,account,amount,active,trade_date,note\n");
            for (int id = 1; written < targetBytes; id++) {
                Double amount = id % 1009 == 0 ? null : random.nextInt(100_000) / 100.0;
                boolean active = random.nextInt(4) != 0;
                String note = id % 997 == 0 ? "\"multi-line, \"\"quoted\"\"\nnote\"" : "n" + (id % 100);
                String line = id + ",ACC-" + (id % 5000) + ","
                        + (amount == null ? "\\N" : ValueNormalizer.toText(amount)) + ","
                        + active + "," + firstDay.plusDays(id % 2000) + "," + note + "\n";
                out.write(line);
                written += line.length();
                expected.rows++;
                if (active && amount != null && amount >= 900) {
                    expected.matchingRows++;
                    expected.matchingIdSum += id;
                }
            }
        }
        return expected;
    }

    /** Streams the output file back to count records and checksum ids, checking order too. */
    private static Expected verifyOutput(Path file) throws IOException {
        Expected seen = new Expected();
        int previous = Integer.MIN_VALUE;
        try (CsvRecordSource source = new CsvRecordSource(file, RESULT, CsvFormat.DEFAULT)) {
            for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
                for (int r = 0; r < batch.rowCount(); r++) {
                    int id = (Integer) batch.value(r, 0);
                    assertTrue(id > previous, "output is ordered by id");
                    assertTrue((Double) batch.value(r, 2) >= 900, "output respects the filter");
                    previous = id;
                    seen.rows++;
                    seen.matchingIdSum += id;
                }
            }
        }
        return seen;
    }

    private static double seconds(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1e9;
    }

    private static void deleteAll(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
