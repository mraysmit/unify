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
import dev.mars.jtable.pipeline.selection.EngineSelection.SourceReport;
import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;
import dev.mars.jtable.pipeline.selection.SelectionInputs.Mode;
import dev.mars.jtable.pipeline.selection.SelectionProfile.Budgets;
import dev.mars.jtable.pipeline.selection.SelectionProfile.UseCase;
import dev.mars.jtable.pipeline.selection.SelectionProfile.VersionRange;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Chooses the execution engine for an operation (ADR-003, TD "Engine selection"). It runs after
 * the operation is validated and every source has reported its size, and before any source record
 * is read. Given the same inputs and profile it always chooses the same engine: it keeps no
 * history of earlier runs.
 *
 * <p>Order: an explicit engine wins; otherwise an engine-specific extension pins its engine;
 * otherwise automatic selection uses the profile when requested; otherwise the host's configured
 * default applies. Only installed engines are ever chosen, and there is no silent substitution:
 * when the requested engine cannot run the operation, selection fails.</p>
 *
 * <p>Immutable and safe to share between operations.</p>
 */
public final class EngineSelector {
    private final SelectionProfile profile;
    private final Map<String, String> installed;
    private final String configuredDefault;

    /**
     * @param profile           the selection profile for automatic selection, or null when the
     *                          host offers none
     * @param installed         the installed engines and their versions, as discovered
     * @param configuredDefault the host's default engine, or null
     */
    public EngineSelector(SelectionProfile profile, Map<String, String> installed, String configuredDefault) {
        this.profile = profile;
        this.installed = Map.copyOf(new TreeMap<>(installed));
        this.configuredDefault = configuredDefault;
        for (String engine : this.installed.keySet()) {
            if (!SelectionProfile.ENGINES.contains(engine)) {
                throw new IllegalArgumentException("Unknown engine " + engine);
            }
        }
        if (configuredDefault != null && !this.installed.containsKey(configuredDefault)) {
            throw new IllegalArgumentException("The configured default engine " + configuredDefault + " is not installed");
        }
    }

    /**
     * What the operation asks for and the inputs selection uses.
     *
     * @param explicitEngine  the engine the operation names, or null
     * @param extensionEngine the engine an engine-specific extension in the pipeline needs, or null
     * @param automatic       whether automatic selection is requested
     * @param shape           the compiled pipeline's shape class
     * @param sources         every source's reported size and estimate
     * @param budgets         the operation's engine budgets
     * @param warmEngines     the engines already initialised in this process
     */
    public record Request(String explicitEngine, String extensionEngine, boolean automatic, ShapeClass shape,
                          List<SourceReport> sources, Budgets budgets, Set<String> warmEngines) {
        public Request {
            Objects.requireNonNull(shape, "shape");
            Objects.requireNonNull(budgets, "budgets");
            sources = List.copyOf(sources);
            warmEngines = Set.copyOf(warmEngines);
        }
    }

    /**
     * Selects the engine.
     *
     * @throws SelectionException when no installed engine may run the operation as requested
     */
    public EngineSelection select(Request request) {
        SizeEstimate total = SizeEstimate.ZERO;
        for (SourceReport source : request.sources()) {
            total = total.plus(source.estimate());
        }
        if (request.explicitEngine() != null) {
            String engine = requireInstalled(request.explicitEngine(), "The requested engine");
            if (request.extensionEngine() != null && !request.extensionEngine().equals(engine)) {
                throw new SelectionException("The pipeline uses an extension of " + request.extensionEngine()
                        + ", so it cannot run on the requested engine " + engine);
            }
            return fixed(Mode.EXPLICIT, engine, request, total);
        }
        if (request.extensionEngine() != null) {
            String engine = requireInstalled(request.extensionEngine(), "The engine the pipeline's extension needs");
            return fixed(Mode.EXTENSION, engine, request, total);
        }
        if (request.automatic()) {
            return automatic(request, total);
        }
        if (configuredDefault != null) {
            return fixed(Mode.CONFIGURED_DEFAULT, configuredDefault, request, total);
        }
        throw new SelectionException("No engine was chosen: name one, configure a default, or request automatic selection");
    }

    private EngineSelection automatic(Request request, SizeEstimate total) {
        if (profile == null) {
            throw new SelectionException("Automatic selection needs a selection profile, and none is configured");
        }
        if (installed.isEmpty()) {
            throw new SelectionException("No execution engine is installed");
        }
        for (Map.Entry<String, String> engine : installed.entrySet()) {
            VersionRange tested = profile.engineVersions().get(engine.getKey());
            if (tested == null || !tested.contains(engine.getValue())) {
                throw new SelectionException("Installed " + engine.getKey() + " " + engine.getValue()
                        + " is outside the versions profile " + profile.id() + " " + profile.version() + " was tested with ("
                        + tested + ")");
            }
        }
        Budgets budgets = request.budgets();
        EngineState state = request.warmEngines().containsAll(installed.keySet()) ? EngineState.WARM : EngineState.COLD;
        Budgets defaults = profile.defaultBudgets();
        BudgetClass budgetClass = budgets.sqliteMemoryBytes() >= defaults.sqliteMemoryBytes()
                && budgets.duckdbMemoryBytes() >= defaults.duckdbMemoryBytes() ? BudgetClass.DEFAULT : BudgetClass.CONSTRAINED;

        String engine;
        String useCase;
        SelectionProfile.RecordRange range = null;
        if (installed.size() == 1) {
            engine = installed.keySet().iterator().next();
            useCase = EngineSelection.ONLY_INSTALLED;
            if (engine.equals(SelectionInputs.SQLITE) && total.bytes() > budgets.sqliteMemoryBytes()) {
                throw new SelectionException("Only SQLite is installed, and the operation's estimated " + total.bytes()
                        + " bytes exceed its memory budget of " + budgets.sqliteMemoryBytes() + " bytes");
            }
        } else if (total.bytes() > budgets.sqliteMemoryBytes()) {
            engine = SelectionInputs.DUCKDB;
            useCase = EngineSelection.EXCEEDS_SQLITE_BUDGET;
        } else {
            UseCase matched = profile.match(request.shape(), state, budgetClass, total.records());
            if (matched.engine() == null) {
                throw new SelectionException("Profile " + profile.id() + " " + profile.version() + " use case " + matched.id()
                        + " (" + request.shape() + ", " + state + ", " + budgetClass + ", " + total.records()
                        + " records) is unsupported at this budget: neither engine stayed within it in testing");
            }
            engine = matched.engine();
            useCase = matched.id();
            range = matched.records();
        }
        return new EngineSelection(Mode.AUTOMATIC, engine, installed.get(engine), needsFile(engine, total, budgets),
                profile.id(), profile.version(), useCase, range, request.shape(), request.sources(), total, budgets,
                budgetClass, state, installed);
    }

    private EngineSelection fixed(Mode mode, String engine, Request request, SizeEstimate total) {
        return new EngineSelection(mode, engine, installed.get(engine), needsFile(engine, total, request.budgets()),
                null, null, null, null, request.shape(), request.sources(), total, request.budgets(), null, null, installed);
    }

    private static boolean needsFile(String engine, SizeEstimate total, Budgets budgets) {
        long memory = engine.equals(SelectionInputs.SQLITE) ? budgets.sqliteMemoryBytes() : budgets.duckdbMemoryBytes();
        return total.bytes() > memory;
    }

    private String requireInstalled(String engine, String what) {
        if (!installed.containsKey(engine)) {
            throw new SelectionException(what + " " + engine + " is not installed (installed: " + installed.keySet() + ")");
        }
        return engine;
    }
}
