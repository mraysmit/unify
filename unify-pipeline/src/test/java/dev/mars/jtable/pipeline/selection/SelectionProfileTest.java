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
import dev.mars.jtable.pipeline.selection.SelectionProfile.RecordRange;
import dev.mars.jtable.pipeline.selection.SelectionProfile.UseCase;
import dev.mars.jtable.pipeline.selection.TestedUseCase.EngineRun;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static dev.mars.jtable.pipeline.selection.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** Profile derivation, the decision rule, validation (AC60) and the release gate (AC62). */
class SelectionProfileTest {

    @Test
    void shapeClassesApplyInOrder() {
        assertEquals(ShapeClass.MULTI_JOIN, ShapeClass.classify(2, true, false, true));
        assertEquals(ShapeClass.JOIN_AGGREGATE, ShapeClass.classify(1, false, true, false));
        assertEquals(ShapeClass.JOIN, ShapeClass.classify(1, false, false, true));
        assertEquals(ShapeClass.AGGREGATE, ShapeClass.classify(0, true, false, true));
        assertEquals(ShapeClass.SORT, ShapeClass.classify(0, false, false, true));
        assertEquals(ShapeClass.SCAN, ShapeClass.classify(0, false, false, false));
    }

    @Test
    void theDecisionRuleUsesEligibilityThenP95ThenNativeMemory() {
        TestedUseCase faster = tested(ShapeClass.SCAN, 1000, EngineState.WARM, BudgetClass.DEFAULT, 5, 50);
        assertEquals("sqlite", faster.decide(0.10).orElseThrow());
        // Within the 10% tie margin, less native memory wins (SQLite 12 MiB against DuckDB 80 MiB).
        TestedUseCase tie = tested(ShapeClass.SCAN, 1000, EngineState.WARM, BudgetClass.DEFAULT, 100, 95);
        assertEquals("sqlite", tie.decide(0.10).orElseThrow());
        assertEquals("duckdb", tie.decide(0.01).orElseThrow());
        // A budget failure in any run makes an engine ineligible.
        TestedUseCase failed = new TestedUseCase("f", "d", ShapeClass.SCAN, 1000, "narrow", "file", EngineState.WARM,
                BudgetClass.DEFAULT, OS, Map.of("sqlite", run(SQLITE_VERSION, 1, 1, true), "duckdb", run(DUCKDB_VERSION, 999, 1, false)));
        assertEquals("duckdb", failed.decide(0.10).orElseThrow());
        TestedUseCase neither = new TestedUseCase("n", "d", ShapeClass.SCAN, 1000, "narrow", "file", EngineState.WARM,
                BudgetClass.DEFAULT, OS, Map.of("sqlite", run(SQLITE_VERSION, 1, 1, true), "duckdb", run(DUCKDB_VERSION, 1, 1, true)));
        assertTrue(neither.decide(0.10).isEmpty());
    }

    @Test
    void derivationPlacesEachBoundaryAfterTheLargestSizeTheSmallerEngineWins() {
        SelectionProfile profile = profile();
        assertEquals(ShapeClass.values().length * EngineState.values().length * BudgetClass.values().length * 2,
                profile.useCases().size());
        UseCase small = profile.match(ShapeClass.JOIN, EngineState.COLD, BudgetClass.DEFAULT, 0);
        assertEquals("sqlite", small.engine());
        assertEquals(new RecordRange(0, 10_001), small.records());
        assertEquals(2, small.evidence().size());
        UseCase large = profile.match(ShapeClass.JOIN, EngineState.COLD, BudgetClass.DEFAULT, 10_001);
        assertEquals("duckdb", large.engine());
        assertEquals(Long.MAX_VALUE, large.records().to());
        assertEquals(10_002, profile.probeLimit());
    }

    @Test
    void aProfileSurvivesItsJsonForm() {
        SelectionProfile profile = profile();
        SelectionProfile read = SelectionProfile.parse(profile.toJson());
        assertEquals(profile.id(), read.id());
        assertEquals(profile.version(), read.version());
        assertEquals(profile.useCases(), read.useCases());
        assertEquals(profile.testedUseCases(), read.testedUseCases());
        assertEquals(profile.defaultBudgets(), read.defaultBudgets());
        assertEquals(profile.toJson(), read.toJson());
    }

    @Test
    void derivationRefusesUntestedGroupsAndContradictoryResults() {
        List<TestedUseCase> missing = new ArrayList<>(allGroups());
        missing.removeIf(t -> t.shape() == ShapeClass.SORT && t.state() == EngineState.WARM);
        InvalidProfileException e = assertThrows(InvalidProfileException.class, () -> profile(missing));
        assertTrue(e.problems().stream().anyMatch(p -> p.startsWith("SORT/WARM")), e.getMessage());

        List<TestedUseCase> contradictory = new ArrayList<>(allGroups());
        TestedUseCase wide = new TestedUseCase("T-wide", "synthetic", ShapeClass.SCAN, 1000, "wide", "file",
                EngineState.WARM, BudgetClass.DEFAULT, OS, Map.of("sqlite", run(SQLITE_VERSION, 500, 1, false),
                "duckdb", run(DUCKDB_VERSION, 5, 1, false)));
        contradictory.add(wide);
        e = assertThrows(InvalidProfileException.class, () -> profile(contradictory));
        assertTrue(e.getMessage().contains("decide different engines"), e.getMessage());
    }

    // ---- AC60: overlapping, incomplete, unevidenced and out-of-range profiles are rejected ----

    private static SelectionProfile with(List<UseCase> useCases, List<TestedUseCase> tested) {
        return SelectionProfile.of("p", "1", "run", List.of(OS), versions(), 0.10, DEFAULT_BUDGETS, widths(), useCases, tested);
    }

    private static List<UseCase> replacing(SelectionProfile base, String id, UseCase replacement) {
        List<UseCase> useCases = new ArrayList<>(base.useCases());
        useCases.replaceAll(u -> u.id().equals(id) ? replacement : u);
        return useCases;
    }

    @Test
    void anOverlappingProfileIsRejected() {
        SelectionProfile base = profile();
        UseCase small = base.match(ShapeClass.SCAN, EngineState.WARM, BudgetClass.DEFAULT, 0);
        UseCase widened = new UseCase(small.id(), small.description(), small.shapes(), small.states(), small.budgets(),
                new RecordRange(0, 20_000), small.engine(), small.evidence());
        InvalidProfileException e = assertThrows(InvalidProfileException.class,
                () -> with(replacing(base, small.id(), widened), new ArrayList<>(base.testedUseCases().values())));
        assertTrue(e.getMessage().contains("overlaps"), e.getMessage());
    }

    @Test
    void anIncompleteProfileIsRejected() {
        SelectionProfile base = profile();
        UseCase small = base.match(ShapeClass.SCAN, EngineState.WARM, BudgetClass.DEFAULT, 0);
        List<UseCase> useCases = new ArrayList<>(base.useCases());
        useCases.remove(small);
        InvalidProfileException e = assertThrows(InvalidProfileException.class,
                () -> with(useCases, new ArrayList<>(base.testedUseCases().values())));
        assertTrue(e.getMessage().contains("SCAN/WARM/DEFAULT: no use case covers records 0 to 10000"), e.getMessage());
    }

    @Test
    void anUnevidencedOrContradictedUseCaseIsRejected() {
        SelectionProfile base = profile();
        List<TestedUseCase> tested = new ArrayList<>(base.testedUseCases().values());
        UseCase small = base.match(ShapeClass.SCAN, EngineState.WARM, BudgetClass.DEFAULT, 0);

        UseCase noEvidence = new UseCase(small.id(), "", small.shapes(), small.states(), small.budgets(), small.records(),
                small.engine(), List.of());
        assertTrue(assertThrows(InvalidProfileException.class, () -> with(replacing(base, small.id(), noEvidence), tested))
                .getMessage().contains("cites no tested use cases"));

        UseCase wrongEngine = new UseCase(small.id(), "", small.shapes(), small.states(), small.budgets(), small.records(),
                "duckdb", small.evidence());
        assertTrue(assertThrows(InvalidProfileException.class, () -> with(replacing(base, small.id(), wrongEngine), tested))
                .getMessage().contains("but its evidence"));

        UseCase outside = new UseCase(small.id(), "", EnumSet.of(ShapeClass.SCAN), small.states(), small.budgets(),
                small.records(), small.engine(), List.of(base.match(ShapeClass.SORT, EngineState.WARM, BudgetClass.DEFAULT, 0).evidence().get(0)));
        assertTrue(assertThrows(InvalidProfileException.class, () -> with(replacing(base, small.id(), outside), tested))
                .getMessage().contains("lies outside it"));
    }

    @Test
    void incompleteMetricsAndOutOfRangeVersionsAreRejected() {
        List<TestedUseCase> tested = new ArrayList<>(allGroups());
        TestedUseCase first = tested.get(0);
        EngineRun fewRuns = new EngineRun("1.5.6", 10, 40, 50, 2, 1, 80 * MIB, 0, false, "narrow:4");
        EngineRun oldVersion = new EngineRun("3.30.0", 10, 4, 5, 5, 1, 12 * MIB, 0, false, "narrow:4");
        tested.set(0, new TestedUseCase(first.id(), first.description(), first.shape(), first.records(), first.width(),
                first.sources(), first.state(), first.budget(), first.os(), Map.of("sqlite", oldVersion, "duckdb", fewRuns)));
        InvalidProfileException e = assertThrows(InvalidProfileException.class, () -> profile(tested));
        assertTrue(e.getMessage().contains("fewer than 5 timed runs"), e.getMessage());
        assertTrue(e.getMessage().contains("outside the profile's range"), e.getMessage());
    }

    @Test
    void malformedDocumentsAreRejected() {
        assertThrows(InvalidProfileException.class, () -> SelectionProfile.parse("{not json"));
        String json = profile().toJson();
        assertThrows(InvalidProfileException.class, () -> SelectionProfile.parse(json.replace("\"format\" : 1", "\"format\" : 2")));
        assertThrows(InvalidProfileException.class, () -> SelectionProfile.parse(
                json.replace(SelectionProfile.DECISION_RULE, "fastest-wins")));
    }

    // ---- AC62: the release gate ----

    @Test
    void anIdenticalRerunLetsTheReleaseShip() {
        SelectionProfile profile = profile();
        assertEquals(List.of(), ReleaseCheck.contradictions(profile, allGroups()));
    }

    @Test
    void aContradictingOrMissingRerunBlocksTheRelease() {
        SelectionProfile profile = profile();
        List<TestedUseCase> rerun = new ArrayList<>(allGroups());
        TestedUseCase flipped = tested(ShapeClass.AGGREGATE, 1000, EngineState.COLD, BudgetClass.DEFAULT, 500, 5);
        rerun.replaceAll(t -> t.id().equals(flipped.id()) ? flipped : t);
        rerun.removeIf(t -> t.shape() == ShapeClass.SORT && t.records() == 1_000_000 && t.state() == EngineState.WARM
                && t.budget() == BudgetClass.DEFAULT);
        List<String> problems = ReleaseCheck.contradictions(profile, rerun);
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains(flipped.id()) && p.contains("decides duckdb")));
        assertTrue(problems.stream().anyMatch(p -> p.contains("was not rerun")));
    }
}
