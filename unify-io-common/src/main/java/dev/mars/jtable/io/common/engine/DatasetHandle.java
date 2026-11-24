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

import java.util.Objects;

/**
 * A reference to a dataset held inside an execution engine. The records stay in the engine; the
 * handle carries only the name, the Unify schema (which the engine's catalogue cannot always
 * reproduce) and the number of records loaded.
 */
public record DatasetHandle(String engine, String name, TableSchema schema, long rowCount) {

    public DatasetHandle {
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(schema, "schema");
        if (rowCount < 0) {
            throw new IllegalArgumentException("rowCount cannot be negative");
        }
    }
}
