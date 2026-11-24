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

import dev.mars.jtable.core.schema.RecordBatch;

import java.io.IOException;

/** Helpers for moving records between sources and sinks one batch at a time. */
public final class RecordStreams {

    private RecordStreams() {
    }

    /**
     * Copies every batch from {@code source} to {@code sink} and completes the sink. Neither is
     * closed; the caller owns both. Returns the number of records copied.
     */
    public static long copy(RecordSource source, RecordSink sink) throws IOException {
        long rows = 0;
        for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
            if (!batch.schema().equals(source.schema())) {
                throw new IOException("A batch does not match its source's schema");
            }
            sink.write(batch);
            rows += batch.rowCount();
        }
        sink.complete();
        return rows;
    }
}
