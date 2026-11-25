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

import dev.mars.jtable.definitions.CatalogueQuery.Capability;
import dev.mars.jtable.definitions.DefinitionId;
import dev.mars.jtable.definitions.NamespaceId;
import dev.mars.jtable.definitions.OrganisationId;
import dev.mars.jtable.definitions.Result;
import dev.mars.jtable.host.HostContext.Principal;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The host's authorisation decisions (TD "Access policy", FR036). Every service method asks
 * {@link #decide} before doing anything, and binding resolution and execution ask again for each
 * provider, view and connection used, so withdrawn access takes effect at once. A denial changes
 * nothing.
 *
 * <p>There is no implicit default. Development and tests name {@link #allowAll()} explicitly; the
 * Unify service supplies a policy built from role grants; an embedding application supplies its
 * own.</p>
 */
public interface AccessPolicy {

    /** Whether the principal may perform the action on the resource. */
    Decision decide(Principal principal, Action action, ResourceRef resource);

    /**
     * What the principal may discover in a namespace's catalogue. The catalogue applies it inside
     * its query, so pages and counts include only visible entries.
     */
    CatalogueVisibility visibility(Principal principal, NamespaceRef namespace);

    /** The actions a policy decides (TD "Access policy"). */
    enum Action {
        DISCOVER, INSPECT, BIND, SAVE, PUBLISH, EXECUTE, USE_CONNECTION, TEST_CONNECTION, VIEW_HISTORY,
        ADMINISTER_STRUCTURE
    }

    /** An allow or a deny with its reason. */
    record Decision(boolean allowed, String reason) {
        public static final Decision ALLOW = new Decision(true, "allowed");

        public static Decision deny(String reason) {
            return new Decision(false, Objects.requireNonNull(reason, "reason"));
        }
    }

    /** A namespace within its organisation. */
    record NamespaceRef(OrganisationId organisation, NamespaceId namespace) {
        public NamespaceRef {
            Objects.requireNonNull(organisation, "organisation");
            Objects.requireNonNull(namespace, "namespace");
        }
    }

    /**
     * What a decision is about: a structure element, or a definition in a namespace.
     *
     * @param type         what kind of resource
     * @param organisation its organisation
     * @param namespace    its namespace, for namespace resources
     * @param id           its identifier or name, for display and audit
     */
    record ResourceRef(Type type, OrganisationId organisation, NamespaceId namespace, String id) {
        public enum Type { ORGANISATION, NAMESPACE, WORKSPACE, DEFINITION, CONNECTION, RUN }

        public ResourceRef {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(organisation, "organisation");
            if (type != Type.ORGANISATION && type != Type.WORKSPACE && namespace == null) {
                throw new IllegalArgumentException("A " + type + " resource belongs to a namespace");
            }
        }

        public static ResourceRef definition(OrganisationId organisation, NamespaceId namespace, DefinitionId id) {
            return new ResourceRef(Type.DEFINITION, organisation, namespace, id.toString());
        }

        @Override
        public String toString() {
            return type.name().toLowerCase() + ":" + organisation + (namespace == null ? "" : "/" + namespace)
                    + (id == null ? "" : "/" + id);
        }
    }

    /**
     * Which catalogue entries a principal may discover. A null set means "any"; an empty set
     * means "none".
     */
    record CatalogueVisibility(Set<String> categories, Set<Capability> capabilities, Set<DefinitionId> entries) {
        public static final CatalogueVisibility ALL = new CatalogueVisibility(null, null, null);
        public static final CatalogueVisibility NONE = new CatalogueVisibility(Set.of(), Set.of(), Set.of());

        public CatalogueVisibility {
            categories = categories == null ? null : Set.copyOf(categories);
            capabilities = capabilities == null ? null : Set.copyOf(capabilities);
            entries = entries == null ? null : Set.copyOf(entries);
        }

        public boolean permits(String category, Capability capability, DefinitionId entry) {
            return (categories == null || categories.contains(category))
                    && (capabilities == null || capabilities.contains(capability))
                    && (entries == null || entries.contains(entry));
        }
    }

    /**
     * A policy that allows everything and shows the whole catalogue. Only for development and
     * tests, where it must be named explicitly.
     */
    static AccessPolicy allowAll() {
        return new AccessPolicy() {
            @Override
            public Decision decide(Principal principal, Action action, ResourceRef resource) {
                return Decision.ALLOW;
            }

            @Override
            public CatalogueVisibility visibility(Principal principal, NamespaceRef namespace) {
                return CatalogueVisibility.ALL;
            }

            @Override
            public String toString() {
                return "AccessPolicy.allowAll()";
            }
        };
    }

    /**
     * Runs {@code work} only when the policy allows the action; otherwise returns
     * {@link Result.Denied} without calling it, so a denial changes nothing. Services use this as
     * their first step.
     */
    default <T> Result<T> guard(HostContext context, Action action, ResourceRef resource, Supplier<Result<T>> work) {
        Decision decision = decide(context.principal(), action, resource);
        if (decision == null || !decision.allowed()) {
            return Result.denied(action.name(), resource.toString());
        }
        return work.get();
    }
}
