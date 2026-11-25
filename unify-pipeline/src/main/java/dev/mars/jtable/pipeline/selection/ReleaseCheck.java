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

import dev.mars.jtable.pipeline.selection.SelectionProfile.UseCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The release gate of AC62 and TD09: every release reruns the use-case tests and compares them
 * with the shipped profile. Any contradicted use case blocks the release until the profile is
 * regenerated from the new results.
 */
public final class ReleaseCheck {

    private ReleaseCheck() {
    }

    /**
     * The reasons the rerun contradicts the profile; empty when the release may ship. A rerun
     * contradicts the profile when a tested use case decides a different engine from the profile
     * use case it falls in, has incomplete metrics, or when a tested use case the profile cites
     * was not rerun.
     */
    public static List<String> contradictions(SelectionProfile profile, List<TestedUseCase> rerun) {
        List<String> problems = new ArrayList<>();
        Set<String> rerunIds = new HashSet<>();
        for (TestedUseCase t : rerun) {
            rerunIds.add(t.id());
            problems.addAll(t.problems(SelectionProfile.ENGINES));
            UseCase useCase = profile.match(t);
            Optional<String> decided = t.decide(profile.tieMargin());
            if (!Objects.equals(decided.orElse(null), useCase.engine())) {
                problems.add("rerun of " + t.id() + " decides " + label(decided.orElse(null)) + " but profile use case "
                        + useCase.id() + " selects " + label(useCase.engine()));
            }
        }
        for (UseCase useCase : profile.useCases()) {
            for (String cited : useCase.evidence()) {
                if (!rerunIds.contains(cited)) {
                    problems.add("tested use case " + cited + " cited by " + useCase.id() + " was not rerun");
                }
            }
        }
        return problems;
    }

    private static String label(String engine) {
        return engine == null ? "no engine" : engine;
    }
}
