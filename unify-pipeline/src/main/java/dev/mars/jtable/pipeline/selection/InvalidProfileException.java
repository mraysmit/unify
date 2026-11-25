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

import java.util.List;

/** A selection profile failed validation; it is rejected before any operation uses it. */
public class InvalidProfileException extends IllegalArgumentException {
    private final String profile;
    private final List<String> problems;

    public InvalidProfileException(String profile, List<String> problems) {
        super("Selection profile " + profile + " is invalid: " + String.join("; ", problems));
        this.profile = profile;
        this.problems = List.copyOf(problems);
    }

    public String profile() {
        return profile;
    }

    /** Every problem found, so a profile author can fix them all at once. */
    public List<String> problems() {
        return problems;
    }
}
