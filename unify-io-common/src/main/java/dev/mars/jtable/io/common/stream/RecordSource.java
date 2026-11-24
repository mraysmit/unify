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
import dev.mars.jtable.core.schema.TableSchema;

import java.io.IOException;

/**
 * A stream of checked records, delivered in bounded batches. This is how every reader hands data
 * on (to an execution engine's loader, or to a sink) without holding the whole dataset in Java
 * memory (ADR-001).
 *
 * <p>A source is single-use and single-threaded. The schema is known before the first batch.
 * Every batch carries that schema and contains only values that already satisfy it. The caller
 * must close the source, including after a failure; closing releases files, cursors and
 * connections owned by the source.</p>
 */
public interface RecordSource extends AutoCloseable {

    /** The schema every batch from this source conforms to. */
    TableSchema schema();

    /**
     * The next non-empty batch, or {@code null} when the source is exhausted. A record that
     * cannot be read or converted fails the call with a located error.
     */
    RecordBatch nextBatch() throws IOException;

    @Override
    void close() throws IOException;
}
