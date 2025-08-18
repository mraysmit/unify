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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ProfiledOptimizedTableCore.
 * This class demonstrates how to use the profiled table to identify bottlenecks
 * in concurrent collection usage.
 */
public class ProfiledOptimizedTableCoreTest {

    private ProfiledOptimizedTableCore profiledTable;
    private OptimizedTableCore standardTable;

    @BeforeEach
    void setUp() {
        // Create tables with the same structure
        profiledTable = new ProfiledOptimizedTableCore(true); // Use concurrent collections
        standardTable = new OptimizedTableCore(true);

        // Define columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");

        // Set columns for both tables
        profiledTable.setColumns(columns);
        standardTable.setColumns(columns);
    }

    /**
     * Test that the profiled table correctly tracks operations.
     */
    @Test
    void testProfilingBasicOperations() {
        // Add some rows to the table
        for (int i = 0; i < 10; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("Name", "Person" + i);
            row.put("Age", String.valueOf(20 + i));
            row.put("Salary", String.valueOf(50000.0 + i * 1000.0));
            row.put("IsActive", String.valueOf(i % 2 == 0));
            profiledTable.addRow(row);
        }

        // Perform some operations
        for (int i = 0; i < 5; i++) {
            profiledTable.getValueAt(i, "Name");
            profiledTable.getValueAt(i, "Salary");
        }

        // Update some values
        for (int i = 0; i < 3; i++) {
            profiledTable.setValueAt(i, "Age", String.valueOf(30 + i));
        }

        // Generate and print profiling reports
        System.out.println("=== Columns Profiler Report ===");
        System.out.println(profiledTable.getColumnsProfiler().generateReport());

        System.out.println("=== Rows Profiler Report ===");
        System.out.println(profiledTable.getRowsProfiler().generateReport());

        System.out.println("=== DoubleStrings Profiler Report ===");
        System.out.println(profiledTable.getDoubleStringsProfiler().generateReport());

        // Verify that operations were tracked
        assertTrue(profiledTable.getColumnsProfiler().generateReport().contains("Write operations:"));
        assertTrue(profiledTable.getRowsProfiler().generateReport().contains("Write operations: 10")); // 10 row additions
        assertTrue(profiledTable.getDoubleStringsProfiler().generateReport().contains("Read operations:")); // getValueAt operations
        assertTrue(profiledTable.getDoubleStringsProfiler().generateReport().contains("Write operations:")); // setValueAt operations
    }

    /**
     * Test that profiling can be enabled and disabled.
     */
    @Test
    void testEnableDisableProfiling() {
        // Enable profiling
        ConcurrentCollectionProfiler.getInstance().setProfilingEnabled(true);

        // Add a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Salary", "50000.0");
        row.put("IsActive", "true");
        profiledTable.addRow(row);

        // Verify that the operation was tracked
        assertTrue(profiledTable.getRowsProfiler().generateReport().contains("Write operations: 1"));

        // Reset profiling data
        profiledTable.resetProfiling();

        // Disable profiling
        ConcurrentCollectionProfiler.getInstance().setProfilingEnabled(false);

        // Add another row
        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Salary", "60000.0");
        row2.put("IsActive", "false");
        profiledTable.addRow(row2);

        // Verify that the operation was not tracked
        assertTrue(profiledTable.getRowsProfiler().generateReport().contains("Write operations: 0"));
    }

    /**
     * Test that the profiled table correctly identifies bottlenecks in concurrent usage.
     */
    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Ensure profiling is enabled
        ConcurrentCollectionProfiler.getInstance().setProfilingEnabled(true);

        // Create a profiled table with concurrent access
        ProfiledOptimizedTableCore concurrentTable = new ProfiledOptimizedTableCore(true);

        // Reset profiling data to ensure clean state
        concurrentTable.resetProfiling();

        // Define columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "string");
        columns.put("Value", "double");
        concurrentTable.setColumns(columns);

        // Number of threads and operations
        int numThreads = 10;
        int operationsPerThread = 100;

        // Create a countdown latch to synchronize thread start
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);

        // Create and start threads
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();

                    // Perform operations
                    for (int j = 0; j < operationsPerThread; j++) {
                        // Create a unique key for this thread and operation
                        String id = "thread-" + threadId + "-op-" + j;

                        // Add a row
                        Map<String, String> row = new HashMap<>();
                        row.put("ID", id);
                        row.put("Value", String.valueOf(threadId * 1000.0 + j));
                        concurrentTable.addRow(row);

                        // Read the row back
                        int rowIndex = concurrentTable.getRowCount() - 1;
                        concurrentTable.getValueAt(rowIndex, "ID");
                        concurrentTable.getValueAt(rowIndex, "Value");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        completionLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Generate and print profiling reports
        System.out.println("\n=== Concurrent Access Profiling Report ===");
        System.out.println(concurrentTable.generateProfilingReport());

        // Verify that operations were tracked
        String rowsReport = concurrentTable.getRowsProfiler().generateReport();
        String doubleStringsReport = concurrentTable.getDoubleStringsProfiler().generateReport();

        // Print the entire doubleStringsReport for debugging
        System.out.println("[DEBUG_LOG] DoubleStrings Profiler Report:");
        System.out.println("[DEBUG_LOG] " + doubleStringsReport.replace("\n", "\n[DEBUG_LOG] "));

        // Verify row additions
        assertTrue(rowsReport.contains("Write operations: " + (numThreads * operationsPerThread)));

        // Verify read operations on double strings
        assertTrue(doubleStringsReport.contains("Read operations:"));

        // Extract the number of read operations from the report
        String readOpsLine = doubleStringsReport.lines()
            .filter(line -> line.contains("Read operations:"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Read operations line not found in report"));

        int readOps = Integer.parseInt(readOpsLine.split(": ")[1].trim());

        // We expect at least some read operations since each thread reads the "Value" column (which is a Double)
        if (readOps == 0) {
            throw new AssertionError("Expected non-zero read operations for doubleStrings, but got: " + readOps + "\nReport: " + doubleStringsReport);
        }

        // Print the actual number of read operations
        System.out.println("[DEBUG_LOG] Number of read operations for doubleStrings: " + readOps);

        // Check for potential bottlenecks (high max times compared to average)
        if (doubleStringsReport.contains("Max read time:") && doubleStringsReport.contains("Avg read time:")) {
            String maxReadLine = doubleStringsReport.lines()
                .filter(line -> line.contains("Max read time:"))
                .findFirst()
                .orElse("");

            String avgReadLine = doubleStringsReport.lines()
                .filter(line -> line.contains("Avg read time:"))
                .findFirst()
                .orElse("");

            if (!maxReadLine.isEmpty() && !avgReadLine.isEmpty()) {
                double maxReadTime = Double.parseDouble(maxReadLine.split(": ")[1].split(" ")[0]);
                double avgReadTime = Double.parseDouble(avgReadLine.split(": ")[1].split(" ")[0]);

                System.out.println("Max read time: " + maxReadTime + " ms");
                System.out.println("Avg read time: " + avgReadTime + " ms");
                System.out.println("Ratio (max/avg): " + (maxReadTime / avgReadTime));

                // A high ratio could indicate contention
                if (maxReadTime / avgReadTime > 10) {
                    System.out.println("Potential bottleneck detected: Max read time is significantly higher than average");
                }
            }
        }
    }

    /**
     * Test that the profiled table can be used to compare different collection implementations.
     */
    @Test
    void testCompareCollectionImplementations() {
        // Create tables with different collection implementations
        ProfiledOptimizedTableCore concurrentTable = new ProfiledOptimizedTableCore(true);
        ProfiledOptimizedTableCore nonConcurrentTable = new ProfiledOptimizedTableCore(false);

        // Define columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "string");
        columns.put("Value", "double");
        concurrentTable.setColumns(columns);
        nonConcurrentTable.setColumns(columns);

        // Perform the same operations on both tables
        int numOperations = 1000;

        // Measure time for concurrent table
        long startTimeConcurrent = System.nanoTime();
        for (int i = 0; i < numOperations; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("ID", "id-" + i);
            row.put("Value", String.valueOf(i * 1.5));
            concurrentTable.addRow(row);
        }
        long endTimeConcurrent = System.nanoTime();
        long durationConcurrent = endTimeConcurrent - startTimeConcurrent;

        // Measure time for non-concurrent table
        long startTimeNonConcurrent = System.nanoTime();
        for (int i = 0; i < numOperations; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("ID", "id-" + i);
            row.put("Value", String.valueOf(i * 1.5));
            nonConcurrentTable.addRow(row);
        }
        long endTimeNonConcurrent = System.nanoTime();
        long durationNonConcurrent = endTimeNonConcurrent - startTimeNonConcurrent;

        // Print results
        System.out.println("\n=== Collection Implementation Comparison ===");
        System.out.println("Concurrent table time: " + (durationConcurrent / 1_000_000.0) + " ms");
        System.out.println("Non-concurrent table time: " + (durationNonConcurrent / 1_000_000.0) + " ms");
        System.out.println("Ratio (concurrent/non-concurrent): " + ((double) durationConcurrent / durationNonConcurrent));

        // Print profiling reports
        System.out.println("\n=== Concurrent Table Profiling Report ===");
        System.out.println(concurrentTable.generateProfilingReport());

        System.out.println("\n=== Non-Concurrent Table Profiling Report ===");
        System.out.println(nonConcurrentTable.generateProfilingReport());

        // Compare operation times
        String concurrentRowsReport = concurrentTable.getRowsProfiler().generateReport();
        String nonConcurrentRowsReport = nonConcurrentTable.getRowsProfiler().generateReport();

        if (concurrentRowsReport.contains("Avg write time:") && nonConcurrentRowsReport.contains("Avg write time:")) {
            String concurrentAvgWriteLine = concurrentRowsReport.lines()
                .filter(line -> line.contains("Avg write time:"))
                .findFirst()
                .orElse("");

            String nonConcurrentAvgWriteLine = nonConcurrentRowsReport.lines()
                .filter(line -> line.contains("Avg write time:"))
                .findFirst()
                .orElse("");

            if (!concurrentAvgWriteLine.isEmpty() && !nonConcurrentAvgWriteLine.isEmpty()) {
                double concurrentAvgWriteTime = Double.parseDouble(concurrentAvgWriteLine.split(": ")[1].split(" ")[0]);
                double nonConcurrentAvgWriteTime = Double.parseDouble(nonConcurrentAvgWriteLine.split(": ")[1].split(" ")[0]);

                System.out.println("Concurrent avg write time: " + concurrentAvgWriteTime + " ms");
                System.out.println("Non-concurrent avg write time: " + nonConcurrentAvgWriteTime + " ms");
                System.out.println("Ratio (concurrent/non-concurrent): " + (concurrentAvgWriteTime / nonConcurrentAvgWriteTime));
            }
        }
    }
}
