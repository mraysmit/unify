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
package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.IColumn;
import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.profiling.ConcurrentCollectionProfiler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An extension of OptimizedTableCore that adds profiling support to identify bottlenecks
 * in concurrent collection usage. This class wraps collection operations with profiling
 * hooks to track metrics such as operation counts, latencies, and read-write ratios.
 * 
 * Usage example:
 * <pre>
 * // Create a profiled table with concurrent access
 * ProfiledOptimizedTableCore table = new ProfiledOptimizedTableCore(true);
 * 
 * // Use the table as normal
 * table.addColumn(...);
 * table.addRow(...);
 * 
 * // Get profiling reports
 * String columnsReport = table.getColumnsProfiler().generateReport();
 * String rowsReport = table.getRowsProfiler().generateReport();
 * String doubleStringsReport = table.getDoubleStringsProfiler().generateReport();
 * </pre>
 */
public class ProfiledOptimizedTableCore extends OptimizedTableCore {

    // Profilers for different collections
    private final ConcurrentCollectionProfiler.MapProfiler<String, IColumn<?>> columnsProfiler;
    private final ConcurrentCollectionProfiler.ListProfiler<IRow> rowsProfiler;
    private final ConcurrentCollectionProfiler.MapProfiler<String, Map<Integer, String>> doubleStringsProfiler;

    /**
     * Creates a new ProfiledOptimizedTableCore.
     */
    public ProfiledOptimizedTableCore() {
        super();
        this.columnsProfiler = ConcurrentCollectionProfiler.forMap("TableColumns");
        this.rowsProfiler = ConcurrentCollectionProfiler.forList("TableRows");
        this.doubleStringsProfiler = ConcurrentCollectionProfiler.forMap("DoubleStrings");
    }

    /**
     * Creates a new ProfiledOptimizedTableCore with the specified initial capacity for rows.
     * 
     * @param initialRowCapacity the initial capacity for the rows collection
     */
    public ProfiledOptimizedTableCore(int initialRowCapacity) {
        super(initialRowCapacity);
        this.columnsProfiler = ConcurrentCollectionProfiler.forMap("TableColumns");
        this.rowsProfiler = ConcurrentCollectionProfiler.forList("TableRows");
        this.doubleStringsProfiler = ConcurrentCollectionProfiler.forMap("DoubleStrings");
    }

    /**
     * Creates a new ProfiledOptimizedTableCore optimized for concurrent access.
     * 
     * @param concurrent whether to use thread-safe collections
     */
    public ProfiledOptimizedTableCore(boolean concurrent) {
        super(concurrent);
        this.columnsProfiler = ConcurrentCollectionProfiler.forMap("TableColumns");
        this.rowsProfiler = ConcurrentCollectionProfiler.forList("TableRows");
        this.doubleStringsProfiler = ConcurrentCollectionProfiler.forMap("DoubleStrings");
    }

    /**
     * Creates a new ProfiledOptimizedTableCore with specified initial capacity and concurrent access option.
     * 
     * @param concurrent whether to use thread-safe collections
     * @param initialRowCapacity the initial capacity for the rows collection
     */
    public ProfiledOptimizedTableCore(boolean concurrent, int initialRowCapacity) {
        super(concurrent, initialRowCapacity);
        this.columnsProfiler = ConcurrentCollectionProfiler.forMap("TableColumns");
        this.rowsProfiler = ConcurrentCollectionProfiler.forList("TableRows");
        this.doubleStringsProfiler = ConcurrentCollectionProfiler.forMap("DoubleStrings");
    }

    /**
     * Gets the profiler for the columns collection.
     * 
     * @return the columns profiler
     */
    public ConcurrentCollectionProfiler.MapProfiler<String, IColumn<?>> getColumnsProfiler() {
        return columnsProfiler;
    }

    /**
     * Gets the profiler for the rows collection.
     * 
     * @return the rows profiler
     */
    public ConcurrentCollectionProfiler.ListProfiler<IRow> getRowsProfiler() {
        return rowsProfiler;
    }

    /**
     * Gets the profiler for the originalDoubleStrings collection.
     * 
     * @return the originalDoubleStrings profiler
     */
    public ConcurrentCollectionProfiler.MapProfiler<String, Map<Integer, String>> getDoubleStringsProfiler() {
        return doubleStringsProfiler;
    }

    /**
     * Generates a comprehensive profiling report for all collections.
     * 
     * @return a string containing the report
     */
    public String generateProfilingReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== ProfiledOptimizedTableCore Profiling Report ===\n\n");

        report.append("--- Columns Collection ---\n");
        report.append(columnsProfiler.generateReport()).append("\n");

        report.append("--- Rows Collection ---\n");
        report.append(rowsProfiler.generateReport()).append("\n");

        report.append("--- DoubleStrings Collection ---\n");
        report.append(doubleStringsProfiler.generateReport()).append("\n");

        return report.toString();
    }

    /**
     * Resets all profiling data.
     */
    public void resetProfiling() {
        columnsProfiler.reset();
        rowsProfiler.reset();
        doubleStringsProfiler.reset();
    }

    // Override methods to add profiling hooks

    @Override
    public IColumn<?> getColumn(String name) {
        return columnsProfiler.profileGet(null, name, () -> super.getColumn(name));
    }

    @Override
    public void addColumn(IColumn<?> column) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null");
        }

        columnsProfiler.profilePut(null, column.getName(), column, () -> {
            super.addColumn(column);
            return null;
        });
    }

    @Override
    public IRow getRow(int index) {
        return rowsProfiler.profileGet(null, index, () -> super.getRow(index));
    }

    @Override
    public void addRow(IRow row) {
        // Use manual timing to ensure we record the operation correctly
        if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
            super.addRow(row);
            return;
        }

        long startTime = System.nanoTime();
        super.addRow(row);
        long endTime = System.nanoTime();

        // Record the write operation
        rowsProfiler.recordWriteOperation(row, endTime - startTime);
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> newColumns) {
        // Profile the operation as a whole
        long startTime = System.nanoTime();

        super.setColumns(newColumns);

        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        // Reset profiler since we're replacing all columns
        columnsProfiler.reset();

        // Record a write operation for each column
        for (Map.Entry<String, String> entry : newColumns.entrySet()) {
            final String key = entry.getKey();
            // Use recordWriteOperation to record the operation
            columnsProfiler.recordWriteOperation(key, 0); // 0 time since we already measured the whole operation
        }
    }

    @Override
    public void addRow(Map<String, String> row) {
        // Use manual timing to ensure we record the operation correctly
        if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
            super.addRow(row);
            return;
        }

        long startTime = System.nanoTime();
        super.addRow(row);
        long endTime = System.nanoTime();

        // Record the write operation
        rowsProfiler.recordWriteOperation(row, endTime - startTime);
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        // This method potentially accesses the originalDoubleStrings map
        // We need to manually time this operation since the return types don't match
        if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
            return super.getValueAt(rowIndex, columnName);
        }

        // Get the value object first to check if it's a Double
        Object value = getValueObject(rowIndex, columnName);
        if (value == null) {
            return null;
        }

        // Only profile doubleStrings access if the value is a Double
        if (value instanceof Double) {
            long startTime = System.nanoTime();
            String result = super.getValueAt(rowIndex, columnName);
            long endTime = System.nanoTime();

            // Record the read operation using the public method
            doubleStringsProfiler.recordReadOperation(columnName, endTime - startTime);

            return result;
        } else {
            // For non-Double values, just call the super method without profiling
            return super.getValueAt(rowIndex, columnName);
        }
    }

    @Override
    public void setValueAt(int rowIndex, String columnName, String value) {
        // This method potentially updates the originalDoubleStrings map
        // Use manual timing to ensure we record the operation correctly
        if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
            super.setValueAt(rowIndex, columnName, value);
            return;
        }

        // Only profile doubleStrings access if the value is a potential Double with a decimal point
        // This matches the condition in OptimizedTableCore.setValueAt() for adding to originalDoubleStrings
        IColumn<?> column = getColumn(columnName);
        if (column != null && column.getType() == Double.class && value != null && value.contains(".")) {
            long startTime = System.nanoTime();
            super.setValueAt(rowIndex, columnName, value);
            long endTime = System.nanoTime();

            // Record the write operation
            doubleStringsProfiler.recordWriteOperation(columnName, endTime - startTime);
        } else {
            // For non-Double values or Doubles without decimal points, just call the super method without profiling
            super.setValueAt(rowIndex, columnName, value);
        }
    }

}
