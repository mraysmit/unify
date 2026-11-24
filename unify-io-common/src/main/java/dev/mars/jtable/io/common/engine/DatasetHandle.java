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

import java.util.List;
import java.util.Objects;

/**
 * A reference to a dataset held inside an execution engine. The records stay in the engine; the
 * handle carries the logical name, the Unify schema (which the engine's catalogue cannot always
 * reproduce), the number of records loaded, and the generated physical names the engine uses.
 *
 * <p>Engine tables and columns use generated names ({@code t3}, {@code c1} …) because SQLite and
 * DuckDB match identifiers case-insensitively, while Unify column names are case-sensitive
 * (FR001): {@code "Name"} and {@code "name"} are distinct columns. Logical names appear only in
 * results and diagnostics.</p>
 *
 * @param table   the engine table holding the dataset
 * @param columns the engine column for each schema column, in schema order
 */
public record DatasetHandle(String engine, String name, TableSchema schema, long rowCount,
                            String table, List<String> columns) {

    public DatasetHandle {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(table, "table");
        columns = List.copyOf(columns);
        if (rowCount < 0) {
            throw new IllegalArgumentException("rowCount cannot be negative");
        }
        if (columns.size() != schema.size()) {
            throw new IllegalArgumentException("Every schema column needs exactly one engine column");
        }
    }

    /** The engine column that holds the named logical column. */
    public String column(String logicalName) {
        int position = schema.indexOf(logicalName);
        if (position < 0) {
            throw new IllegalArgumentException("Dataset '" + name + "' has no column '" + logicalName + "'");
        }
        return columns.get(position);
    }
}
