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
package dev.mars.jtable.io.common.engine;

import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;

/**
 * An execution backend (SQLite or DuckDB) that holds an operation's working data (ADR-001).
 * Records enter through {@link #load} and leave as streams through {@link #query}; Java never
 * holds a whole dataset.
 *
 * <p>This is an internal contract between Unify's orchestration and its backends. The SQL passed
 * to {@link #query} is backend SQL; the public API will accept only portable SQL that Apache
 * Calcite has parsed and validated, then translated for the selected backend. An engine instance
 * is operation-scoped and single-threaded.</p>
 */
public interface ExecutionEngine extends AutoCloseable {

    /** The backend's name, for example {@code "duckdb"}. */
    String name();

    /** The backend's actual version, recorded in operation results. */
    String version();

    /**
     * Creates a dataset from the source's schema and loads every record into it as one unit: on
     * any failure no dataset of that name is left behind. The source is read to the end but not
     * closed; the caller owns it.
     *
     * @param dataset a new dataset name, unique within this engine
     */
    DatasetHandle load(String dataset, RecordSource source) throws IOException;

    /**
     * Runs a read-only query and streams its result in batches of at most {@code batchSize}
     * records. Until Calcite validation supplies result types, the caller declares the result
     * schema; the result's column count and labels must match it and every value must convert to
     * the declared type, otherwise reading fails with a located error. The returned source must
     * be closed; it owns the query's cursor.
     */
    RecordSource query(String sql, TableSchema resultSchema, int batchSize) throws IOException;

    /** Streams every record of a loaded dataset, using the schema recorded in its handle. */
    RecordSource scan(DatasetHandle dataset, int batchSize) throws IOException;

    /** Removes a dataset. Removing a dataset that does not exist does nothing. */
    void drop(String dataset) throws IOException;

    /** Releases the engine and any temporary storage it owns. */
    @Override
    void close() throws IOException;
}
