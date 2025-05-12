# Profiling Concurrent Collections

This document explains how to use the profiling support in the Java Table project to identify bottlenecks in concurrent collection usage.

## Overview

The Java Table project includes a profiling framework that can help identify performance bottlenecks in concurrent collections. The framework tracks metrics such as:

- Operation counts (reads, writes, etc.)
- Operation latencies (min, max, average)
- Read-write ratios
- Specific operation types (get, put, add, remove, etc.)

This information can help you identify bottlenecks, optimize collection usage, and make informed decisions about which collection types to use for different scenarios.

## Getting Started

### Using the ConcurrentCollectionProfiler

The `ConcurrentCollectionProfiler` class provides a simple way to profile any collection in your code:

```java
// Create a profiler for a Map
ConcurrentCollectionProfiler.MapProfiler<String, Integer> mapProfiler = 
    ConcurrentCollectionProfiler.forMap("userCache");

// Profile a get operation
Integer value = mapProfiler.profileGet(map, "key", () -> map.get("key"));

// Profile a put operation
mapProfiler.profilePut(map, "key", 42, () -> map.put("key", 42));

// Generate a report
String report = mapProfiler.generateReport();
System.out.println(report);
```

Similar profilers are available for List and Set collections:

```java
// Create a profiler for a List
ConcurrentCollectionProfiler.ListProfiler<String> listProfiler = 
    ConcurrentCollectionProfiler.forList("userList");

// Create a profiler for a Set
ConcurrentCollectionProfiler.SetProfiler<String> setProfiler = 
    ConcurrentCollectionProfiler.forSet("userSet");
```

### Using ProfiledOptimizedTableCore

For a more integrated approach, you can use the `ProfiledOptimizedTableCore` class, which extends `OptimizedTableCore` and adds profiling hooks to key collection operations:

```java
// Create a profiled table with concurrent access
ProfiledOptimizedTableCore table = new ProfiledOptimizedTableCore(true);

// Use the table as normal
LinkedHashMap<String, String> columns = new LinkedHashMap<>();
columns.put("Name", "string");
columns.put("Age", "int");
table.setColumns(columns);

// Add some rows
Map<String, String> row = new HashMap<>();
row.put("Name", "Alice");
row.put("Age", "30");
table.addRow(row);

// Get profiling reports
String columnsReport = table.getColumnsProfiler().generateReport();
String rowsReport = table.getRowsProfiler().generateReport();
String doubleStringsReport = table.getDoubleStringsProfiler().generateReport();

// Or get a comprehensive report
String fullReport = table.generateProfilingReport();
```

## Identifying Bottlenecks

When analyzing profiling reports, look for the following indicators of potential bottlenecks:

1. **High maximum latency compared to average latency**: This can indicate contention or occasional performance issues.

   ```
   Avg read time: 0.050 ms
   Max read time: 5.000 ms
   ```

   If the maximum time is significantly higher than the average (e.g., 10x or more), it suggests that some operations are experiencing delays, possibly due to contention.

2. **Imbalanced read-write ratio**: Different collection types are optimized for different read-write ratios.

   ```
   Read-write ratio: 90.0% reads, 10.0% writes
   ```

   For read-heavy workloads (e.g., 90% reads, 10% writes), consider collections like `CopyOnWriteArrayList` or `ConcurrentHashMap`. For write-heavy workloads, other collections may be more appropriate.

3. **High operation counts**: If certain operations are performed much more frequently than others, optimize for those operations.

   ```
   Get operations: 10000
   Put operations: 100
   Remove operations: 10
   ```

   In this example, get operations dominate, so you should optimize for read performance.

## Optimization Strategies

Based on profiling results, consider the following optimization strategies:

### For Maps

1. **Read-heavy workloads (>80% reads)**:
   - Use `ConcurrentHashMap` with appropriate initial capacity
   - Consider read-only snapshots for purely read operations

2. **Write-heavy workloads (>50% writes)**:
   - Use `ConcurrentHashMap` with appropriate concurrency level
   - Consider batching writes to reduce contention
   - Use partitioning strategies to distribute writes across different segments

3. **High contention (high max/avg latency ratio)**:
   - Increase the concurrency level of `ConcurrentHashMap`
   - Use striped locks or partitioning to reduce contention points
   - Consider using lock-free algorithms if appropriate

### For Lists

1. **Read-heavy workloads (>80% reads)**:
   - Use `CopyOnWriteArrayList` for thread safety with minimal read overhead
   - Consider immutable lists for purely read operations

2. **Write-heavy workloads (>50% writes)**:
   - Use `ConcurrentLinkedDeque` or synchronized `ArrayList`
   - Avoid `CopyOnWriteArrayList` due to high write overhead
   - Consider batching writes to reduce overhead

3. **Random access vs. sequential access**:
   - For frequent random access, use `ArrayList` with synchronization
   - For frequent insertions/deletions, use `LinkedList` with synchronization

### For Sets

1. **Read-heavy workloads (>80% reads)**:
   - Use `CopyOnWriteArraySet` for thread safety with minimal read overhead
   - Consider `ConcurrentHashMap.newKeySet()` for better scalability

2. **Write-heavy workloads (>50% writes)**:
   - Use `ConcurrentHashMap.newKeySet()` for better write performance
   - Avoid `CopyOnWriteArraySet` due to high write overhead

3. **Ordered sets**:
   - Use `ConcurrentSkipListSet` for maintaining order in concurrent environments

## Sizing Strategies

Proper sizing of concurrent collections can significantly reduce contention and improve performance:

1. **Initial capacity**: Set the initial capacity to avoid resizing operations, which can be expensive in concurrent contexts.

   ```java
   // For a map expected to hold 100 elements with default load factor (0.75)
   Map<String, String> map = new ConcurrentHashMap<>((int)(100 / 0.75) + 1);
   ```

2. **Load factor**: The default load factor (0.75) provides a good balance between space efficiency and performance. Lower values reduce collision probability but increase memory usage.

3. **Concurrency level**: For Java 7 and earlier, set the concurrency level to the estimated number of threads that will concurrently modify the map. In Java 8+, `ConcurrentHashMap` dynamically adjusts the concurrency level.

## Example: Identifying and Fixing a Bottleneck

Here's an example of how to use profiling to identify and fix a bottleneck:

1. **Identify the bottleneck**:

   ```java
   ProfiledOptimizedTableCore table = new ProfiledOptimizedTableCore(true);
   // ... use the table in a concurrent environment ...
   String report = table.generateProfilingReport();
   System.out.println(report);
   ```

   The report shows:
   ```
   === Profiler: TableRows ===
   Read operations: 10000
   Avg read time: 0.050 ms
   Max read time: 10.000 ms
   Write operations: 1000
   Avg write time: 0.500 ms
   Max write time: 50.000 ms
   ```

   The high max write time compared to the average suggests contention during write operations.

2. **Fix the bottleneck**:

   ```java
   // Before: Using a single collection for all rows
   private final List<IRow> rows = new CopyOnWriteArrayList<>();

   // After: Using partitioning to reduce contention
   private final List<List<IRow>> partitions = new ArrayList<>(16);
   {
       for (int i = 0; i < 16; i++) {
           partitions.add(new CopyOnWriteArrayList<>());
       }
   }

   // Add a row to the appropriate partition
   public void addRow(IRow row) {
       int partition = Math.abs(row.hashCode() % partitions.size());
       partitions.get(partition).add(row);
   }
   ```

3. **Verify the improvement**:

   After implementing the fix, run the profiling again to verify that the max write time has decreased and is closer to the average.

## Conclusion

Profiling concurrent collections can help you identify bottlenecks and optimize your code for better performance. By using the `ConcurrentCollectionProfiler` and `ProfiledOptimizedTableCore` classes, you can gather valuable metrics about your collection usage and make informed decisions about which collection types and optimization strategies to use.

Remember that profiling should be done in an environment that closely resembles your production environment to get the most accurate results. Also, be aware that profiling itself adds some overhead, so it's best to enable it only when needed and disable it in production.