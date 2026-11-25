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
import dev.mars.jtable.pipeline.selection.EngineSelection.SourceReport;
import dev.mars.jtable.pipeline.selection.EngineSelector.Request;
import dev.mars.jtable.pipeline.selection.SelectionInputs.BudgetClass;
import dev.mars.jtable.pipeline.selection.SelectionInputs.EngineState;
import dev.mars.jtable.pipeline.selection.SelectionInputs.Mode;
import dev.mars.jtable.pipeline.selection.SelectionProfile.Budgets;
import dev.mars.jtable.pipeline.selection.SelectionProfile.UseCase;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.mars.jtable.pipeline.selection.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

/** AC59 (generated from the profile) and AC60 (modes, extensions, installed engines). */
class EngineSelectorTest {

    private static final Map<String, String> BOTH = Map.of("sqlite", SQLITE_VERSION, "duckdb", DUCKDB_VERSION);
    private static final SelectionProfile PROFILE = profile();

    private static SourceReport counted(String alias, long records) {
        SourceSize size = SourceSize.exactCount(records);
        return new SourceReport(alias, size, new SizeEstimate(records, records * 32, true, false));
    }

    private static Request automatic(ShapeClass shape, long records, Budgets budgets, Set<String> warm) {
        return new Request(null, null, true, shape, List.of(counted("src", records)), budgets, warm);
    }

    private static Budgets budgetsFor(BudgetClass budget) {
        return budget == BudgetClass.DEFAULT ? DEFAULT_BUDGETS : CONSTRAINED_BUDGETS;
    }

    private static Set<String> warmFor(EngineState state) {
        return state == EngineState.WARM ? Set.of("sqlite", "duckdb") : Set.of("sqlite");
    }

    /**
     * AC59: one case per profile use case at each end of its record range, so both sides of every
     * boundary are exercised; the selection and everything it records are checked.
     */
    @TestFactory
    List<DynamicTest> everyUseCaseIsSelectedOnBothSidesOfItsBoundaries() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, null);
        List<DynamicTest> tests = new ArrayList<>();
        for (UseCase useCase : PROFILE.useCases()) {
            ShapeClass shape = useCase.shapes().iterator().next();
            EngineState state = useCase.states().iterator().next();
            BudgetClass budget = useCase.budgets().iterator().next();
            long last = useCase.records().to() == Long.MAX_VALUE ? useCase.records().from() + 1_000_000 : useCase.records().to() - 1;
            for (long records : new long[]{useCase.records().from(), last}) {
                tests.add(DynamicTest.dynamicTest(useCase.id() + " @ " + records, () -> {
                    Request request = automatic(shape, records, budgetsFor(budget), warmFor(state));
                    EngineSelection selection = selector.select(request);
                    assertEquals(useCase.engine(), selection.engine());
                    assertEquals(Mode.AUTOMATIC, selection.mode());
                    assertEquals(PROFILE.id(), selection.profileId());
                    assertEquals(PROFILE.version(), selection.profileVersion());
                    assertEquals(useCase.id(), selection.useCase());
                    assertEquals(useCase.records(), selection.recordRange());
                    assertEquals(state, selection.engineState());
                    assertEquals(budget, selection.budgetClass());
                    assertEquals(request.sources(), selection.sources());
                    assertEquals("exact-count", selection.sources().get(0).size().method().label());
                    assertEquals(budgetsFor(budget), selection.budgets());
                    assertEquals(BOTH, selection.installed());
                    assertEquals(BOTH.get(useCase.engine()), selection.engineVersion());
                }));
            }
        }
        return tests;
    }

    @Test
    void sameInputsAlwaysGiveTheSameEngine() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, null);
        Request request = automatic(ShapeClass.JOIN, 10_001, DEFAULT_BUDGETS, Set.of());
        assertEquals(selector.select(request), selector.select(request));
    }

    // ---- AC60 ----

    @Test
    void anExplicitChoiceWinsOverAutomaticSelection() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, "sqlite");
        Request request = new Request("duckdb", null, true, ShapeClass.SCAN, List.of(counted("s", 10)), DEFAULT_BUDGETS, Set.of());
        EngineSelection selection = selector.select(request);
        assertEquals("duckdb", selection.engine());
        assertEquals(Mode.EXPLICIT, selection.mode());
        assertNull(selection.profileId());
    }

    @Test
    void anExtensionPinsItsEngineAndConflictsWithAnotherExplicitEngine() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, null);
        Request pinned = new Request(null, "duckdb", true, ShapeClass.SCAN, List.of(counted("s", 10)), DEFAULT_BUDGETS, Set.of());
        EngineSelection selection = selector.select(pinned);
        assertEquals("duckdb", selection.engine());
        assertEquals(Mode.EXTENSION, selection.mode());

        Request conflict = new Request("sqlite", "duckdb", false, ShapeClass.SCAN, List.of(counted("s", 10)), DEFAULT_BUDGETS, Set.of());
        assertThrows(SelectionException.class, () -> selector.select(conflict));
    }

    @Test
    void onlyInstalledEnginesAreChosen() {
        EngineSelector sqliteOnly = new EngineSelector(PROFILE, Map.of("sqlite", SQLITE_VERSION), null);
        // The profile would pick DuckDB at a million records; with only SQLite installed and within
        // its budget, SQLite runs it.
        EngineSelection selection = sqliteOnly.select(automatic(ShapeClass.SCAN, 1_000_000, DEFAULT_BUDGETS, Set.of()));
        assertEquals("sqlite", selection.engine());
        assertEquals(EngineSelection.ONLY_INSTALLED, selection.useCase());
        // Beyond SQLite's budget it fails before any source is read; there is no substitution.
        assertThrows(SelectionException.class,
                () -> sqliteOnly.select(automatic(ShapeClass.SCAN, 10_000_000, DEFAULT_BUDGETS, Set.of())));
        // An explicit engine that is not installed fails.
        Request duck = new Request("duckdb", null, false, ShapeClass.SCAN, List.of(counted("s", 1)), DEFAULT_BUDGETS, Set.of());
        assertThrows(SelectionException.class, () -> sqliteOnly.select(duck));
        assertThrows(SelectionException.class,
                () -> new EngineSelector(PROFILE, Map.of(), null).select(automatic(ShapeClass.SCAN, 1, DEFAULT_BUDGETS, Set.of())));
        assertThrows(IllegalArgumentException.class, () -> new EngineSelector(PROFILE, Map.of("sqlite", SQLITE_VERSION), "duckdb"));
    }

    @Test
    void aHostSuppliedProfileIsRecorded() {
        SelectionProfile host = ProfileDerivation.derive("host-profile", "7.2.0", "host-run-42", List.of(OS), versions(), 0.10,
                DEFAULT_BUDGETS, widths(), allGroups());
        EngineSelection selection = new EngineSelector(host, BOTH, null)
                .select(automatic(ShapeClass.SORT, 50, DEFAULT_BUDGETS, Set.of()));
        assertEquals("host-profile", selection.profileId());
        assertEquals("7.2.0", selection.profileVersion());
    }

    @Test
    void anUntestedInstalledVersionOrAMissingProfileFails() {
        EngineSelector newer = new EngineSelector(PROFILE, Map.of("sqlite", "3.50.0", "duckdb", DUCKDB_VERSION), null);
        SelectionException e = assertThrows(SelectionException.class,
                () -> newer.select(automatic(ShapeClass.SCAN, 1, DEFAULT_BUDGETS, Set.of())));
        assertTrue(e.getMessage().contains("outside the versions"), e.getMessage());
        assertThrows(SelectionException.class,
                () -> new EngineSelector(null, BOTH, null).select(automatic(ShapeClass.SCAN, 1, DEFAULT_BUDGETS, Set.of())));
        assertThrows(SelectionException.class, () -> new EngineSelector(null, BOTH, null)
                .select(new Request(null, null, false, ShapeClass.SCAN, List.of(), DEFAULT_BUDGETS, Set.of())));
    }

    @Test
    void theConfiguredDefaultAppliesWhenNothingElseIsRequested() {
        EngineSelection selection = new EngineSelector(null, BOTH, "sqlite")
                .select(new Request(null, null, false, ShapeClass.SCAN, List.of(counted("s", 1)), DEFAULT_BUDGETS, Set.of()));
        assertEquals("sqlite", selection.engine());
        assertEquals(Mode.CONFIGURED_DEFAULT, selection.mode());
    }

    @Test
    void anOperationLargerThanSqlitesBudgetGoesToDuckDbWithAFileWhenNeeded() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, null);
        // 10 million records x 32 bytes = 320 MB: above SQLite's 256 MiB, below DuckDB's 1 GiB.
        EngineSelection selection = selector.select(automatic(ShapeClass.SCAN, 10_000_000, DEFAULT_BUDGETS, Set.of()));
        assertEquals("duckdb", selection.engine());
        assertEquals(EngineSelection.EXCEEDS_SQLITE_BUDGET, selection.useCase());
        assertFalse(selection.databaseFile());
        EngineSelection huge = selector.select(automatic(ShapeClass.SCAN, 100_000_000, DEFAULT_BUDGETS, Set.of()));
        assertTrue(huge.databaseFile());
    }

    @Test
    void anUnsupportedUseCaseFailsBeforeReading() {
        List<TestedUseCase> tested = new ArrayList<>(allGroups());
        tested.replaceAll(t -> t.shape() == ShapeClass.MULTI_JOIN && t.records() == 1_000_000
                ? new TestedUseCase(t.id(), t.description(), t.shape(), t.records(), t.width(), t.sources(), t.state(),
                t.budget(), t.os(), Map.of("sqlite", run(SQLITE_VERSION, 1, 1, true), "duckdb", run(DUCKDB_VERSION, 1, 1, true)))
                : t);
        SelectionProfile profile = profile(tested);
        EngineSelector selector = new EngineSelector(profile, BOTH, null);
        SelectionException e = assertThrows(SelectionException.class,
                () -> selector.select(automatic(ShapeClass.MULTI_JOIN, 2_000_000, DEFAULT_BUDGETS, Set.of())));
        assertTrue(e.getMessage().contains("unsupported at this budget"), e.getMessage());
    }

    // ---- Source change after the size was reported ----

    @Test
    void aSourceThatDeliversMoreThanItReportedFailsWithoutSwitchingEngines() {
        EngineSelector selector = new EngineSelector(PROFILE, BOTH, null);
        SourceReport file = new SourceReport("file", SourceSize.fileBytes(1000), SizeEstimate.fromBytes(SourceSize.fileBytes(1000), 10));
        SourceReport table = counted("table", 50);
        SourceReport bounded = new SourceReport("jdbc", SourceSize.boundedCount(PROFILE.probeLimit(), PROFILE.probeLimit()),
                new SizeEstimate(PROFILE.probeLimit(), PROFILE.probeLimit() * 32, true, false));
        EngineSelection selection = selector.select(new Request(null, null, true, ShapeClass.SCAN,
                List.of(file, table, bounded), DEFAULT_BUDGETS, Set.of()));

        selection.checkDelivered("table", 50);
        assertThrows(SourceChangeException.class, () -> selection.checkDelivered("table", 51));
        selection.checkDelivered("file", 100);
        assertThrows(SourceChangeException.class, () -> selection.checkDelivered("file", 101));
        selection.checkDelivered("jdbc", 5_000_000);  // still inside the unbounded large-size use case
        assertThrows(IllegalArgumentException.class, () -> selection.checkDelivered("unknown", 1));

        EngineSelection small = selector.select(new Request(null, null, true, ShapeClass.SCAN,
                List.of(new SourceReport("jdbc", SourceSize.boundedCount(10, 10), new SizeEstimate(10, 320, true, false))),
                DEFAULT_BUDGETS, Set.of()));
        // A bounded count may grow within the matched use case's range [0, 10001), but not beyond it.
        small.checkDelivered("jdbc", 10_000);
        assertThrows(SourceChangeException.class, () -> small.checkDelivered("jdbc", 10_001));
    }
}
