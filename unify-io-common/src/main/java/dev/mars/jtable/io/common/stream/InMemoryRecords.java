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
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * Adapts an {@link InMemoryTable} to the record-stream contract, so a programmatic table is
 * loaded into an engine, or filled from one, exactly like any other source or sink.
 */
public final class InMemoryRecords {

    /** The default batch size for reading a table. */
    public static final int DEFAULT_BATCH_SIZE = 1024;

    private InMemoryRecords() {
    }

    /** A source over the table's records in order, in batches of {@link #DEFAULT_BATCH_SIZE}. */
    public static RecordSource source(InMemoryTable table) {
        return source(table, DEFAULT_BATCH_SIZE);
    }

    /**
     * A source over the table's records in order. The table must not change while the source is
     * read: appending records during the read fails the next batch rather than silently mixing
     * old and new data.
     */
    public static RecordSource source(InMemoryTable table, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("The batch size must be at least 1");
        }
        return new RecordSource() {
            private final int rows = table.rowCount();
            private int next = 0;
            private boolean closed = false;

            @Override
            public TableSchema schema() {
                return table.schema();
            }

            @Override
            public RecordBatch nextBatch() throws IOException {
                if (closed) {
                    throw new IOException("The source is closed");
                }
                if (table.rowCount() != rows) {
                    throw new ConcurrentModificationException("The table changed while it was being read");
                }
                if (next >= rows) {
                    return null;
                }
                int to = Math.min(rows, next + batchSize);
                RecordBatch batch = table.batch(next, to);
                next = to;
                return batch;
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    /**
     * A sink that appends to the table. Following the sink contract, nothing is appended until
     * {@link RecordSink#complete()}, and then everything is appended or nothing is: batches that
     * would exceed the table's bound fail the completion and leave the table unchanged.
     */
    public static RecordSink sink(InMemoryTable table) {
        return new RecordSink() {
            private final List<RecordBatch> pending = new ArrayList<>();
            private long pendingRows = 0;
            private boolean done = false;

            @Override
            public void write(RecordBatch batch) throws IOException {
                if (done) {
                    throw new IOException("The sink is completed or closed");
                }
                if (!batch.schema().equals(table.schema())) {
                    throw new IOException("The batch schema does not match the table schema");
                }
                pendingRows += batch.rowCount();
                if (table.rowCount() + pendingRows > table.maxRecords()) {
                    throw new IOException("The table holds at most " + table.maxRecords() + " records");
                }
                pending.add(batch);
            }

            @Override
            public void complete() throws IOException {
                if (done) {
                    throw new IOException("The sink is completed or closed");
                }
                if (table.rowCount() + pendingRows > table.maxRecords()) {
                    throw new IOException("The table holds at most " + table.maxRecords() + " records");
                }
                for (RecordBatch batch : pending) {
                    table.appendAll(batch);
                }
                pending.clear();
                done = true;
            }

            @Override
            public void close() {
                pending.clear();
                done = true;
            }
        };
    }
}
