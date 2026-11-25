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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A catalogue search (TD "Provider catalogue"): free text matched case-insensitively as a
 * substring of the name and description, filters, a page size, and one keyset cursor per
 * category. Search covers every category, whatever the UI shows collapsed.
 *
 * @param text           free text, or null for none; blank is treated as none
 * @param capabilities   capability filter; empty means all
 * @param connectors     connector-kind filter; empty means all
 * @param categories     category filter; empty means all
 * @param pageSize       entries per category page, 1 to {@link #MAX_PAGE_SIZE}
 * @param cursors        the cursor to continue after, per category; absent means the first page
 * @param includeRetired whether retired entries are included
 */
public record CatalogueQuery(String text, Set<Capability> capabilities, Set<String> connectors, Set<String> categories,
                             int pageSize, Map<String, Cursor> cursors, boolean includeRetired) {

    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int MAX_PAGE_SIZE = 200;

    /** What a catalogue entry offers. */
    public enum Capability { READ, WRITE, VIEW }

    public CatalogueQuery {
        text = text == null || text.isBlank() ? null : text.strip();
        capabilities = capabilities.isEmpty() ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(capabilities);
        capabilities = Set.copyOf(capabilities);
        connectors = Set.copyOf(connectors);
        categories = Set.copyOf(categories);
        cursors = Map.copyOf(cursors);
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("The page size must be 1 to " + MAX_PAGE_SIZE);
        }
    }

    /** Every entry, first page of each category, default page size. */
    public static CatalogueQuery all() {
        return new CatalogueQuery(null, Set.of(), Set.of(), Set.of(), DEFAULT_PAGE_SIZE, Map.of(), false);
    }

    /** Entries whose name or description contains {@code text}, ignoring case. */
    public static CatalogueQuery text(String text) {
        return new CatalogueQuery(text, Set.of(), Set.of(), Set.of(), DEFAULT_PAGE_SIZE, Map.of(), false);
    }

    /**
     * A keyset position: the (name, id) of the last entry on the previous page. Pages ordered by
     * name then identifier stay stable while entries are added. The {@link #encode() encoded}
     * form is opaque to clients.
     */
    public record Cursor(String name, DefinitionId id) {
        public Cursor {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(id, "id");
        }

        public String encode() {
            String raw = id + "\n" + name;
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        /** Decodes {@link #encode()}; a tampered or foreign cursor is rejected. */
        public static Cursor decode(String token) {
            try {
                String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
                int split = raw.indexOf('\n');
                if (split < 0) {
                    throw new IllegalArgumentException("Not a catalogue cursor");
                }
                return new Cursor(raw.substring(split + 1), new DefinitionId(UUID.fromString(raw.substring(0, split))));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Not a catalogue cursor", e);
            }
        }
    }

    /**
     * A category-qualified catalogue name such as {@code Files.Holdings}: the category is the
     * text before the first dot, the entry name the rest. Both are nonblank.
     */
    public record QualifiedName(String category, String name) {
        public QualifiedName {
            if (category == null || category.isBlank() || name == null || name.isBlank()) {
                throw new IllegalArgumentException("A catalogue name needs a category and a name");
            }
            if (category.contains(".")) {
                throw new IllegalArgumentException("A category cannot contain a dot");
            }
        }

        public static QualifiedName parse(String qualified) {
            int dot = qualified == null ? -1 : qualified.indexOf('.');
            if (dot < 1 || dot == qualified.length() - 1) {
                throw new IllegalArgumentException("A catalogue name has the form Category.Name");
            }
            return new QualifiedName(qualified.substring(0, dot), qualified.substring(dot + 1));
        }

        @Override
        public String toString() {
            return category + "." + name;
        }
    }
}
