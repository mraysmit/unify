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
import java.util.UUID;

/**
 * What a draft is the working copy of (TD "Drafts"): {@code pipeline:<definition id>} for an
 * existing pipeline, or {@code new:<uuid>} for one not yet saved.
 */
public record DraftSubject(DefinitionId pipeline, UUID newId) {
    public DraftSubject {
        if ((pipeline == null) == (newId == null)) {
            throw new IllegalArgumentException("A draft subject is either an existing pipeline or a new one");
        }
    }

    public static DraftSubject of(DefinitionId pipeline) {
        return new DraftSubject(Objects.requireNonNull(pipeline, "pipeline"), null);
    }

    public static DraftSubject newPipeline() {
        return new DraftSubject(null, UUID.randomUUID());
    }

    public boolean isNew() {
        return newId != null;
    }

    @Override
    public String toString() {
        return isNew() ? "new:" + newId : "pipeline:" + pipeline;
    }

    /** Parses {@link #toString()}. */
    public static DraftSubject parse(String text) {
        if (text != null && text.startsWith("pipeline:")) {
            return of(DefinitionId.parse(text.substring("pipeline:".length())));
        }
        if (text != null && text.startsWith("new:")) {
            return new DraftSubject(null, UUID.fromString(text.substring("new:".length())));
        }
        throw new IllegalArgumentException("Not a draft subject: " + text);
    }
}
