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

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * A utility class for profiling concurrent collections to identify bottlenecks.
 * This class provides methods for tracking and reporting metrics for different
 * types of concurrent collections, such as ConcurrentHashMap, CopyOnWriteArrayList, etc.
 * 
 * The profiler tracks metrics such as:
 * - Operation counts (reads, writes, etc.)
 * - Operation latencies (min, max, average)
 * - Contention metrics (when available)
 * - Memory usage estimates
 * 
 * Usage example:
 * <pre>
 * // Create a profiler for a ConcurrentHashMap
 * ConcurrentCollectionProfiler.MapProfiler<String, Integer> profiler = 
 *     ConcurrentCollectionProfiler.forMap("userCache");
 *     
 * // Profile a map operation
 * Integer value = profiler.profileGet(map, "key", () -> map.get("key"));
 * 
 * // Print profiling report
 * System.out.println(profiler.generateReport());
 * </pre>
 */
public class ConcurrentCollectionProfiler {

    // Singleton instance for global configuration
    private static final ConcurrentCollectionProfiler INSTANCE = new ConcurrentCollectionProfiler();

    // Global enable/disable flag
    private volatile boolean profilingEnabled = true;

    // Registry of all active profilers
    private final ConcurrentMap<String, CollectionProfiler<?>> profilers = new ConcurrentHashMap<>();

    /**
     * Private constructor to enforce singleton pattern.
     */
    private ConcurrentCollectionProfiler() {
    }

    /**
     * Gets the singleton instance of the profiler.
     * 
     * @return the singleton instance
     */
    public static ConcurrentCollectionProfiler getInstance() {
        return INSTANCE;
    }

    /**
     * Enables or disables profiling globally.
     * 
     * @param enabled true to enable profiling, false to disable
     */
    public void setProfilingEnabled(boolean enabled) {
        this.profilingEnabled = enabled;
    }

    /**
     * Checks if profiling is enabled globally.
     * 
     * @return true if profiling is enabled, false otherwise
     */
    public boolean isProfilingEnabled() {
        return profilingEnabled;
    }

    /**
     * Resets all profiling data.
     */
    public void resetAllProfilers() {
        profilers.values().forEach(CollectionProfiler::reset);
    }

    /**
     * Generates a report of all profilers.
     * 
     * @return a string containing the report
     */
    public String generateGlobalReport() {
        StringBuilder report = new StringBuilder("=== Concurrent Collection Profiling Report ===\n");

        profilers.forEach((name, profiler) -> {
            report.append("\n").append(profiler.generateReport());
        });

        return report.toString();
    }

    /**
     * Creates a new map profiler with the given name.
     * 
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param name the name of the profiler
     * @return a new map profiler
     */
    public static <K, V> MapProfiler<K, V> forMap(String name) {
        MapProfiler<K, V> profiler = new MapProfiler<>(name);
        getInstance().profilers.put(name, profiler);
        return profiler;
    }

    /**
     * Creates a new list profiler with the given name.
     * 
     * @param <E> the type of elements in the list
     * @param name the name of the profiler
     * @return a new list profiler
     */
    public static <E> ListProfiler<E> forList(String name) {
        ListProfiler<E> profiler = new ListProfiler<>(name);
        getInstance().profilers.put(name, profiler);
        return profiler;
    }

    /**
     * Creates a new set profiler with the given name.
     * 
     * @param <E> the type of elements in the set
     * @param name the name of the profiler
     * @return a new set profiler
     */
    public static <E> SetProfiler<E> forSet(String name) {
        SetProfiler<E> profiler = new SetProfiler<>(name);
        getInstance().profilers.put(name, profiler);
        return profiler;
    }

    /**
     * Base class for all collection profilers.
     * 
     * @param <T> the type of collection being profiled
     */
    public abstract static class CollectionProfiler<T> {
        protected final String name;
        protected final LongAdder readCount = new LongAdder();
        protected final LongAdder writeCount = new LongAdder();
        protected final LongAdder totalReadTimeNanos = new LongAdder();
        protected final LongAdder totalWriteTimeNanos = new LongAdder();
        protected final AtomicLong minReadTimeNanos = new AtomicLong(Long.MAX_VALUE);
        protected final AtomicLong maxReadTimeNanos = new AtomicLong(0);
        protected final AtomicLong minWriteTimeNanos = new AtomicLong(Long.MAX_VALUE);
        protected final AtomicLong maxWriteTimeNanos = new AtomicLong(0);

        /**
         * Creates a new collection profiler with the given name.
         * 
         * @param name the name of the profiler
         */
        protected CollectionProfiler(String name) {
            this.name = name;
        }

        /**
         * Resets all profiling data.
         */
        public void reset() {
            readCount.reset();
            writeCount.reset();
            totalReadTimeNanos.reset();
            totalWriteTimeNanos.reset();
            minReadTimeNanos.set(Long.MAX_VALUE);
            maxReadTimeNanos.set(0);
            minWriteTimeNanos.set(Long.MAX_VALUE);
            maxWriteTimeNanos.set(0);
        }

        /**
         * Records a read operation.
         * 
         * @param timeNanos the time taken for the operation in nanoseconds
         */
        protected void recordRead(long timeNanos) {
            readCount.increment();
            totalReadTimeNanos.add(timeNanos);
            updateMin(minReadTimeNanos, timeNanos);
            updateMax(maxReadTimeNanos, timeNanos);
        }

        /**
         * Records a write operation.
         * 
         * @param timeNanos the time taken for the operation in nanoseconds
         */
        protected void recordWrite(long timeNanos) {
            writeCount.increment();
            totalWriteTimeNanos.add(timeNanos);
            updateMin(minWriteTimeNanos, timeNanos);
            updateMax(maxWriteTimeNanos, timeNanos);
        }

        /**
         * Records a read operation with the given key.
         * This is a public method that can be used by external classes.
         * 
         * @param key the key that was read (for logging purposes)
         * @param timeNanos the time taken for the operation in nanoseconds
         */
        public void recordReadOperation(Object key, long timeNanos) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return;
            }
            recordRead(timeNanos);
        }

        /**
         * Records a write operation with the given key.
         * This is a public method that can be used by external classes.
         * 
         * @param key the key that was written (for logging purposes)
         * @param timeNanos the time taken for the operation in nanoseconds
         */
        public void recordWriteOperation(Object key, long timeNanos) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return;
            }
            recordWrite(timeNanos);
        }

        /**
         * Updates the minimum value atomically.
         * 
         * @param atomic the atomic value to update
         * @param value the new value to compare with
         */
        private void updateMin(AtomicLong atomic, long value) {
            long current;
            do {
                current = atomic.get();
                if (value >= current) {
                    break;
                }
            } while (!atomic.compareAndSet(current, value));
        }

        /**
         * Updates the maximum value atomically.
         * 
         * @param atomic the atomic value to update
         * @param value the new value to compare with
         */
        private void updateMax(AtomicLong atomic, long value) {
            long current;
            do {
                current = atomic.get();
                if (value <= current) {
                    break;
                }
            } while (!atomic.compareAndSet(current, value));
        }

        /**
         * Generates a report of the profiling data.
         * 
         * @return a string containing the report
         */
        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("=== Profiler: ").append(name).append(" ===\n");

            // Read metrics
            long reads = readCount.sum();
            report.append("Read operations: ").append(reads).append("\n");
            if (reads > 0) {
                double avgReadTimeMs = (double) totalReadTimeNanos.sum() / reads / 1_000_000.0;
                double minReadTimeMs = minReadTimeNanos.get() == Long.MAX_VALUE ? 0 : minReadTimeNanos.get() / 1_000_000.0;
                double maxReadTimeMs = maxReadTimeNanos.get() / 1_000_000.0;

                report.append("  Avg read time: ").append(String.format("%.3f", avgReadTimeMs)).append(" ms\n");
                report.append("  Min read time: ").append(String.format("%.3f", minReadTimeMs)).append(" ms\n");
                report.append("  Max read time: ").append(String.format("%.3f", maxReadTimeMs)).append(" ms\n");
            }

            // Write metrics
            long writes = writeCount.sum();
            report.append("Write operations: ").append(writes).append("\n");
            if (writes > 0) {
                double avgWriteTimeMs = (double) totalWriteTimeNanos.sum() / writes / 1_000_000.0;
                double minWriteTimeMs = minWriteTimeNanos.get() == Long.MAX_VALUE ? 0 : minWriteTimeNanos.get() / 1_000_000.0;
                double maxWriteTimeMs = maxWriteTimeNanos.get() / 1_000_000.0;

                report.append("  Avg write time: ").append(String.format("%.3f", avgWriteTimeMs)).append(" ms\n");
                report.append("  Min write time: ").append(String.format("%.3f", minWriteTimeMs)).append(" ms\n");
                report.append("  Max write time: ").append(String.format("%.3f", maxWriteTimeMs)).append(" ms\n");
            }

            // Read-write ratio
            if (reads > 0 || writes > 0) {
                double readPercentage = 100.0 * reads / (reads + writes);
                double writePercentage = 100.0 * writes / (reads + writes);
                report.append("Read-write ratio: ")
                      .append(String.format("%.1f", readPercentage)).append("% reads, ")
                      .append(String.format("%.1f", writePercentage)).append("% writes\n");
            }

            return report.toString();
        }
    }

    /**
     * Profiler for Map implementations.
     * 
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     */
    public static class MapProfiler<K, V> extends CollectionProfiler<Map<K, V>> {
        private final LongAdder getCount = new LongAdder();
        private final LongAdder putCount = new LongAdder();
        private final LongAdder removeCount = new LongAdder();
        private final LongAdder containsKeyCount = new LongAdder();

        /**
         * Creates a new map profiler with the given name.
         * 
         * @param name the name of the profiler
         */
        public MapProfiler(String name) {
            super(name);
        }

        @Override
        public void reset() {
            super.reset();
            getCount.reset();
            putCount.reset();
            removeCount.reset();
            containsKeyCount.reset();
        }

        /**
         * Profiles a get operation on the map.
         * 
         * @param map the map to profile
         * @param key the key to get
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public V profileGet(Map<K, V> map, K key, Supplier<V> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            V result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordRead(duration);
            getCount.increment();

            return result;
        }

        /**
         * Profiles a put operation on the map.
         * 
         * @param map the map to profile
         * @param key the key to put
         * @param value the value to put
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public V profilePut(Map<K, V> map, K key, V value, Supplier<V> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            V result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            putCount.increment();

            return result;
        }

        /**
         * Profiles a remove operation on the map.
         * 
         * @param map the map to profile
         * @param key the key to remove
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public V profileRemove(Map<K, V> map, K key, Supplier<V> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            V result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            removeCount.increment();

            return result;
        }

        /**
         * Profiles a containsKey operation on the map.
         * 
         * @param map the map to profile
         * @param key the key to check
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileContainsKey(Map<K, V> map, K key, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordRead(duration);
            containsKeyCount.increment();

            return result;
        }

        @Override
        public String generateReport() {
            StringBuilder report = new StringBuilder(super.generateReport());

            // Operation breakdown
            report.append("Operation breakdown:\n");
            report.append("  Get operations: ").append(getCount.sum()).append("\n");
            report.append("  Put operations: ").append(putCount.sum()).append("\n");
            report.append("  Remove operations: ").append(removeCount.sum()).append("\n");
            report.append("  ContainsKey operations: ").append(containsKeyCount.sum()).append("\n");

            return report.toString();
        }
    }

    /**
     * Profiler for List implementations.
     * 
     * @param <E> the type of elements in the list
     */
    public static class ListProfiler<E> extends CollectionProfiler<List<E>> {
        private final LongAdder getCount = new LongAdder();
        private final LongAdder addCount = new LongAdder();
        private final LongAdder removeCount = new LongAdder();
        private final LongAdder containsCount = new LongAdder();

        /**
         * Creates a new list profiler with the given name.
         * 
         * @param name the name of the profiler
         */
        public ListProfiler(String name) {
            super(name);
        }

        @Override
        public void reset() {
            super.reset();
            getCount.reset();
            addCount.reset();
            removeCount.reset();
            containsCount.reset();
        }

        /**
         * Profiles a get operation on the list.
         * 
         * @param list the list to profile
         * @param index the index to get
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public E profileGet(List<E> list, int index, Supplier<E> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            E result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordRead(duration);
            getCount.increment();

            return result;
        }

        /**
         * Profiles an add operation on the list.
         * 
         * @param list the list to profile
         * @param element the element to add
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileAdd(List<E> list, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            addCount.increment();

            return result;
        }

        /**
         * Profiles a remove operation on the list.
         * 
         * @param list the list to profile
         * @param element the element to remove
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileRemove(List<E> list, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            removeCount.increment();

            return result;
        }

        /**
         * Profiles a contains operation on the list.
         * 
         * @param list the list to profile
         * @param element the element to check
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileContains(List<E> list, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordRead(duration);
            containsCount.increment();

            return result;
        }

        @Override
        public String generateReport() {
            StringBuilder report = new StringBuilder(super.generateReport());

            // Operation breakdown
            report.append("Operation breakdown:\n");
            report.append("  Get operations: ").append(getCount.sum()).append("\n");
            report.append("  Add operations: ").append(addCount.sum()).append("\n");
            report.append("  Remove operations: ").append(removeCount.sum()).append("\n");
            report.append("  Contains operations: ").append(containsCount.sum()).append("\n");

            return report.toString();
        }
    }

    /**
     * Profiler for Set implementations.
     * 
     * @param <E> the type of elements in the set
     */
    public static class SetProfiler<E> extends CollectionProfiler<Set<E>> {
        private final LongAdder addCount = new LongAdder();
        private final LongAdder removeCount = new LongAdder();
        private final LongAdder containsCount = new LongAdder();

        /**
         * Creates a new set profiler with the given name.
         * 
         * @param name the name of the profiler
         */
        public SetProfiler(String name) {
            super(name);
        }

        @Override
        public void reset() {
            super.reset();
            addCount.reset();
            removeCount.reset();
            containsCount.reset();
        }

        /**
         * Profiles an add operation on the set.
         * 
         * @param set the set to profile
         * @param element the element to add
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileAdd(Set<E> set, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            addCount.increment();

            return result;
        }

        /**
         * Profiles a remove operation on the set.
         * 
         * @param set the set to profile
         * @param element the element to remove
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileRemove(Set<E> set, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordWrite(duration);
            removeCount.increment();

            return result;
        }

        /**
         * Profiles a contains operation on the set.
         * 
         * @param set the set to profile
         * @param element the element to check
         * @param operation the operation to profile
         * @return the result of the operation
         */
        public boolean profileContains(Set<E> set, E element, Supplier<Boolean> operation) {
            if (!ConcurrentCollectionProfiler.getInstance().isProfilingEnabled()) {
                return operation.get();
            }

            long startTime = System.nanoTime();
            boolean result = operation.get();
            long endTime = System.nanoTime();

            long duration = endTime - startTime;
            recordRead(duration);
            containsCount.increment();

            return result;
        }

        @Override
        public String generateReport() {
            StringBuilder report = new StringBuilder(super.generateReport());

            // Operation breakdown
            report.append("Operation breakdown:\n");
            report.append("  Add operations: ").append(addCount.sum()).append("\n");
            report.append("  Remove operations: ").append(removeCount.sum()).append("\n");
            report.append("  Contains operations: ").append(containsCount.sum()).append("\n");

            return report.toString();
        }
    }
}
