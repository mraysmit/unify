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
package dev.mars.jtable.definitions;

import java.util.Objects;

/**
 * An exact reference to one immutable version of a definition. References are always exact
 * (definition, version) pairs; nothing resolves "latest" implicitly once saved.
 */
public record VersionRef(DefinitionId definition, int version) {
    public VersionRef {
        Objects.requireNonNull(definition, "definition");
        if (version < 1) {
            throw new IllegalArgumentException("Versions start at 1");
        }
    }

    /** The text form {@code <definition id>@<version>}. */
    @Override
    public String toString() {
        return definition + "@" + version;
    }

    /** Parses {@link #toString()}. */
    public static VersionRef parse(String text) {
        int at = text.lastIndexOf('@');
        if (at < 1) {
            throw new IllegalArgumentException("Not a version reference: " + text);
        }
        return new VersionRef(DefinitionId.parse(text.substring(0, at)), Integer.parseInt(text.substring(at + 1)));
    }
}
