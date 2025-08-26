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
package dev.mars.jtable.integration;

import dev.mars.jtable.integration.async.AsyncDataProcessor;
import dev.mars.jtable.integration.performance.PerformanceMetrics;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demonstration of asynchronous data processing patterns using the Unify framework.
 * This class showcases various async processing scenarios including:
 * 1. Concurrent file processing
 * 2. Pipeline processing with backpressure
 * 3. Error handling and recovery
 * 4. Performance monitoring of async operations
 */
public class AsyncDataProcessingDemo {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDataProcessingDemo.class);
    
    private final AsyncDataProcessor asyncProcessor;
    
    public AsyncDataProcessingDemo() {
        // Create a custom executor with optimal thread pool size
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
        );
        this.asyncProcessor = new AsyncDataProcessor(executor);
    }
    
    /**
     * Main method to run the async data processing demonstration.
     */
    public static void main(String[] args) {
        AsyncDataProcessingDemo demo = new AsyncDataProcessingDemo();
        
        try {
            logger.info("Starting Async Data Processing Demo");
            
            // Run various async processing demonstrations
            demo.demonstrateConcurrentProcessing();
            demo.demonstratePipelineProcessing();
            demo.demonstrateErrorHandling();
            demo.demonstratePerformanceComparison();
            
            logger.info("Async Data Processing Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during async processing demo: {}", e.getMessage(), e);
        } finally {
            try {
                demo.shutdown();
            } catch (InterruptedException e) {
                logger.warn("Interrupted during shutdown", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Demonstrates concurrent processing of multiple files.
     */
    public void demonstrateConcurrentProcessing() throws IOException {
        logger.info("=== Concurrent Processing Demonstration ===");
        
        // Create test files
        List<String> csvFiles = createTestFiles(5, "concurrent_test_");
        
        try {
            // Process files concurrently
            CompletableFuture<List<PerformanceMetrics>> future = asyncProcessor.processMultipleFilesAsync(
                csvFiles, 
                this::createMappingConfigurations
            );
            
            // Wait for completion and analyze results
            List<PerformanceMetrics> results = future.join();
            
            logger.info("Concurrent processing completed:");
            double totalTime = results.stream().mapToDouble(PerformanceMetrics::getDurationSeconds).sum();
            double avgRecordsPerSec = results.stream().mapToDouble(PerformanceMetrics::getRecordsPerSecond).average().orElse(0);
            
            logger.info("- Processed {} files", results.size());
            logger.info("- Total processing time: {:.2f} seconds", totalTime);
            logger.info("- Average records/sec: {:.2f}", avgRecordsPerSec);
            
            results.forEach(metrics -> 
                logger.debug("File processing: {}", metrics.toString())
            );
            
        } finally {
            cleanupFiles(csvFiles);
        }
    }
    
    /**
     * Demonstrates pipeline processing with controlled batching.
     */
    public void demonstratePipelineProcessing() throws IOException {
        logger.info("=== Pipeline Processing Demonstration ===");
        
        // Create more test files to demonstrate batching
        List<String> csvFiles = createTestFiles(10, "pipeline_test_");
        
        try {
            // Process with pipeline (batch size of 3)
            CompletableFuture<List<PerformanceMetrics>> future = asyncProcessor.createProcessingPipeline(
                csvFiles, 
                3, // batch size
                this::createMappingConfigurations
            );
            
            List<PerformanceMetrics> results = future.join();
            
            logger.info("Pipeline processing completed:");
            logger.info("- Processed {} files in batches of 3", results.size());
            
            // Analyze batch performance
            for (int i = 0; i < results.size(); i += 3) {
                int batchEnd = Math.min(i + 3, results.size());
                List<PerformanceMetrics> batch = results.subList(i, batchEnd);
                
                double batchTotalTime = batch.stream().mapToDouble(PerformanceMetrics::getDurationSeconds).sum();
                double batchAvgTime = batchTotalTime / batch.size();
                
                logger.info("Batch {}: {} files, avg time: {:.2f}s", 
                           (i / 3) + 1, batch.size(), batchAvgTime);
            }
            
        } finally {
            cleanupFiles(csvFiles);
        }
    }
    
    /**
     * Demonstrates error handling and recovery in async processing.
     */
    public void demonstrateErrorHandling() throws IOException {
        logger.info("=== Error Handling Demonstration ===");
        
        // Create mix of valid and invalid files
        List<String> csvFiles = new ArrayList<>();
        csvFiles.addAll(createTestFiles(3, "valid_test_"));
        csvFiles.add("non_existent_file.csv"); // This will cause an error
        csvFiles.add(""); // This will also cause an error
        
        try {
            CompletableFuture<AsyncDataProcessor.AsyncProcessingResult> future = 
                asyncProcessor.processWithErrorHandling(csvFiles, this::createMappingConfigurations);
            
            AsyncDataProcessor.AsyncProcessingResult result = future.join();
            
            logger.info("Error handling processing completed:");
            logger.info("- Total files: {}", result.getTotalCount());
            logger.info("- Successful: {}", result.getSuccessfulCount());
            logger.info("- Failed: {}", result.getFailedCount());
            
            // Log successful results
            result.getSuccessfulResults().forEach(fileResult -> 
                logger.info("SUCCESS: {} - {}", fileResult.getFileName(), 
                           fileResult.getMetrics().toString())
            );
            
            // Log failed results
            result.getFailedResults().forEach(fileResult -> 
                logger.warn("FAILED: {} - {}", fileResult.getFileName(), fileResult.getErrorMessage())
            );
            
        } finally {
            // Clean up only the valid files (invalid ones don't exist)
            cleanupFiles(csvFiles.stream()
                .filter(file -> file.startsWith("valid_test_"))
                .toList());
        }
    }
    
    /**
     * Demonstrates performance comparison between sync and async processing.
     */
    public void demonstratePerformanceComparison() throws IOException {
        logger.info("=== Performance Comparison Demonstration ===");
        
        List<String> csvFiles = createTestFiles(4, "perf_test_");
        
        try {
            // Measure async processing time
            long asyncStart = System.currentTimeMillis();
            CompletableFuture<List<PerformanceMetrics>> asyncFuture = asyncProcessor.processMultipleFilesAsync(
                csvFiles, 
                this::createMappingConfigurations
            );
            List<PerformanceMetrics> asyncResults = asyncFuture.join();
            long asyncEnd = System.currentTimeMillis();
            
            double asyncTotalTime = (asyncEnd - asyncStart) / 1000.0;
            double asyncProcessingTime = asyncResults.stream()
                .mapToDouble(PerformanceMetrics::getDurationSeconds).sum();
            
            logger.info("Performance Comparison Results:");
            logger.info("- Async wall clock time: {:.2f} seconds", asyncTotalTime);
            logger.info("- Async total processing time: {:.2f} seconds", asyncProcessingTime);
            logger.info("- Concurrency benefit: {:.2f}x speedup", asyncProcessingTime / asyncTotalTime);
            logger.info("- Average records/sec (async): {:.2f}", 
                       asyncResults.stream().mapToDouble(PerformanceMetrics::getRecordsPerSecond).average().orElse(0));
            
        } finally {
            cleanupFiles(csvFiles);
        }
    }
    
    /**
     * Creates test CSV files with sample data.
     */
    private List<String> createTestFiles(int count, String prefix) throws IOException {
        List<String> files = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            String filename = prefix + i + ".csv";
            StringBuilder content = new StringBuilder();
            content.append("id,name,email,age,salary\n");
            
            // Create different amounts of data for each file
            int recordCount = 100 + (i * 50);
            for (int j = 1; j <= recordCount; j++) {
                content.append(String.format("%d,User%d_%d,user%d_%d@example.com,%d,%.2f\n",
                              j, i, j, i, j, 20 + (j % 50), 30000.0 + (j % 70000)));
            }
            
            Files.write(Paths.get(filename), content.toString().getBytes());
            files.add(filename);
            logger.debug("Created test file: {} with {} records", filename, recordCount);
        }
        
        return files;
    }
    
    /**
     * Creates mapping configurations for CSV and database operations.
     */
    private MappingConfiguration[] createMappingConfigurations(String csvFile) {
        // CSV mapping configuration
        MappingConfiguration csvConfig = new MappingConfiguration();
        csvConfig.setSourceLocation(csvFile);
        
        List<ColumnMapping> csvMappings = new ArrayList<>();
        csvMappings.add(new ColumnMapping("id", "id", "int"));
        csvMappings.add(new ColumnMapping("name", "name", "string"));
        csvMappings.add(new ColumnMapping("email", "email", "string"));
        csvMappings.add(new ColumnMapping("age", "age", "int"));
        csvMappings.add(new ColumnMapping("salary", "salary", "double"));
        csvConfig.setColumnMappings(csvMappings);
        
        Map<String, Object> csvOptions = new HashMap<>();
        csvOptions.put("hasHeader", true);
        csvOptions.put("delimiter", ",");
        csvConfig.setOptions(csvOptions);
        
        // Database mapping configuration
        MappingConfiguration dbConfig = new MappingConfiguration();
        dbConfig.setSourceLocation("jdbc:sqlite:async_demo.db");
        dbConfig.setColumnMappings(csvMappings); // Same mappings
        
        Map<String, Object> dbOptions = new HashMap<>();
        dbOptions.put("tableName", "async_" + csvFile.replace(".csv", "").replace("-", "_"));
        dbOptions.put("createTable", true);
        dbConfig.setOptions(dbOptions);
        
        return new MappingConfiguration[]{csvConfig, dbConfig};
    }
    
    /**
     * Cleans up test files.
     */
    private void cleanupFiles(List<String> files) {
        files.forEach(file -> {
            try {
                Files.deleteIfExists(Paths.get(file));
                logger.debug("Cleaned up test file: {}", file);
            } catch (IOException e) {
                logger.warn("Failed to cleanup test file: {}", file, e);
            }
        });
        
        // Also cleanup the demo database
        try {
            Files.deleteIfExists(Paths.get("async_demo.db"));
        } catch (IOException e) {
            logger.warn("Failed to cleanup demo database", e);
        }
    }
    
    /**
     * Shuts down the async processor.
     */
    public void shutdown() throws InterruptedException {
        asyncProcessor.shutdown(30);
    }
}
