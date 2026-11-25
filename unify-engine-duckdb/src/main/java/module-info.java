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

/** The DuckDB execution engine, for large and analytical work (ADR-002). Optional for a host. */
module dev.mars.jtable.engine.duckdb {
    requires transitive dev.mars.jtable.io.common;
    requires java.sql;
    // DuckDB's JDBC jar has no module descriptor; this is its derived automatic module name.
    requires duckdb.jdbc;

    exports dev.mars.jtable.engine.duckdb;
}
