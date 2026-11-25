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
package dev.mars.jtable.host;

import dev.mars.jtable.definitions.NamespaceId;
import dev.mars.jtable.definitions.OrganisationId;
import dev.mars.jtable.definitions.Result;
import dev.mars.jtable.definitions.WorkspaceId;

import java.util.Objects;

/**
 * Who is calling and where, passed explicitly to every host service call (TD "Host configuration
 * API"). There is no ambient or thread-local context: a call without a namespace cannot read or
 * write namespace resources.
 *
 * @param organisation the organisation
 * @param namespace    the namespace, or null for organisation-level calls
 * @param workspace    the design workspace, for design actions; otherwise null
 * @param principal    the caller, as the host identified it; Unify never authenticates it
 */
public record HostContext(OrganisationId organisation, NamespaceId namespace, WorkspaceId workspace, Principal principal) {

    public HostContext {
        Objects.requireNonNull(organisation, "organisation");
        Objects.requireNonNull(principal, "principal");
    }

    /**
     * The namespace, or an {@link Result.Invalid} for a call that needs one but was made without
     * it.
     */
    public Result<NamespaceId> requireNamespace() {
        return namespace == null
                ? Result.invalid("namespace-required", "context.namespace", "This call needs a namespace")
                : Result.ok(namespace);
    }

    /** The design workspace, or an {@link Result.Invalid} for a design call made without one. */
    public Result<WorkspaceId> requireWorkspace() {
        return workspace == null
                ? Result.invalid("workspace-required", "context.workspace", "This call needs a design workspace")
                : Result.ok(workspace);
    }

    /**
     * The caller: an identifier recorded on saved versions, drafts and run records, and a display
     * name for the UI. Both are opaque to Unify.
     */
    public record Principal(String id, String displayName) {
        public Principal {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("A principal needs an identifier");
            }
            displayName = displayName == null || displayName.isBlank() ? id : displayName;
        }
    }
}
