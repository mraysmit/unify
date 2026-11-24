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
package dev.mars.jtable.core.schema;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, ordered list of column definitions with a name-to-position lookup.
 *
 * <p>A schema travels with every record batch and with every dataset held by an execution engine,
 * because an engine's own catalogue cannot always reproduce Unify logical types (SQLite, for
 * example, stores booleans as integers and temporal values as text).</p>
 */
public final class TableSchema {
    private final List<ColumnDefinition> columns;
    private final Map<String, Integer> positions;

    private TableSchema(List<ColumnDefinition> columns) {
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("A schema needs at least one column");
        }
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = Objects.requireNonNull(columns.get(i), "Column cannot be null");
            if (index.putIfAbsent(column.name(), i) != null) {
                throw new IllegalArgumentException("Duplicate column name: " + column.name());
            }
        }
        this.columns = List.copyOf(columns);
        this.positions = Map.copyOf(index);
    }

    public static TableSchema of(ColumnDefinition... columns) {
        return new TableSchema(Arrays.asList(columns));
    }

    public static TableSchema of(List<ColumnDefinition> columns) {
        return new TableSchema(columns);
    }

    /** The columns in order. The list is unmodifiable. */
    public List<ColumnDefinition> columns() {
        return columns;
    }

    public int size() {
        return columns.size();
    }

    public ColumnDefinition column(int position) {
        return columns.get(position);
    }

    /** The position of the named column, or -1 when the schema has no such column. */
    public int indexOf(String name) {
        Integer position = positions.get(name);
        return position == null ? -1 : position;
    }

    /** The named column; fails when the schema has no such column. */
    public ColumnDefinition column(String name) {
        int position = indexOf(name);
        if (position < 0) {
            throw new IllegalArgumentException("Unknown column: " + name);
        }
        return columns.get(position);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TableSchema that && columns.equals(that.columns);
    }

    @Override
    public int hashCode() {
        return columns.hashCode();
    }

    @Override
    public String toString() {
        return "TableSchema" + columns;
    }
}
