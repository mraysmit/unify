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

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.integration.async.AsyncDataProcessor;
import dev.mars.jtable.integration.performance.PerformanceMetrics;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for asynchronous data processing functionality.
 */
public class AsyncDataProcessingTest {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDataProcessingTest.class);
    
    private AsyncDataProcessor asyncProcessor;
    private List<String> testFiles;
    
    @BeforeEach
    public void setUp() {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        asyncProcessor = new AsyncDataProcessor(executor);
        testFiles = new ArrayList<>();
    }
    
    @AfterEach
    public void tearDown() throws InterruptedException {
        // Clean up test files
        testFiles.forEach(file -> {
            try {
                Files.deleteIfExists(Paths.get(file));
            } catch (IOException e) {
                logger.warn("Failed to cleanup test file: {}", file, e);
            }
        });
        
        // Clean up database files
        try {
            Files.deleteIfExists(Paths.get("test_async.db"));
        } catch (IOException e) {
            logger.warn("Failed to cleanup test database", e);
        }
        
        // Shutdown async processor
        asyncProcessor.shutdown(10);
    }
    
    @Test
    public void testAsyncCSVReading() throws IOException {
        logger.info("Testing async CSV reading");
        
        // Create test CSV file
        String csvFile = createTestCSVFile("test_async_read.csv", 50);
        
        // Create mapping configuration
        MappingConfiguration config = createCSVMappingConfiguration(csvFile);
        
        // Read CSV asynchronously
        CompletableFuture<ITable> future = asyncProcessor.readCSVAsync(csvFile, config);
        
        // Verify result
        ITable table = future.join();
        assertNotNull(table, "Table should not be null");
        assertEquals(50, table.getRowCount(), "Should have read 50 rows");
        assertEquals(5, table.getColumnCount(), "Should have 5 columns");
        
        logger.info("Async CSV reading test completed successfully");
    }
    
    @Test
    public void testConcurrentFileProcessing() throws IOException {
        logger.info("Testing concurrent file processing");
        
        // Create multiple test files
        List<String> csvFiles = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String file = createTestCSVFile("test_concurrent_" + i + ".csv", 30 + (i * 10));
            csvFiles.add(file);
        }
        
        // Process files concurrently
        CompletableFuture<List<PerformanceMetrics>> future = asyncProcessor.processMultipleFilesAsync(
            csvFiles, 
            this::createMappingConfigurations
        );
        
        // Verify results
        List<PerformanceMetrics> results = future.join();
        assertEquals(3, results.size(), "Should have 3 processing results");
        
        // Verify all operations were successful
        results.forEach(metrics -> {
            assertTrue(metrics.isSuccessful(), "All operations should be successful");
            assertTrue(metrics.getRecordsProcessed() > 0, "Should have processed records");
            assertTrue(metrics.getDurationMillis() > 0, "Should have measurable duration");
        });
        
        logger.info("Concurrent file processing test completed successfully");
    }
    
    @Test
    public void testPipelineProcessing() throws IOException {
        logger.info("Testing pipeline processing with batching");
        
        // Create test files
        List<String> csvFiles = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            String file = createTestCSVFile("test_pipeline_" + i + ".csv", 20);
            csvFiles.add(file);
        }
        
        // Process with pipeline (batch size of 2)
        CompletableFuture<List<PerformanceMetrics>> future = asyncProcessor.createProcessingPipeline(
            csvFiles, 
            2, // batch size
            this::createMappingConfigurations
        );
        
        // Verify results
        List<PerformanceMetrics> results = future.join();
        assertEquals(5, results.size(), "Should have 5 processing results");
        
        // Verify all operations were successful
        results.forEach(metrics -> {
            assertTrue(metrics.isSuccessful(), "All operations should be successful");
            assertEquals(20, metrics.getRecordsProcessed(), "Should have processed 20 records each");
        });
        
        logger.info("Pipeline processing test completed successfully");
    }
    
    @Test
    public void testErrorHandling() throws IOException {
        logger.info("Testing error handling in async processing");
        
        // Create mix of valid and invalid files
        List<String> csvFiles = new ArrayList<>();
        csvFiles.add(createTestCSVFile("test_error_valid.csv", 25));
        csvFiles.add("non_existent_file.csv"); // This will cause an error
        csvFiles.add(createTestCSVFile("test_error_valid2.csv", 30));
        
        // Process with error handling
        CompletableFuture<AsyncDataProcessor.AsyncProcessingResult> future = 
            asyncProcessor.processWithErrorHandling(csvFiles, this::createMappingConfigurations);
        
        // Verify results
        AsyncDataProcessor.AsyncProcessingResult result = future.join();
        assertEquals(3, result.getTotalCount(), "Should have 3 total files");
        assertEquals(2, result.getSuccessfulCount(), "Should have 2 successful files");
        assertEquals(1, result.getFailedCount(), "Should have 1 failed file");
        
        // Verify successful results
        List<AsyncDataProcessor.FileProcessingResult> successfulResults = result.getSuccessfulResults();
        assertEquals(2, successfulResults.size(), "Should have 2 successful results");
        successfulResults.forEach(fileResult -> {
            assertTrue(fileResult.isSuccessful(), "Successful results should be marked as successful");
            assertNotNull(fileResult.getMetrics(), "Successful results should have metrics");
            assertNull(fileResult.getErrorMessage(), "Successful results should not have error message");
        });
        
        // Verify failed results
        List<AsyncDataProcessor.FileProcessingResult> failedResults = result.getFailedResults();
        assertEquals(1, failedResults.size(), "Should have 1 failed result");
        AsyncDataProcessor.FileProcessingResult failedResult = failedResults.get(0);
        assertFalse(failedResult.isSuccessful(), "Failed result should be marked as unsuccessful");
        assertNull(failedResult.getMetrics(), "Failed result should not have metrics");
        assertNotNull(failedResult.getErrorMessage(), "Failed result should have error message");
        assertEquals("non_existent_file.csv", failedResult.getFileName(), "Failed file should be the non-existent one");
        
        logger.info("Error handling test completed successfully");
    }
    
    @Test
    public void testPerformanceMetrics() throws IOException {
        logger.info("Testing performance metrics collection");
        
        // Create test file
        String csvFile = createTestCSVFile("test_performance.csv", 100);
        
        // Process file and collect metrics
        CompletableFuture<List<PerformanceMetrics>> future = asyncProcessor.processMultipleFilesAsync(
            List.of(csvFile), 
            this::createMappingConfigurations
        );
        
        List<PerformanceMetrics> results = future.join();
        assertEquals(1, results.size(), "Should have 1 result");
        
        PerformanceMetrics metrics = results.get(0);
        assertTrue(metrics.isSuccessful(), "Operation should be successful");
        assertEquals(100, metrics.getRecordsProcessed(), "Should have processed 100 records");
        assertTrue(metrics.getDurationMillis() > 0, "Should have measurable duration");
        assertTrue(metrics.getRecordsPerSecond() > 0, "Should have positive records per second");
        assertTrue(metrics.getMemoryUsageMB() >= 0, "Memory usage should be non-negative");
        
        logger.info("Performance metrics: {}", metrics.toString());
        logger.info("Performance metrics test completed successfully");
    }
    
    private String createTestCSVFile(String filename, int recordCount) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("id,name,email,age,salary\n");
        
        for (int i = 1; i <= recordCount; i++) {
            content.append(String.format("%d,User%d,user%d@example.com,%d,%.2f\n",
                          i, i, i, 20 + (i % 50), 30000.0 + (i % 70000)));
        }
        
        Files.write(Paths.get(filename), content.toString().getBytes());
        testFiles.add(filename); // Track for cleanup
        logger.debug("Created test CSV file: {} with {} records", filename, recordCount);
        return filename;
    }
    
    private MappingConfiguration createCSVMappingConfiguration(String csvFile) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(csvFile);
        
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("id", "id", "int"));
        mappings.add(new ColumnMapping("name", "name", "string"));
        mappings.add(new ColumnMapping("email", "email", "string"));
        mappings.add(new ColumnMapping("age", "age", "int"));
        mappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(mappings);
        
        Map<String, Object> options = new HashMap<>();
        options.put("hasHeaderRow", true);
        options.put("delimiter", ",");
        config.setOptions(options);
        
        return config;
    }
    
    private MappingConfiguration[] createMappingConfigurations(String csvFile) {
        // CSV configuration
        MappingConfiguration csvConfig = createCSVMappingConfiguration(csvFile);
        
        // Database configuration
        MappingConfiguration dbConfig = new MappingConfiguration();
        // One database per file: SQLite allows a single writer per file, and concurrent first
        // opens of a file that does not exist yet can fail with SQLITE_READONLY or SQLITE_BUSY.
        String dbFile = "test_async_" + Paths.get(csvFile).getFileName().toString().replace(".csv", "") + ".db";
        testFiles.add(dbFile);
        dbConfig.setSourceLocation("jdbc:sqlite:" + dbFile);
        dbConfig.setColumnMappings(csvConfig.getColumnMappings());
        
        Map<String, Object> dbOptions = new HashMap<>();
        dbOptions.put("tableName", "test_" + csvFile.replace(".csv", "").replace("-", "_"));
        dbOptions.put("createTable", true);
        dbConfig.setOptions(dbOptions);
        
        return new MappingConfiguration[]{csvConfig, dbConfig};
    }
}
