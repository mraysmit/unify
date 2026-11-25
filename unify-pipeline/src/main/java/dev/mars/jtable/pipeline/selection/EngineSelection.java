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

import dev.mars.jtable.io.common.size.SizeEstimate;
import dev.mars.jtable.io.common.size.SourceSize;
import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;
import dev.mars.jtable.pipeline.selection.SelectionInputs.Mode;
import dev.mars.jtable.pipeline.selection.SelectionProfile.Budgets;
import dev.mars.jtable.pipeline.selection.SelectionProfile.RecordRange;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * How an operation's engine was chosen, recorded in its result (FR011): the mode, the engine and
 * its actual version, and for automatic selection the profile, the matched use case and every
 * input used. It is fixed before any source record is read.
 *
 * @param mode           how the engine was chosen
 * @param engine         the engine name
 * @param engineVersion  the installed engine's actual version
 * @param databaseFile   whether the engine must use a database file because the operation exceeds
 *                       its memory budget
 * @param profileId      the profile identifier, for automatic selection; otherwise null
 * @param profileVersion the profile version, for automatic selection; otherwise null
 * @param useCase        the matched profile use case or built-in rule, for automatic selection
 * @param recordRange    the matched use case's record range, which bounded probes must respect
 * @param shape          the pipeline shape class
 * @param sources        each source's reported size and normalised estimate
 * @param total          the operation's total estimate
 * @param budgets        the budgets applied
 * @param budgetClass    the budget class, for automatic selection
 * @param engineState    the engine state, for automatic selection
 * @param installed      the installed engines and their versions
 */
public record EngineSelection(Mode mode, String engine, String engineVersion, boolean databaseFile, String profileId,
                              String profileVersion, String useCase, RecordRange recordRange, ShapeClass shape,
                              List<SourceReport> sources, SizeEstimate total, Budgets budgets, BudgetClass budgetClass,
                              EngineState engineState, Map<String, String> installed) {

    /** The built-in rule used when only one engine is installed. */
    public static final String ONLY_INSTALLED = "builtin:only-installed-engine";

    /** The built-in rule used when the operation exceeds SQLite's memory budget. */
    public static final String EXCEEDS_SQLITE_BUDGET = "builtin:exceeds-sqlite-memory-budget";

    public EngineSelection {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(engine, "engine");
        sources = List.copyOf(sources);
        installed = Map.copyOf(installed);
    }

    /** One source's reported size and its normalised estimate. */
    public record SourceReport(String alias, SourceSize size, SizeEstimate estimate) {
        public SourceReport {
            Objects.requireNonNull(alias, "alias");
            Objects.requireNonNull(size, "size");
            Objects.requireNonNull(estimate, "estimate");
        }
    }

    /**
     * Checks what a source actually delivered against what it reported (TD "Size probes"): an
     * exact count must match exactly, a record estimate derived from bytes is an upper bound, and
     * a bounded count must keep the operation inside the matched use case's record range.
     * Otherwise the operation fails before publication as a source change; it never switches
     * engines.
     *
     * @throws SourceChangeException when the source delivered more, or other, than it reported
     */
    public void checkDelivered(String alias, long deliveredRecords) {
        SourceReport report = sources.stream().filter(s -> s.alias().equals(alias)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown source " + alias));
        SourceSize size = report.size();
        if (size.unit() == SourceSize.Unit.RECORDS && size.exact()) {
            if (deliveredRecords != size.value()) {
                throw new SourceChangeException(alias, "reported " + size.value() + " records (" + size.method().label()
                        + ") but delivered " + deliveredRecords);
            }
        } else if (size.unit() == SourceSize.Unit.BYTES) {
            if (deliveredRecords > report.estimate().records()) {
                throw new SourceChangeException(alias, "delivered " + deliveredRecords
                        + " records, more than the " + report.estimate().records() + " its " + size + " allows");
            }
        } else if (recordRange != null) {
            long others = total.records() - report.estimate().records();
            if (!recordRange.contains(others + deliveredRecords)) {
                throw new SourceChangeException(alias, "delivered " + deliveredRecords
                        + " records, which moves the operation outside the matched use case " + useCase + " " + recordRange);
            }
        }
    }
}
