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
package dev.mars.jtable.pipeline.selection;

/** The small, finite selector input dimensions shared by profiles, tests and the selector. */
public final class SelectionInputs {

    private SelectionInputs() {
    }

    /** The SQLite engine name, as {@code ExecutionEngine.name()} reports it. */
    public static final String SQLITE = "sqlite";

    /** The DuckDB engine name, as {@code ExecutionEngine.name()} reports it. */
    public static final String DUCKDB = "duckdb";

    /**
     * Whether the engines would start cold. WARM means every installed engine is already
     * initialised in the host process; otherwise the operation is COLD, because the comparison
     * between engines then includes at least one first-use start (unpacking a native library).
     */
    public enum EngineState { COLD, WARM }

    /**
     * The operation's engine memory budget compared with the profile's default budgets: DEFAULT
     * when both engine budgets are at least the defaults, CONSTRAINED otherwise.
     */
    public enum BudgetClass { DEFAULT, CONSTRAINED }

    /** How the engine was chosen, recorded in the result (FR011). */
    public enum Mode {
        /** The operation named an engine. */
        EXPLICIT,
        /** The pipeline uses an engine-specific extension, which pins its engine. */
        EXTENSION,
        /** The host's configured default engine. */
        CONFIGURED_DEFAULT,
        /** The selector chose from the selection profile. */
        AUTOMATIC
    }
}
