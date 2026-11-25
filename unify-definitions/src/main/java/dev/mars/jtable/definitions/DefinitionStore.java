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

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Versioned definitions in one store (TD "Definition store"). Implemented by
 * {@code unify-store-postgres}; store tests run against real PostgreSQL, never a substitute.
 *
 * <p>Every call is one transaction and every lookup is scoped to a namespace, with no fallback to
 * another namespace or organisation (FR037). Expected outcomes are {@link Result} values:
 * a name already used in the namespace and kind, or a revision from a stale base version, is a
 * {@link Result.Conflict} carrying the current latest version, and nothing is written. Stored
 * versions are immutable.</p>
 */
public interface DefinitionStore {

    /** Creates a definition and its version 1. */
    Result<StoredVersion> create(NamespaceId namespace, String name, Envelope envelope, List<VersionRef> dependencies,
                                 String note, String principal);

    /**
     * Creates the next version, provided {@code baseVersion} is still the latest. A revision whose
     * content hash equals the latest version's returns that version unchanged. Every dependency
     * must exist in the same namespace.
     */
    Result<StoredVersion> revise(NamespaceId namespace, DefinitionId definition, int baseVersion, Envelope envelope,
                                 List<VersionRef> dependencies, String note, String principal);

    /** Loads one exact version, including retired definitions' versions. */
    Result<StoredVersion> load(NamespaceId namespace, VersionRef version);

    /** Finds a definition by its unique name within the namespace and kind. */
    Result<Optional<DefinitionSummary>> find(NamespaceId namespace, DefinitionKind kind, String name);

    /** Retires a definition: hidden from listings and the catalogue, still resolvable by exact version. */
    Result<DefinitionSummary> retire(NamespaceId namespace, DefinitionId definition, long revision);

    /**
     * One stored, immutable version.
     *
     * @param ref          the exact version
     * @param namespace    the owning namespace
     * @param name         the definition name at the time of loading
     * @param envelope     the stored envelope, upgraded in memory to the current envelope version
     * @param sha256       the canonical content hash
     * @param dependencies every exact reference the version makes
     */
    record StoredVersion(VersionRef ref, NamespaceId namespace, String name, Envelope envelope, String sha256,
                         List<VersionRef> dependencies, String note, Instant createdAt, String createdBy) {
        public StoredVersion {
            Objects.requireNonNull(ref, "ref");
            Objects.requireNonNull(envelope, "envelope");
            dependencies = List.copyOf(dependencies);
        }
    }

    /** The current state of one definition. {@code retiredAt} is null while it is active. */
    record DefinitionSummary(DefinitionId id, NamespaceId namespace, DefinitionKind kind, String name, int latestVersion,
                             long revision, Instant retiredAt) {
        public boolean retired() {
            return retiredAt != null;
        }
    }
}
