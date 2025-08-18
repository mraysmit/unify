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
package dev.mars.jtable.core.collections;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Benchmark for comparing the performance of different collection types under various read-write ratios.
 * This class provides methods for measuring the performance of Map, List, and Set implementations
 * with different read-write patterns.
 */
public class CollectionBenchmark {

    // Number of operations to perform in each benchmark
    private static final int OPERATIONS = 100_000;
    
    // Warm-up iterations to allow JVM optimizations
    private static final int WARM_UP_ITERATIONS = 5;
    
    // Benchmark iterations for averaging results
    private static final int BENCHMARK_ITERATIONS = 10;

    /**
     * Main method to run the benchmarks.
     */
    public static void main(String[] args) {
        System.out.println("Running collection benchmarks...");
        
        // Run Map benchmarks
        runMapBenchmarks();
        
        // Run List benchmarks
        runListBenchmarks();
        
        // Run Set benchmarks
        runSetBenchmarks();
    }

    /**
     * Runs benchmarks for different Map implementations.
     */
    private static void runMapBenchmarks() {
        System.out.println("\n=== Map Benchmarks ===");
        
        // Create suppliers for different Map implementations
        Map<String, Supplier<Map<String, String>>> mapSuppliers = new LinkedHashMap<>();
        mapSuppliers.put("HashMap", HashMap::new);
        mapSuppliers.put("LinkedHashMap", LinkedHashMap::new);
        mapSuppliers.put("TreeMap", TreeMap::new);
        mapSuppliers.put("ConcurrentHashMap", ConcurrentHashMap::new);
        
        // Define different read-write ratios to test
        int[][] readWriteRatios = {
            {90, 10},  // Read-heavy: 90% reads, 10% writes
            {50, 50},  // Balanced: 50% reads, 50% writes
            {10, 90}   // Write-heavy: 10% reads, 90% writes
        };
        
        // Run benchmarks for each read-write ratio
        for (int[] ratio : readWriteRatios) {
            int readPercentage = ratio[0];
            int writePercentage = ratio[1];
            
            System.out.println("\nRead-Write Ratio: " + readPercentage + "% reads, " + writePercentage + "% writes");
            System.out.println("Implementation\tAverage Time (ms)");
            
            // Run benchmark for each Map implementation
            for (Map.Entry<String, Supplier<Map<String, String>>> entry : mapSuppliers.entrySet()) {
                String name = entry.getKey();
                Supplier<Map<String, String>> supplier = entry.getValue();
                
                // Run the benchmark
                long averageTime = benchmarkMap(supplier, readPercentage, writePercentage);
                
                // Print results
                System.out.println(name + "\t" + averageTime);
            }
        }
    }

    /**
     * Runs benchmarks for different List implementations.
     */
    private static void runListBenchmarks() {
        System.out.println("\n=== List Benchmarks ===");
        
        // Create suppliers for different List implementations
        Map<String, Supplier<List<String>>> listSuppliers = new LinkedHashMap<>();
        listSuppliers.put("ArrayList", ArrayList::new);
        listSuppliers.put("LinkedList", LinkedList::new);
        listSuppliers.put("CopyOnWriteArrayList", CopyOnWriteArrayList::new);
        
        // Define different read-write ratios to test
        int[][] readWriteRatios = {
            {90, 10},  // Read-heavy: 90% reads, 10% writes
            {50, 50},  // Balanced: 50% reads, 50% writes
            {10, 90}   // Write-heavy: 10% reads, 90% writes
        };
        
        // Run benchmarks for each read-write ratio
        for (int[] ratio : readWriteRatios) {
            int readPercentage = ratio[0];
            int writePercentage = ratio[1];
            
            System.out.println("\nRead-Write Ratio: " + readPercentage + "% reads, " + writePercentage + "% writes");
            System.out.println("Implementation\tAverage Time (ms)");
            
            // Run benchmark for each List implementation
            for (Map.Entry<String, Supplier<List<String>>> entry : listSuppliers.entrySet()) {
                String name = entry.getKey();
                Supplier<List<String>> supplier = entry.getValue();
                
                // Run the benchmark
                long averageTime = benchmarkList(supplier, readPercentage, writePercentage);
                
                // Print results
                System.out.println(name + "\t" + averageTime);
            }
        }
    }

    /**
     * Runs benchmarks for different Set implementations.
     */
    private static void runSetBenchmarks() {
        System.out.println("\n=== Set Benchmarks ===");
        
        // Create suppliers for different Set implementations
        Map<String, Supplier<Set<String>>> setSuppliers = new LinkedHashMap<>();
        setSuppliers.put("HashSet", HashSet::new);
        setSuppliers.put("LinkedHashSet", LinkedHashSet::new);
        setSuppliers.put("TreeSet", TreeSet::new);
        setSuppliers.put("CopyOnWriteArraySet", CopyOnWriteArraySet::new);
        setSuppliers.put("ConcurrentSkipListSet", ConcurrentSkipListSet::new);
        
        // Define different read-write ratios to test
        int[][] readWriteRatios = {
            {90, 10},  // Read-heavy: 90% reads, 10% writes
            {50, 50},  // Balanced: 50% reads, 50% writes
            {10, 90}   // Write-heavy: 10% reads, 90% writes
        };
        
        // Run benchmarks for each read-write ratio
        for (int[] ratio : readWriteRatios) {
            int readPercentage = ratio[0];
            int writePercentage = ratio[1];
            
            System.out.println("\nRead-Write Ratio: " + readPercentage + "% reads, " + writePercentage + "% writes");
            System.out.println("Implementation\tAverage Time (ms)");
            
            // Run benchmark for each Set implementation
            for (Map.Entry<String, Supplier<Set<String>>> entry : setSuppliers.entrySet()) {
                String name = entry.getKey();
                Supplier<Set<String>> supplier = entry.getValue();
                
                // Run the benchmark
                long averageTime = benchmarkSet(supplier, readPercentage, writePercentage);
                
                // Print results
                System.out.println(name + "\t" + averageTime);
            }
        }
    }

    /**
     * Benchmarks a Map implementation with the specified read-write ratio.
     *
     * @param mapSupplier the supplier for the Map implementation to benchmark
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @return the average time in milliseconds
     */
    private static long benchmarkMap(Supplier<Map<String, String>> mapSupplier, int readPercentage, int writePercentage) {
        // Warm-up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            runMapOperations(mapSupplier.get(), readPercentage, writePercentage, OPERATIONS / 10);
        }
        
        // Benchmark
        long totalTime = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Map<String, String> map = mapSupplier.get();
            long startTime = System.currentTimeMillis();
            runMapOperations(map, readPercentage, writePercentage, OPERATIONS);
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);
        }
        
        return totalTime / BENCHMARK_ITERATIONS;
    }

    /**
     * Runs operations on a Map with the specified read-write ratio.
     *
     * @param map the Map to operate on
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @param operations the number of operations to perform
     */
    private static void runMapOperations(Map<String, String> map, int readPercentage, int writePercentage, int operations) {
        Random random = new Random();
        
        // Pre-populate the map with some entries
        for (int i = 0; i < operations / 10; i++) {
            map.put("key" + i, "value" + i);
        }
        
        // Run operations with the specified read-write ratio
        for (int i = 0; i < operations; i++) {
            int operation = random.nextInt(100);
            
            if (operation < readPercentage) {
                // Read operation
                int key = random.nextInt(operations / 10);
                map.get("key" + key);
            } else {
                // Write operation
                int key = random.nextInt(operations);
                map.put("key" + key, "value" + key);
            }
        }
    }

    /**
     * Benchmarks a List implementation with the specified read-write ratio.
     *
     * @param listSupplier the supplier for the List implementation to benchmark
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @return the average time in milliseconds
     */
    private static long benchmarkList(Supplier<List<String>> listSupplier, int readPercentage, int writePercentage) {
        // Warm-up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            runListOperations(listSupplier.get(), readPercentage, writePercentage, OPERATIONS / 10);
        }
        
        // Benchmark
        long totalTime = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            List<String> list = listSupplier.get();
            long startTime = System.currentTimeMillis();
            runListOperations(list, readPercentage, writePercentage, OPERATIONS);
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);
        }
        
        return totalTime / BENCHMARK_ITERATIONS;
    }

    /**
     * Runs operations on a List with the specified read-write ratio.
     *
     * @param list the List to operate on
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @param operations the number of operations to perform
     */
    private static void runListOperations(List<String> list, int readPercentage, int writePercentage, int operations) {
        Random random = new Random();
        
        // Pre-populate the list with some entries
        for (int i = 0; i < operations / 10; i++) {
            list.add("value" + i);
        }
        
        // Run operations with the specified read-write ratio
        for (int i = 0; i < operations; i++) {
            int operation = random.nextInt(100);
            
            if (operation < readPercentage) {
                // Read operation
                int index = random.nextInt(list.size());
                list.get(index);
            } else {
                // Write operation (add or remove with equal probability)
                if (random.nextBoolean() && !list.isEmpty()) {
                    // Remove operation
                    int index = random.nextInt(list.size());
                    list.remove(index);
                } else {
                    // Add operation
                    list.add("value" + i);
                }
            }
        }
    }

    /**
     * Benchmarks a Set implementation with the specified read-write ratio.
     *
     * @param setSupplier the supplier for the Set implementation to benchmark
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @return the average time in milliseconds
     */
    private static long benchmarkSet(Supplier<Set<String>> setSupplier, int readPercentage, int writePercentage) {
        // Warm-up
        for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
            runSetOperations(setSupplier.get(), readPercentage, writePercentage, OPERATIONS / 10);
        }
        
        // Benchmark
        long totalTime = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            Set<String> set = setSupplier.get();
            long startTime = System.currentTimeMillis();
            runSetOperations(set, readPercentage, writePercentage, OPERATIONS);
            long endTime = System.currentTimeMillis();
            totalTime += (endTime - startTime);
        }
        
        return totalTime / BENCHMARK_ITERATIONS;
    }

    /**
     * Runs operations on a Set with the specified read-write ratio.
     *
     * @param set the Set to operate on
     * @param readPercentage the percentage of read operations
     * @param writePercentage the percentage of write operations
     * @param operations the number of operations to perform
     */
    private static void runSetOperations(Set<String> set, int readPercentage, int writePercentage, int operations) {
        Random random = new Random();
        
        // Pre-populate the set with some entries
        for (int i = 0; i < operations / 10; i++) {
            set.add("value" + i);
        }
        
        // Run operations with the specified read-write ratio
        for (int i = 0; i < operations; i++) {
            int operation = random.nextInt(100);
            
            if (operation < readPercentage) {
                // Read operation
                int value = random.nextInt(operations / 10);
                set.contains("value" + value);
            } else {
                // Write operation (add or remove with equal probability)
                if (random.nextBoolean() && !set.isEmpty()) {
                    // Remove operation
                    int value = random.nextInt(operations / 10);
                    set.remove("value" + value);
                } else {
                    // Add operation
                    set.add("value" + i);
                }
            }
        }
    }
}