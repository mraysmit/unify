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

import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;
import dev.mars.jtable.pipeline.selection.SelectionProfile.Budgets;
import dev.mars.jtable.pipeline.selection.SelectionProfile.VersionRange;
import dev.mars.jtable.pipeline.selection.TestedUseCase.EngineRun;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * SYNTHETIC test fixtures for the selection mechanics. These numbers are invented to exercise
 * derivation, validation and selection; they are not measurements and must never be shipped as
 * a selection profile. A shipped profile is derived only from the use-case test harness.
 */
final class Fixtures {
    static final String OS = "test-os";
    static final String SQLITE_VERSION = "3.42.0";
    static final String DUCKDB_VERSION = "1.5.6";
    static final long MIB = 1024L * 1024;
    static final Budgets DEFAULT_BUDGETS = new Budgets(256 * MIB, 1024 * MIB, 4096 * MIB);
    static final Budgets CONSTRAINED_BUDGETS = new Budgets(64 * MIB, 256 * MIB, 1024 * MIB);
    static final List<Long> SIZES = List.of(1_000L, 10_000L, 100_000L, 1_000_000L);

    private Fixtures() {
    }

    static Map<String, VersionRange> versions() {
        return Map.of(SelectionInputs.SQLITE, new VersionRange("3.42.0", "3.42.0"),
                SelectionInputs.DUCKDB, new VersionRange("1.5.0", "1.5.9"));
    }

    static Map<LogicalType, Long> widths() {
        Map<LogicalType, Long> widths = new EnumMap<>(LogicalType.class);
        for (LogicalType type : LogicalType.values()) {
            widths.put(type, 8L);
        }
        return widths;
    }

    static EngineRun run(String version, double p95, long nativeBytes, boolean budgetFailure) {
        return new EngineRun(version, 10, p95 * 0.8, p95, 5, 4 * MIB, nativeBytes, 0, budgetFailure, "narrow:4");
    }

    /** A tested use case where SQLite has the given p95 and DuckDB the other. */
    static TestedUseCase tested(ShapeClass shape, long records, EngineState state, BudgetClass budget,
                                double sqliteP95, double duckdbP95) {
        String id = ("T-" + shape + "-" + state + "-" + budget + "-" + records).toLowerCase();
        return new TestedUseCase(id, "synthetic", shape, records, "narrow", "file", state, budget, OS,
                Map.of(SelectionInputs.SQLITE, run(SQLITE_VERSION, sqliteP95, 12 * MIB, false),
                        SelectionInputs.DUCKDB, run(DUCKDB_VERSION, duckdbP95, 80 * MIB, false)));
    }

    /**
     * Results for every group: SQLite clearly faster up to 10,000 records, DuckDB clearly faster
     * from 100,000.
     */
    static List<TestedUseCase> allGroups() {
        List<TestedUseCase> tested = new ArrayList<>();
        for (ShapeClass shape : ShapeClass.values()) {
            for (EngineState state : EngineState.values()) {
                for (BudgetClass budget : BudgetClass.values()) {
                    for (long size : SIZES) {
                        boolean small = size <= 10_000;
                        tested.add(tested(shape, size, state, budget, small ? 5 : 500, small ? 50 : 100));
                    }
                }
            }
        }
        return tested;
    }

    static SelectionProfile profile(List<TestedUseCase> tested) {
        return ProfileDerivation.derive("test-profile", "1.0.0", "synthetic-run", List.of(OS), versions(), 0.10,
                DEFAULT_BUDGETS, widths(), tested);
    }

    static SelectionProfile profile() {
        return profile(allGroups());
    }
}
