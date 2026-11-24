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
package dev.mars.jtable.integration.performance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Utility class for benchmarking data integration operations.
 * Provides methods to measure performance of various operations with automatic
 * memory monitoring and timing.
 */
public class PerformanceBenchmark {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmark.class);
    
    private final Runtime runtime;
    
    public PerformanceBenchmark() {
        this.runtime = Runtime.getRuntime();
    }
    
    /**
     * Benchmarks a Runnable operation that doesn't return a value.
     * 
     * @param operationName descriptive name for the operation
     * @param operation the operation to benchmark
     * @return performance metrics for the operation
     */
    public PerformanceMetrics benchmark(String operationName, Runnable operation) {
        return benchmark(operationName, (Supplier<Object>) () -> {
            operation.run();
            return null;
        });
    }
    
    /**
     * Benchmarks a Callable operation that returns a value.
     * 
     * @param operationName descriptive name for the operation
     * @param operation the operation to benchmark
     * @param <T> the return type of the operation
     * @return performance metrics for the operation
     */
    public <T> PerformanceMetrics benchmark(String operationName, Callable<T> operation) {
        return benchmark(operationName, (Supplier<T>) () -> {
            try {
                return operation.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Benchmarks a Supplier operation.
     * 
     * @param operationName descriptive name for the operation
     * @param operation the operation to benchmark
     * @param <T> the return type of the operation
     * @return performance metrics for the operation
     */
    public <T> PerformanceMetrics benchmark(String operationName, Supplier<T> operation) {
        logger.debug("Starting benchmark for operation: {}", operationName);
        
        // Force garbage collection before measurement
        System.gc();
        
        // Capture initial state
        long initialMemory = getUsedMemory();
        Instant startTime = Instant.now();
        
        boolean successful = true;
        String errorMessage = null;
        
        try {
            // Execute the operation
            T result = operation.get();
            
            // Log successful completion
            logger.debug("Operation '{}' completed successfully", operationName);
            
        } catch (Exception e) {
            successful = false;
            errorMessage = e.getMessage();
            logger.error("Operation '{}' failed: {}", operationName, e.getMessage(), e);
        }
        
        // Capture final state
        Instant endTime = Instant.now();
        
        // Force garbage collection to get accurate memory reading
        System.gc();
        long finalMemory = getUsedMemory();
        long memoryUsed = Math.max(0, finalMemory - initialMemory);
        
        PerformanceMetrics metrics = PerformanceMetrics.builder(operationName)
                .startTime(startTime)
                .endTime(endTime)
                .memoryUsed(memoryUsed)
                .successful(successful)
                .error(errorMessage)
                .build();
        
        logger.info("Benchmark completed: {}", metrics);
        return metrics;
    }
    
    /**
     * Benchmarks a data processing operation with record and byte counts.
     * 
     * @param operationName descriptive name for the operation
     * @param recordCount number of records processed
     * @param byteCount number of bytes processed
     * @param operation the operation to benchmark
     * @return performance metrics for the operation
     */
    public PerformanceMetrics benchmarkDataOperation(String operationName, 
                                                    long recordCount, 
                                                    long byteCount, 
                                                    Runnable operation) {
        logger.debug("Starting data operation benchmark: {} (records: {}, bytes: {})", 
                    operationName, recordCount, byteCount);
        
        // Force garbage collection before measurement
        System.gc();
        
        // Capture initial state
        long initialMemory = getUsedMemory();
        Instant startTime = Instant.now();
        
        boolean successful = true;
        String errorMessage = null;
        
        try {
            // Execute the operation
            operation.run();
            logger.debug("Data operation '{}' completed successfully", operationName);
            
        } catch (Exception e) {
            successful = false;
            errorMessage = e.getMessage();
            logger.error("Data operation '{}' failed: {}", operationName, e.getMessage(), e);
        }
        
        // Capture final state
        Instant endTime = Instant.now();
        
        // Force garbage collection to get accurate memory reading
        System.gc();
        long finalMemory = getUsedMemory();
        long memoryUsed = Math.max(0, finalMemory - initialMemory);
        
        PerformanceMetrics metrics = PerformanceMetrics.builder(operationName)
                .startTime(startTime)
                .endTime(endTime)
                .memoryUsed(memoryUsed)
                .recordsProcessed(recordCount)
                .bytesProcessed(byteCount)
                .successful(successful)
                .error(errorMessage)
                .build();
        
        logger.info("Data operation benchmark completed: {}", metrics);
        return metrics;
    }
    
    /**
     * Gets the current memory usage in bytes.
     * 
     * @return current memory usage in bytes
     */
    private long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * Logs system memory information for debugging purposes.
     */
    public void logMemoryInfo() {
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        logger.info("Memory Info - Used: {:.2f} MB, Free: {:.2f} MB, Total: {:.2f} MB, Max: {:.2f} MB",
                   usedMemory / (1024.0 * 1024.0),
                   freeMemory / (1024.0 * 1024.0),
                   totalMemory / (1024.0 * 1024.0),
                   maxMemory / (1024.0 * 1024.0));
    }
}
