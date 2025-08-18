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
package dev.mars.jtable.core.profiling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ConcurrentCollectionProfiler.
 * This class verifies that the profiler correctly tracks metrics for different
 * types of concurrent collections and generates accurate reports.
 */
public class ConcurrentCollectionProfilerTest {

    private ConcurrentCollectionProfiler profiler;
    private ConcurrentCollectionProfiler.MapProfiler<String, String> mapProfiler;
    private ConcurrentCollectionProfiler.ListProfiler<String> listProfiler;
    private ConcurrentCollectionProfiler.SetProfiler<String> setProfiler;
    
    private Map<String, String> concurrentMap;
    private List<String> concurrentList;
    private Set<String> concurrentSet;
    
    @BeforeEach
    void setUp() {
        // Get the singleton instance
        profiler = ConcurrentCollectionProfiler.getInstance();
        profiler.setProfilingEnabled(true);
        
        // Reset all profilers
        profiler.resetAllProfilers();
        
        // Create profilers for different collection types
        mapProfiler = ConcurrentCollectionProfiler.forMap("testMap");
        listProfiler = ConcurrentCollectionProfiler.forList("testList");
        setProfiler = ConcurrentCollectionProfiler.forSet("testSet");
        
        // Create concurrent collections
        concurrentMap = new ConcurrentHashMap<>();
        concurrentList = new CopyOnWriteArrayList<>();
        concurrentSet = new CopyOnWriteArraySet<>();
    }
    
    /**
     * Test that the profiler correctly tracks map operations.
     */
    @Test
    void testMapProfiler() {
        // Profile put operations
        for (int i = 0; i < 10; i++) {
            String key = "key" + i;
            String value = "value" + i;
            mapProfiler.profilePut(concurrentMap, key, value, () -> concurrentMap.put(key, value));
        }
        
        // Profile get operations
        for (int i = 0; i < 10; i++) {
            String key = "key" + i;
            mapProfiler.profileGet(concurrentMap, key, () -> concurrentMap.get(key));
        }
        
        // Profile containsKey operations
        for (int i = 0; i < 5; i++) {
            String key = "key" + i;
            mapProfiler.profileContainsKey(concurrentMap, key, () -> concurrentMap.containsKey(key));
        }
        
        // Profile remove operations
        for (int i = 0; i < 3; i++) {
            String key = "key" + i;
            mapProfiler.profileRemove(concurrentMap, key, () -> concurrentMap.remove(key));
        }
        
        // Generate and print report
        String report = mapProfiler.generateReport();
        System.out.println(report);
        
        // Verify metrics
        assertEquals(10, concurrentMap.size() + 3); // 10 puts, 3 removes
        assertTrue(report.contains("Read operations: 15")); // 10 gets + 5 containsKey
        assertTrue(report.contains("Write operations: 13")); // 10 puts + 3 removes
        assertTrue(report.contains("Get operations: 10"));
        assertTrue(report.contains("Put operations: 10"));
        assertTrue(report.contains("Remove operations: 3"));
        assertTrue(report.contains("ContainsKey operations: 5"));
    }
    
    /**
     * Test that the profiler correctly tracks list operations.
     */
    @Test
    void testListProfiler() {
        // Profile add operations
        for (int i = 0; i < 10; i++) {
            String element = "element" + i;
            listProfiler.profileAdd(concurrentList, element, () -> concurrentList.add(element));
        }
        
        // Profile get operations
        for (int i = 0; i < 10; i++) {
            int index = i;
            listProfiler.profileGet(concurrentList, index, () -> concurrentList.get(index));
        }
        
        // Profile contains operations
        for (int i = 0; i < 5; i++) {
            String element = "element" + i;
            listProfiler.profileContains(concurrentList, element, () -> concurrentList.contains(element));
        }
        
        // Profile remove operations
        for (int i = 0; i < 3; i++) {
            String element = "element" + i;
            listProfiler.profileRemove(concurrentList, element, () -> concurrentList.remove(element));
        }
        
        // Generate and print report
        String report = listProfiler.generateReport();
        System.out.println(report);
        
        // Verify metrics
        assertEquals(7, concurrentList.size()); // 10 adds, 3 removes
        assertTrue(report.contains("Read operations: 15")); // 10 gets + 5 contains
        assertTrue(report.contains("Write operations: 13")); // 10 adds + 3 removes
        assertTrue(report.contains("Get operations: 10"));
        assertTrue(report.contains("Add operations: 10"));
        assertTrue(report.contains("Remove operations: 3"));
        assertTrue(report.contains("Contains operations: 5"));
    }
    
    /**
     * Test that the profiler correctly tracks set operations.
     */
    @Test
    void testSetProfiler() {
        // Profile add operations
        for (int i = 0; i < 10; i++) {
            String element = "element" + i;
            setProfiler.profileAdd(concurrentSet, element, () -> concurrentSet.add(element));
        }
        
        // Profile contains operations
        for (int i = 0; i < 5; i++) {
            String element = "element" + i;
            setProfiler.profileContains(concurrentSet, element, () -> concurrentSet.contains(element));
        }
        
        // Profile remove operations
        for (int i = 0; i < 3; i++) {
            String element = "element" + i;
            setProfiler.profileRemove(concurrentSet, element, () -> concurrentSet.remove(element));
        }
        
        // Generate and print report
        String report = setProfiler.generateReport();
        System.out.println(report);
        
        // Verify metrics
        assertEquals(7, concurrentSet.size()); // 10 adds, 3 removes
        assertTrue(report.contains("Read operations: 5")); // 5 contains
        assertTrue(report.contains("Write operations: 13")); // 10 adds + 3 removes
        assertTrue(report.contains("Add operations: 10"));
        assertTrue(report.contains("Remove operations: 3"));
        assertTrue(report.contains("Contains operations: 5"));
    }
    
    /**
     * Test that the profiler can be enabled and disabled globally.
     */
    @Test
    void testEnableDisable() {
        // Enable profiling
        profiler.setProfilingEnabled(true);
        assertTrue(profiler.isProfilingEnabled());
        
        // Profile some operations
        mapProfiler.profilePut(concurrentMap, "key", "value", () -> concurrentMap.put("key", "value"));
        
        // Verify that operations were profiled
        String report = mapProfiler.generateReport();
        assertTrue(report.contains("Write operations: 1"));
        
        // Reset profilers
        profiler.resetAllProfilers();
        
        // Disable profiling
        profiler.setProfilingEnabled(false);
        assertFalse(profiler.isProfilingEnabled());
        
        // Profile some operations
        mapProfiler.profilePut(concurrentMap, "key2", "value2", () -> concurrentMap.put("key2", "value2"));
        
        // Verify that operations were not profiled
        report = mapProfiler.generateReport();
        assertTrue(report.contains("Write operations: 0"));
    }
    
    /**
     * Test that the profiler correctly generates a global report.
     */
    @Test
    void testGlobalReport() {
        // Profile some operations on each collection type
        mapProfiler.profilePut(concurrentMap, "key", "value", () -> concurrentMap.put("key", "value"));
        listProfiler.profileAdd(concurrentList, "element", () -> concurrentList.add("element"));
        setProfiler.profileAdd(concurrentSet, "element", () -> concurrentSet.add("element"));
        
        // Generate global report
        String report = profiler.generateGlobalReport();
        System.out.println(report);
        
        // Verify that the report contains data for all profilers
        assertTrue(report.contains("=== Profiler: testMap ==="));
        assertTrue(report.contains("=== Profiler: testList ==="));
        assertTrue(report.contains("=== Profiler: testSet ==="));
    }
    
    /**
     * Test that the profiler correctly handles concurrent access.
     */
    @Test
    void testConcurrentAccess() throws InterruptedException {
        // Create a shared map and profiler
        Map<String, String> sharedMap = new ConcurrentHashMap<>();
        ConcurrentCollectionProfiler.MapProfiler<String, String> sharedProfiler = 
            ConcurrentCollectionProfiler.forMap("sharedMap");
        
        // Create and start multiple threads that access the map concurrently
        int numThreads = 10;
        int operationsPerThread = 100;
        Thread[] threads = new Thread[numThreads];
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    String key = "key-" + threadId + "-" + j;
                    String value = "value-" + threadId + "-" + j;
                    
                    // Perform put and get operations
                    sharedProfiler.profilePut(sharedMap, key, value, () -> sharedMap.put(key, value));
                    sharedProfiler.profileGet(sharedMap, key, () -> sharedMap.get(key));
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Generate and print report
        String report = sharedProfiler.generateReport();
        System.out.println(report);
        
        // Verify metrics
        assertEquals(numThreads * operationsPerThread, sharedMap.size());
        assertTrue(report.contains("Read operations: " + (numThreads * operationsPerThread)));
        assertTrue(report.contains("Write operations: " + (numThreads * operationsPerThread)));
    }
    
    /**
     * Test that the profiler correctly measures operation latencies.
     */
    @Test
    void testLatencyMeasurement() throws InterruptedException {
        // Create a map and profiler
        Map<String, String> map = new HashMap<>();
        ConcurrentCollectionProfiler.MapProfiler<String, String> latencyProfiler = 
            ConcurrentCollectionProfiler.forMap("latencyMap");
        
        // Profile a fast operation
        latencyProfiler.profilePut(map, "fastKey", "value", () -> map.put("fastKey", "value"));
        
        // Profile a slow operation
        latencyProfiler.profilePut(map, "slowKey", "value", () -> {
            try {
                Thread.sleep(100); // Simulate a slow operation
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return map.put("slowKey", "value");
        });
        
        // Generate and print report
        String report = latencyProfiler.generateReport();
        System.out.println(report);
        
        // Verify that the max write time is significantly higher than the min write time
        assertTrue(report.contains("Min write time:"));
        assertTrue(report.contains("Max write time:"));
        
        // Extract min and max times from the report
        String minLine = report.lines()
            .filter(line -> line.contains("Min write time:"))
            .findFirst()
            .orElse("");
        
        String maxLine = report.lines()
            .filter(line -> line.contains("Max write time:"))
            .findFirst()
            .orElse("");
        
        double minTime = Double.parseDouble(minLine.split(": ")[1].split(" ")[0]);
        double maxTime = Double.parseDouble(maxLine.split(": ")[1].split(" ")[0]);
        
        // Verify that max time is at least 50ms greater than min time
        assertTrue(maxTime - minTime >= 50.0, "Max time should be significantly higher than min time");
    }
}