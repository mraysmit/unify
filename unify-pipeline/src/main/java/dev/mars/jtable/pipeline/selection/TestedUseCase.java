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

import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * One concrete, repeatable integration that was run on both engines, with every FR030 metric
 * (TD "Use-case testing"). Profiles cite these as evidence.
 *
 * @param id          a stable identifier
 * @param description what the integration does
 * @param shape       its pipeline shape class
 * @param records     its data size in records
 * @param width       its record width class (for example narrow, medium, wide)
 * @param sources     its source mix (for example file, file+jdbc)
 * @param state       the engine state it ran in
 * @param budget      the budget it ran under
 * @param os          the operating system it ran on
 * @param results     the measurements per engine name
 */
public record TestedUseCase(String id, String description, ShapeClass shape, long records, String width,
                            String sources, EngineState state, BudgetClass budget, String os,
                            Map<String, EngineRun> results) {

    /** The minimum number of timed runs behind p50 and p95. */
    public static final int MINIMUM_RUNS = 5;

    public TestedUseCase {
        requireText(id, "id");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(budget, "budget");
        requireText(os, "os");
        if (records < 0) {
            throw new IllegalArgumentException("Tested use case " + id + ": records cannot be negative");
        }
        results = Map.copyOf(results);
    }

    /**
     * The measurements of one engine in one tested use case.
     *
     * @param engineVersion     the engine version measured
     * @param startupMillis     time to open the engine in this use case's state
     * @param p50Millis         median execution time, open to published output
     * @param p95Millis         95th percentile execution time
     * @param runs              the number of timed runs (at least {@link #MINIMUM_RUNS})
     * @param heapRetainedBytes Java heap retained after GC
     * @param nativeBytes       native memory: process peak minus the bare-JVM baseline
     * @param tempDiskBytes     peak temporary disk
     * @param budgetFailure     whether any run exceeded a budget
     * @param dataShape         the data shape measured (schema and width summary)
     */
    public record EngineRun(String engineVersion, double startupMillis, double p50Millis, double p95Millis, int runs,
                            long heapRetainedBytes, long nativeBytes, long tempDiskBytes, boolean budgetFailure,
                            String dataShape) {
    }

    /** Every missing or impossible metric, so a profile can refuse incomplete evidence. */
    public List<String> problems(List<String> engines) {
        List<String> problems = new ArrayList<>();
        for (String engine : engines) {
            EngineRun run = results.get(engine);
            if (run == null) {
                problems.add("tested use case " + id + " has no results for " + engine);
                continue;
            }
            String where = "tested use case " + id + " on " + engine;
            if (run.engineVersion() == null || run.engineVersion().isBlank()) problems.add(where + ": no engine version");
            if (run.dataShape() == null || run.dataShape().isBlank()) problems.add(where + ": no data shape");
            if (run.runs() < MINIMUM_RUNS) problems.add(where + ": fewer than " + MINIMUM_RUNS + " timed runs");
            if (!(run.startupMillis() >= 0)) problems.add(where + ": no startup time");
            if (!(run.p50Millis() >= 0) || !(run.p95Millis() >= run.p50Millis())) problems.add(where + ": inconsistent p50/p95");
            if (run.heapRetainedBytes() < 0 || run.nativeBytes() < 0 || run.tempDiskBytes() < 0) {
                problems.add(where + ": negative memory or disk");
            }
        }
        return problems;
    }

    /**
     * The decision rule of TD "Use-case testing": only engines that stayed within every budget in
     * every run are eligible; the lower p95 wins; within the tie margin the lower native memory
     * wins; a single eligible engine wins outright. Empty when neither is eligible (unsupported at
     * this budget).
     */
    public Optional<String> decide(double tieMargin) {
        String best = null;
        for (Map.Entry<String, EngineRun> entry : results.entrySet()) {
            EngineRun run = entry.getValue();
            if (run.budgetFailure()) {
                continue;
            }
            if (best == null) {
                best = entry.getKey();
                continue;
            }
            EngineRun current = results.get(best);
            double slower = Math.max(run.p95Millis(), current.p95Millis());
            double faster = Math.min(run.p95Millis(), current.p95Millis());
            boolean tie = slower == 0 || (slower - faster) / slower <= tieMargin;
            if (tie) {
                if (run.nativeBytes() < current.nativeBytes()
                        || (run.nativeBytes() == current.nativeBytes() && entry.getKey().compareTo(best) < 0)) {
                    best = entry.getKey();
                }
            } else if (run.p95Millis() < current.p95Millis()) {
                best = entry.getKey();
            }
        }
        return Optional.ofNullable(best);
    }

    private static void requireText(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A tested use case needs a " + what);
        }
    }
}
