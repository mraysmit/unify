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
import dev.mars.jtable.pipeline.selection.SelectionProfile.RecordRange;
import dev.mars.jtable.pipeline.selection.SelectionProfile.UseCase;
import dev.mars.jtable.pipeline.selection.SelectionProfile.VersionRange;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Derives a selection profile mechanically from tested use cases (TD "Selection profile"), so a
 * shipped profile is reproducible from its test run and nothing in it is chosen by hand.
 *
 * <p>Tested use cases are grouped by shape class, engine state and budget class. Within a group,
 * each tested size is decided by the decision rule; every tested use case of the same size must
 * decide the same engine, otherwise the profile cannot express the difference and derivation
 * fails. Consecutive sizes with the same engine form one use case. A boundary lies just after
 * the largest tested size at which the smaller-size engine is still selected, so untested sizes
 * between two tested sizes go to the engine selected at the larger size. The first use case of a
 * group starts at 0 records and the last has no upper end.</p>
 */
public final class ProfileDerivation {

    private ProfileDerivation() {
    }

    /**
     * Derives and validates a profile.
     *
     * @throws InvalidProfileException when the results leave a group untested or contradict
     *                                 each other at one size
     */
    public static SelectionProfile derive(String id, String version, String derivedFrom, List<String> operatingSystems,
                                          Map<String, VersionRange> engineVersions, double tieMargin,
                                          Budgets defaultBudgets, Map<LogicalType, Long> averageWidths,
                                          List<TestedUseCase> tested) {
        List<UseCase> useCases = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (ShapeClass shape : ShapeClass.values()) {
            for (EngineState state : EngineState.values()) {
                for (BudgetClass budget : BudgetClass.values()) {
                    deriveGroup(shape, state, budget, tested, tieMargin, useCases, problems);
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new InvalidProfileException(id, problems);
        }
        return SelectionProfile.of(id, version, derivedFrom, operatingSystems, engineVersions, tieMargin,
                defaultBudgets, averageWidths, useCases, tested);
    }

    private static void deriveGroup(ShapeClass shape, EngineState state, BudgetClass budget, List<TestedUseCase> tested,
                                    double tieMargin, List<UseCase> useCases, List<String> problems) {
        String group = shape + "/" + state + "/" + budget;
        TreeMap<Long, List<TestedUseCase>> bySize = new TreeMap<>();
        for (TestedUseCase t : tested) {
            if (t.shape() == shape && t.state() == state && t.budget() == budget) {
                bySize.computeIfAbsent(t.records(), k -> new ArrayList<>()).add(t);
            }
        }
        if (bySize.isEmpty()) {
            problems.add(group + ": no tested use cases");
            return;
        }
        List<Long> sizes = new ArrayList<>();
        List<String> engines = new ArrayList<>();
        List<List<String>> evidence = new ArrayList<>();
        for (Map.Entry<Long, List<TestedUseCase>> entry : bySize.entrySet()) {
            String engine = null;
            boolean first = true;
            List<String> ids = new ArrayList<>();
            for (TestedUseCase t : entry.getValue()) {
                Optional<String> decided = t.decide(tieMargin);
                if (first) {
                    engine = decided.orElse(null);
                    first = false;
                } else if (!Objects.equals(engine, decided.orElse(null))) {
                    problems.add(group + " at " + entry.getKey() + " records: tested use cases decide different engines ("
                            + ids.get(0) + " and " + t.id() + "); the profile needs a further condition");
                }
                ids.add(t.id());
            }
            sizes.add(entry.getKey());
            engines.add(engine);
            evidence.add(ids);
        }
        int start = 0;
        long from = 0;
        for (int i = 1; i <= sizes.size(); i++) {
            boolean end = i == sizes.size() || !Objects.equals(engines.get(i), engines.get(start));
            if (!end) {
                continue;
            }
            long to = i == sizes.size() ? Long.MAX_VALUE : sizes.get(i - 1) + 1;
            List<String> cited = new ArrayList<>();
            for (int k = start; k < i; k++) {
                cited.addAll(evidence.get(k));
            }
            String engine = engines.get(start);
            String id = (shape + "-" + state + "-" + budget + "-" + from).toLowerCase();
            useCases.add(new UseCase(id, group + " from " + from + " records: " + (engine == null ? "unsupported" : engine),
                    EnumSet.of(shape), EnumSet.of(state), EnumSet.of(budget), new RecordRange(from, to), engine, cited));
            from = to;
            start = i;
        }
    }
}
