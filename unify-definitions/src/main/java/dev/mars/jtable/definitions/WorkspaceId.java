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
 * The stable identifier of a design workspace. Renames change names only, so identifiers, and every
 * reference made with them, never change (FR037).
 */
public record WorkspaceId(UUID value) {
    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value");
    }

    /** A new random identifier. */
    public static WorkspaceId random() {
        return new WorkspaceId(UUID.randomUUID());
    }

    /** Parses the canonical UUID text form. */
    public static WorkspaceId parse(String text) {
        return new WorkspaceId(UUID.fromString(text));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}