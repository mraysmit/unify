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

/**
 * A destination that accepts checked records in batches, typically streamed out of an execution
 * engine's query result.
 *
 * <p>Nothing written is visible at the destination until {@link #complete()} succeeds. Closing a
 * sink that was not completed discards what was written, so a failure part-way leaves the
 * destination unchanged. A sink is single-use and single-threaded.</p>
 */
public interface RecordSink extends AutoCloseable {

    /** Writes one batch. Its schema must equal the schema the sink was opened for. */
    void write(RecordBatch batch) throws IOException;

    /** Publishes everything written. After this the sink accepts no more batches. */
    void complete() throws IOException;

    /** Releases resources; discards unpublished output when {@link #complete()} did not succeed. */
    @Override
    void close() throws IOException;
}
