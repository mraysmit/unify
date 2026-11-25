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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;
import dev.mars.jtable.pipeline.selection.TestedUseCase.EngineRun;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The selection profile: the versioned rules automatic engine selection applies, derived from
 * use-case testing (ADR-003, TD "Selection profile"). Unify ships one; a host may supply its own,
 * derived the same way on its hardware and workloads.
 *
 * <p>Profile format version 1. The profile covers operations for which both engines are installed
 * and the estimated working size fits SQLite's memory budget. Its use cases partition every
 * combination of shape class, engine state and budget class into record ranges. Two rules always
 * apply outside the profile and are built into {@link EngineSelector}: with one engine installed
 * that engine is used if the operation fits its budget, and an operation larger than SQLite's
 * memory budget goes to DuckDB.</p>
 *
 * <p>Loading validates the profile, and an invalid profile is never returned: every combination
 * must match exactly one use case; every use case must cite tested use cases that lie inside it,
 * carry every metric for both engines, and decide the use case's engine under the profile's
 * decision rule.</p>
 */
public final class SelectionProfile {

    /** The only format this version of Unify reads. */
    public static final int FORMAT = 1;

    /** The decision rule this format uses (TD "Use-case testing"). */
    public static final String DECISION_RULE = "eligible-then-p95-then-native-memory";

    static final List<String> ENGINES = List.of(SelectionInputs.SQLITE, SelectionInputs.DUCKDB);

    private static final ObjectMapper JSON = new ObjectMapper();

    private final String id;
    private final String version;
    private final String derivedFrom;
    private final List<String> operatingSystems;
    private final Map<String, VersionRange> engineVersions;
    private final double tieMargin;
    private final Budgets defaultBudgets;
    private final Map<LogicalType, Long> averageWidths;
    private final List<UseCase> useCases;
    private final Map<String, TestedUseCase> tested;

    private SelectionProfile(String id, String version, String derivedFrom, List<String> operatingSystems,
                             Map<String, VersionRange> engineVersions, double tieMargin, Budgets defaultBudgets,
                             Map<LogicalType, Long> averageWidths, List<UseCase> useCases,
                             Map<String, TestedUseCase> tested) {
        this.id = id;
        this.version = version;
        this.derivedFrom = derivedFrom;
        this.operatingSystems = List.copyOf(operatingSystems);
        this.engineVersions = Map.copyOf(engineVersions);
        this.tieMargin = tieMargin;
        this.defaultBudgets = defaultBudgets;
        this.averageWidths = Collections.unmodifiableMap(new EnumMap<>(averageWidths));
        this.useCases = List.copyOf(useCases);
        this.tested = Collections.unmodifiableMap(new LinkedHashMap<>(tested));
    }

    /**
     * Builds and validates a profile.
     *
     * @throws InvalidProfileException listing every problem when the profile is not valid
     */
    public static SelectionProfile of(String id, String version, String derivedFrom, List<String> operatingSystems,
                                      Map<String, VersionRange> engineVersions, double tieMargin, Budgets defaultBudgets,
                                      Map<LogicalType, Long> averageWidths, List<UseCase> useCases,
                                      List<TestedUseCase> testedUseCases) {
        Map<String, TestedUseCase> tested = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        for (TestedUseCase t : testedUseCases) {
            if (tested.put(t.id(), t) != null) {
                problems.add("duplicate tested use case " + t.id());
            }
        }
        SelectionProfile profile = new SelectionProfile(id, version, derivedFrom, operatingSystems, engineVersions,
                tieMargin, defaultBudgets, averageWidths, useCases, tested);
        problems.addAll(profile.validate());
        if (!problems.isEmpty()) {
            throw new InvalidProfileException(id, problems);
        }
        return profile;
    }

    public String id() { return id; }
    public String version() { return version; }
    public String derivedFrom() { return derivedFrom; }
    public List<String> operatingSystems() { return operatingSystems; }
    public Map<String, VersionRange> engineVersions() { return engineVersions; }
    public double tieMargin() { return tieMargin; }
    public Budgets defaultBudgets() { return defaultBudgets; }
    public Map<LogicalType, Long> averageWidths() { return averageWidths; }
    public List<UseCase> useCases() { return useCases; }
    public Map<String, TestedUseCase> testedUseCases() { return tested; }

    /**
     * The record count a bounded JDBC probe should stop at: one more than the largest finite
     * record boundary, so a count at the limit still decides the use case.
     */
    public long probeLimit() {
        long largest = 0;
        for (UseCase useCase : useCases) {
            if (useCase.records().to() != Long.MAX_VALUE) {
                largest = Math.max(largest, useCase.records().to());
            }
        }
        return largest + 1;
    }

    /** The one use case that covers these inputs; the profile is valid, so there always is one. */
    public UseCase match(ShapeClass shape, EngineState state, BudgetClass budget, long records) {
        for (UseCase useCase : useCases) {
            if (useCase.covers(shape, state, budget) && useCase.records().contains(records)) {
                return useCase;
            }
        }
        throw new IllegalStateException("A validated profile covers every input");
    }

    /** The use case that a tested use case falls into. */
    public UseCase match(TestedUseCase t) {
        return match(t.shape(), t.state(), t.budget(), t.records());
    }

    private List<String> validate() {
        List<String> problems = new ArrayList<>();
        if (id == null || id.isBlank()) problems.add("the profile has no identifier");
        if (version == null || version.isBlank()) problems.add("the profile has no version");
        if (derivedFrom == null || derivedFrom.isBlank()) problems.add("the profile does not name the test run it was derived from");
        if (operatingSystems.isEmpty()) problems.add("the profile names no tested operating system");
        if (!(tieMargin >= 0 && tieMargin < 1)) problems.add("the tie margin must be at least 0 and below 1");
        for (String engine : ENGINES) {
            if (!engineVersions.containsKey(engine)) problems.add("no tested version range for " + engine);
        }
        for (LogicalType type : LogicalType.values()) {
            Long width = averageWidths.get(type);
            if (width == null || width < 1) problems.add("no measured average width for " + type);
        }
        Set<String> ids = new HashSet<>();
        for (UseCase useCase : useCases) {
            if (!ids.add(useCase.id())) problems.add("duplicate use case " + useCase.id());
            validateEvidence(useCase, problems);
        }
        validateCoverage(problems);
        return problems;
    }

    private void validateEvidence(UseCase useCase, List<String> problems) {
        if (useCase.evidence().isEmpty()) {
            problems.add("use case " + useCase.id() + " cites no tested use cases");
        }
        for (String cited : useCase.evidence()) {
            TestedUseCase t = tested.get(cited);
            if (t == null) {
                problems.add("use case " + useCase.id() + " cites unknown tested use case " + cited);
                continue;
            }
            if (!useCase.covers(t.shape(), t.state(), t.budget()) || !useCase.records().contains(t.records())) {
                problems.add("use case " + useCase.id() + " cites " + cited + ", which lies outside it");
            }
            if (!operatingSystems.contains(t.os())) {
                problems.add("tested use case " + cited + " ran on " + t.os() + ", which the profile does not list");
            }
            problems.addAll(t.problems(ENGINES));
            for (String engine : ENGINES) {
                EngineRun run = t.results().get(engine);
                VersionRange range = engineVersions.get(engine);
                if (run != null && range != null && run.engineVersion() != null && !run.engineVersion().isBlank()
                        && !range.contains(run.engineVersion())) {
                    problems.add("tested use case " + cited + " measured " + engine + " " + run.engineVersion()
                            + ", outside the profile's range " + range);
                }
            }
            Optional<String> decided = t.decide(tieMargin);
            if (!Objects.equals(decided.orElse(null), useCase.engine())) {
                problems.add("use case " + useCase.id() + " selects " + label(useCase.engine()) + " but its evidence "
                        + cited + " decides " + label(decided.orElse(null)));
            }
        }
    }

    private void validateCoverage(List<String> problems) {
        for (ShapeClass shape : ShapeClass.values()) {
            for (EngineState state : EngineState.values()) {
                for (BudgetClass budget : BudgetClass.values()) {
                    List<RecordRange> ranges = new ArrayList<>();
                    List<String> owners = new ArrayList<>();
                    for (UseCase useCase : useCases) {
                        if (useCase.covers(shape, state, budget)) {
                            ranges.add(useCase.records());
                            owners.add(useCase.id());
                        }
                    }
                    String where = shape + "/" + state + "/" + budget;
                    checkTiling(where, ranges, owners, problems);
                }
            }
        }
    }

    private static void checkTiling(String where, List<RecordRange> ranges, List<String> owners, List<String> problems) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ranges.size(); i++) order.add(i);
        order.sort((a, b) -> Long.compare(ranges.get(a).from(), ranges.get(b).from()));
        long next = 0;
        for (int i : order) {
            RecordRange range = ranges.get(i);
            if (range.from() > next) {
                problems.add(where + ": no use case covers records " + next + " to " + (range.from() - 1));
            } else if (range.from() < next) {
                problems.add(where + ": use case " + owners.get(i) + " overlaps another below record " + next);
            }
            next = Math.max(next, range.to());
        }
        if (next != Long.MAX_VALUE) {
            problems.add(where + ": no use case covers records from " + next);
        }
    }

    private static String label(String engine) {
        return engine == null ? "no engine (unsupported)" : engine;
    }

    // ---- JSON ----

    /**
     * Reads and validates a profile document.
     *
     * @throws InvalidProfileException when the document is not a valid format-1 profile
     */
    public static SelectionProfile parse(String json) {
        JsonNode root;
        try {
            root = JSON.readTree(json);
        } catch (IOException e) {
            throw new InvalidProfileException("(unreadable)", List.of("not valid JSON: " + e.getMessage()));
        }
        String id = text(root, "profile");
        try {
            if (root.path("format").asInt(-1) != FORMAT) {
                throw new IllegalArgumentException("unsupported profile format " + root.path("format") + "; expected " + FORMAT);
            }
            if (!DECISION_RULE.equals(text(root, "decisionRule"))) {
                throw new IllegalArgumentException("unsupported decision rule " + text(root, "decisionRule"));
            }
            List<String> os = new ArrayList<>();
            root.path("operatingSystems").forEach(n -> os.add(n.asText()));
            Map<String, VersionRange> versions = new HashMap<>();
            root.path("engineVersions").fields().forEachRemaining(e ->
                    versions.put(e.getKey(), new VersionRange(text(e.getValue(), "min"), text(e.getValue(), "max"))));
            JsonNode budgets = root.path("defaultBudgets");
            Budgets defaults = new Budgets(budgets.path("sqliteMemoryBytes").asLong(), budgets.path("duckdbMemoryBytes").asLong(),
                    budgets.path("tempDiskBytes").asLong());
            Map<LogicalType, Long> widths = new EnumMap<>(LogicalType.class);
            root.path("averageWidths").fields().forEachRemaining(e ->
                    widths.put(LogicalType.valueOf(e.getKey()), e.getValue().asLong()));
            List<UseCase> useCases = new ArrayList<>();
            for (JsonNode n : root.path("useCases")) {
                useCases.add(new UseCase(text(n, "id"), text(n, "description"),
                        enums(n.path("shapes"), ShapeClass.class), enums(n.path("engineStates"), EngineState.class),
                        enums(n.path("budgets"), BudgetClass.class),
                        new RecordRange(n.path("records").path("from").asLong(),
                                n.path("records").path("to").isMissingNode() || n.path("records").path("to").isNull()
                                        ? Long.MAX_VALUE : n.path("records").path("to").asLong()),
                        n.path("engine").isNull() || n.path("engine").isMissingNode() ? null : n.path("engine").asText(),
                        strings(n.path("evidence"))));
            }
            List<TestedUseCase> tested = new ArrayList<>();
            for (JsonNode n : root.path("testedUseCases")) {
                Map<String, EngineRun> results = new HashMap<>();
                n.path("results").fields().forEachRemaining(e -> {
                    JsonNode r = e.getValue();
                    results.put(e.getKey(), new EngineRun(text(r, "engineVersion"), r.path("startupMillis").asDouble(-1),
                            r.path("p50Millis").asDouble(-1), r.path("p95Millis").asDouble(-1), r.path("runs").asInt(0),
                            r.path("heapRetainedBytes").asLong(-1), r.path("nativeBytes").asLong(-1),
                            r.path("tempDiskBytes").asLong(-1), r.path("budgetFailure").asBoolean(false),
                            text(r, "dataShape")));
                });
                tested.add(new TestedUseCase(text(n, "id"), text(n, "description"),
                        ShapeClass.valueOf(text(n, "shape")), n.path("records").asLong(-1), text(n, "width"),
                        text(n, "sources"), EngineState.valueOf(text(n, "state")), BudgetClass.valueOf(text(n, "budget")),
                        text(n, "os"), results));
            }
            return of(id, text(root, "version"), text(root, "derivedFrom"), os, versions,
                    root.path("tieMargin").asDouble(-1), defaults, widths, useCases, tested);
        } catch (IllegalArgumentException | NullPointerException e) {
            if (e instanceof InvalidProfileException invalid) {
                throw invalid;
            }
            throw new InvalidProfileException(id == null ? "(unnamed)" : id, List.of(String.valueOf(e.getMessage())));
        }
    }

    /** The profile as a format-1 JSON document; {@link #parse} reads it back unchanged. */
    public String toJson() {
        ObjectNode root = JSON.createObjectNode();
        root.put("format", FORMAT);
        root.put("profile", id);
        root.put("version", version);
        root.put("derivedFrom", derivedFrom);
        root.put("decisionRule", DECISION_RULE);
        root.put("tieMargin", tieMargin);
        ArrayNode os = root.putArray("operatingSystems");
        operatingSystems.forEach(os::add);
        ObjectNode versions = root.putObject("engineVersions");
        engineVersions.forEach((engine, range) -> versions.putObject(engine).put("min", range.min()).put("max", range.max()));
        root.putObject("defaultBudgets").put("sqliteMemoryBytes", defaultBudgets.sqliteMemoryBytes())
                .put("duckdbMemoryBytes", defaultBudgets.duckdbMemoryBytes()).put("tempDiskBytes", defaultBudgets.tempDiskBytes());
        ObjectNode widths = root.putObject("averageWidths");
        averageWidths.forEach((type, width) -> widths.put(type.name(), width));
        ArrayNode cases = root.putArray("useCases");
        for (UseCase u : useCases) {
            ObjectNode n = cases.addObject();
            n.put("id", u.id());
            n.put("description", u.description());
            ArrayNode shapes = n.putArray("shapes");
            u.shapes().forEach(s -> shapes.add(s.name()));
            ArrayNode states = n.putArray("engineStates");
            u.states().forEach(s -> states.add(s.name()));
            ArrayNode budgets = n.putArray("budgets");
            u.budgets().forEach(b -> budgets.add(b.name()));
            ObjectNode range = n.putObject("records").put("from", u.records().from());
            if (u.records().to() == Long.MAX_VALUE) range.putNull("to"); else range.put("to", u.records().to());
            if (u.engine() == null) n.putNull("engine"); else n.put("engine", u.engine());
            ArrayNode evidence = n.putArray("evidence");
            u.evidence().forEach(evidence::add);
        }
        ArrayNode testedNodes = root.putArray("testedUseCases");
        for (TestedUseCase t : tested.values()) {
            ObjectNode n = testedNodes.addObject();
            n.put("id", t.id()).put("description", t.description()).put("shape", t.shape().name())
                    .put("records", t.records()).put("width", t.width()).put("sources", t.sources())
                    .put("state", t.state().name()).put("budget", t.budget().name()).put("os", t.os());
            ObjectNode results = n.putObject("results");
            t.results().forEach((engine, r) -> results.putObject(engine).put("engineVersion", r.engineVersion())
                    .put("startupMillis", r.startupMillis()).put("p50Millis", r.p50Millis()).put("p95Millis", r.p95Millis())
                    .put("runs", r.runs()).put("heapRetainedBytes", r.heapRetainedBytes()).put("nativeBytes", r.nativeBytes())
                    .put("tempDiskBytes", r.tempDiskBytes()).put("budgetFailure", r.budgetFailure()).put("dataShape", r.dataShape()));
        }
        try {
            return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asText()));
        return values;
    }

    private static <E extends Enum<E>> Set<E> enums(JsonNode array, Class<E> type) {
        Set<E> values = EnumSet.noneOf(type);
        array.forEach(n -> values.add(Enum.valueOf(type, n.asText())));
        return values;
    }

    // ---- Parts ----

    /**
     * A half-open record range {@code [from, to)}; {@code to} is {@link Long#MAX_VALUE} for no
     * upper end.
     */
    public record RecordRange(long from, long to) {
        public RecordRange {
            if (from < 0 || to <= from) {
                throw new IllegalArgumentException("A record range needs 0 <= from < to");
            }
        }

        public boolean contains(long records) {
            return records >= from && records < to;
        }

        @Override
        public String toString() {
            return "[" + from + ", " + (to == Long.MAX_VALUE ? "unbounded" : to) + ")";
        }
    }

    /** An inclusive range of tested engine versions, compared by numeric parts. */
    public record VersionRange(String min, String max) {
        public VersionRange {
            if (Versions.compare(min, max) > 0) {
                throw new IllegalArgumentException("Version range " + min + " to " + max + " is empty");
            }
        }

        public boolean contains(String version) {
            return Versions.compare(version, min) >= 0 && Versions.compare(version, max) <= 0;
        }

        @Override
        public String toString() {
            return min + " to " + max;
        }
    }

    /**
     * Engine budgets: memory per engine and temporary disk.
     */
    public record Budgets(long sqliteMemoryBytes, long duckdbMemoryBytes, long tempDiskBytes) {
        public Budgets {
            if (sqliteMemoryBytes < 0 || duckdbMemoryBytes < 0 || tempDiskBytes < 0) {
                throw new IllegalArgumentException("Budgets cannot be negative");
            }
        }
    }

    /**
     * One profile use case: the inputs it covers and the engine it selects, with its evidence.
     * Empty sets of shapes, states or budgets mean "all". A null engine means the inputs are
     * unsupported at that budget: operations matching it fail before reading any source.
     */
    public record UseCase(String id, String description, Set<ShapeClass> shapes, Set<EngineState> states,
                          Set<BudgetClass> budgets, RecordRange records, String engine, List<String> evidence) {
        public UseCase {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("A use case needs an id");
            }
            Objects.requireNonNull(records, "records");
            shapes = shapes.isEmpty() ? EnumSet.allOf(ShapeClass.class) : EnumSet.copyOf(shapes);
            states = states.isEmpty() ? EnumSet.allOf(EngineState.class) : EnumSet.copyOf(states);
            budgets = budgets.isEmpty() ? EnumSet.allOf(BudgetClass.class) : EnumSet.copyOf(budgets);
            shapes = Collections.unmodifiableSet(shapes);
            states = Collections.unmodifiableSet(states);
            budgets = Collections.unmodifiableSet(budgets);
            evidence = List.copyOf(evidence);
            if (engine != null && !ENGINES.contains(engine)) {
                throw new IllegalArgumentException("Use case " + id + " selects unknown engine " + engine);
            }
        }

        boolean covers(ShapeClass shape, EngineState state, BudgetClass budget) {
            return shapes.contains(shape) && states.contains(state) && budgets.contains(budget);
        }
    }
}
