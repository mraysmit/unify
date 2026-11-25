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
package dev.mars.jtable.integration.async;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.csv.CSVProcessor;
import dev.mars.jtable.integration.db.DbConnectionManager;
import dev.mars.jtable.integration.db.SQLiteProcessor;
import dev.mars.jtable.integration.performance.PerformanceBenchmark;
import dev.mars.jtable.integration.performance.PerformanceMetrics;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Asynchronous data processor that demonstrates concurrent data integration patterns.
 * This class shows how to process multiple data sources concurrently using CompletableFuture
 * and reactive programming patterns.
 */
public class AsyncDataProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AsyncDataProcessor.class);
    
    private final ExecutorService executorService;
    private final CSVProcessor csvProcessor;
    private final SQLiteProcessor sqliteProcessor;
    private final DbConnectionManager connectionManager;
    private final PerformanceBenchmark benchmark;
    
    public AsyncDataProcessor() {
        this(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }
    
    public AsyncDataProcessor(ExecutorService executorService) {
        this.executorService = executorService;
        this.csvProcessor = new CSVProcessor();
        this.sqliteProcessor = new SQLiteProcessor();
        this.connectionManager = new DbConnectionManager();
        this.benchmark = new PerformanceBenchmark();
    }
    
    /**
     * Asynchronously reads data from a CSV file.
     * 
     * @param csvFile the CSV file to read
     * @param config the mapping configuration
     * @return a CompletableFuture containing the loaded table
     */
    public CompletableFuture<ITable> readCSVAsync(String csvFile, MappingConfiguration config) {
        return CompletableFuture.supplyAsync(() -> readCSV(csvFile, config), executorService);
    }

    private ITable readCSV(String csvFile, MappingConfiguration config) {
        logger.info("Starting CSV read: {}", csvFile);

        ITable table = new TableCore("AsyncCSV-" + System.currentTimeMillis());

        try {
            csvProcessor.readFromCSV(table, csvFile, config);
            logger.info("Completed CSV read: {} rows from {}", table.getRowCount(), csvFile);
            return table;
        } catch (IOException e) {
            logger.error("Error reading CSV file {}: {}", csvFile, e.getMessage(), e);
            throw new RuntimeException("Failed to read CSV file: " + csvFile, e);
        }
    }
    
    /**
     * Asynchronously writes data to a database.
     * 
     * @param table the table to write
     * @param config the mapping configuration
     * @return a CompletableFuture indicating completion
     */
    public CompletableFuture<Void> writeDatabaseAsync(ITable table, MappingConfiguration config) {
        return CompletableFuture.runAsync(() -> writeDatabase(table, config), executorService);
    }

    private void writeDatabase(ITable table, MappingConfiguration config) {
        logger.info("Starting database write for table: {}", table.getName());

        try {
            // Connect to the database the mapping writes to; db.properties is only the fallback.
            String url = config.getSourceLocation();
            DbConnection connection = url != null
                    ? connectionManager.createConnection(url,
                            (String) config.getOption("username", ""), (String) config.getOption("password", ""))
                    : connectionManager.createSQLiteConnection(new DatabaseProperties());
            try {
                sqliteProcessor.writeToSQLiteDatabase(table, connection, config);
                logger.info("Completed database write: {} rows from {}",
                           table.getRowCount(), table.getName());
            } finally {
                if (connection.isConnected()) {
                    connection.disconnect();
                }
            }
        } catch (SQLException | IOException e) {
            logger.error("Error writing to database for table {}: {}", table.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to write to database", e);
        }
    }
    
    /**
     * Processes multiple CSV files concurrently and writes them to separate database tables.
     * 
     * @param csvFiles list of CSV files to process
     * @param configFactory function to create mapping configurations for each file
     * @return a CompletableFuture containing performance metrics for all operations
     */
    public CompletableFuture<List<PerformanceMetrics>> processMultipleFilesAsync(
            List<String> csvFiles, 
            Function<String, MappingConfiguration[]> configFactory) {
        
        logger.info("Starting async processing of {} CSV files", csvFiles.size());
        
        List<CompletableFuture<PerformanceMetrics>> futures = csvFiles.stream()
            .map(csvFile -> processFileAsync(csvFile, configFactory.apply(csvFile)))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    /**
     * Processes a single file asynchronously with performance monitoring. The read and the write
     * run in sequence inside one pool task: a pool task must never block waiting for another task
     * on the same pool, because with every thread waiting, the queued work can never start.
     *
     * @param csvFile the CSV file to process
     * @param configs array containing [csvConfig, dbConfig]
     * @return a CompletableFuture containing performance metrics
     */
    private CompletableFuture<PerformanceMetrics> processFileAsync(String csvFile, MappingConfiguration[] configs) {
        return CompletableFuture.supplyAsync(() -> {
            String operationName = "Async File Processing: " + csvFile;

            long[] records = {0};
            PerformanceMetrics metrics = benchmark.benchmark(operationName, () -> {
                ITable table = readCSV(csvFile, configs[0]);
                writeDatabase(table, configs[1]);
                records[0] = table.getRowCount();
                logger.info("Completed async processing of file: {}", csvFile);
            });
            return metrics.withRecordsProcessed(records[0]);
        }, executorService);
    }
    
    /**
     * Creates a pipeline for streaming data processing with backpressure handling.
     * 
     * @param csvFiles list of CSV files to process
     * @param batchSize number of files to process concurrently
     * @param configFactory function to create mapping configurations
     * @return a CompletableFuture containing all performance metrics
     */
    public CompletableFuture<List<PerformanceMetrics>> createProcessingPipeline(
            List<String> csvFiles, 
            int batchSize,
            Function<String, MappingConfiguration[]> configFactory) {
        
        logger.info("Creating processing pipeline for {} files with batch size {}", csvFiles.size(), batchSize);
        
        List<CompletableFuture<PerformanceMetrics>> allFutures = new ArrayList<>();
        
        // Process files in batches to control resource usage
        for (int i = 0; i < csvFiles.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, csvFiles.size());
            List<String> batch = csvFiles.subList(i, endIndex);
            
            logger.debug("Processing batch {}-{} of {}", i + 1, endIndex, csvFiles.size());
            
            List<CompletableFuture<PerformanceMetrics>> batchFutures = batch.stream()
                .map(csvFile -> processFileAsync(csvFile, configFactory.apply(csvFile)))
                .collect(Collectors.toList());
            
            allFutures.addAll(batchFutures);
            
            // Wait for current batch to complete before starting next batch
            if (i + batchSize < csvFiles.size()) {
                CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();
                logger.debug("Completed batch {}-{}", i + 1, endIndex);
            }
        }
        
        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> allFutures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }
    
    /**
     * Demonstrates error handling and recovery in async processing.
     * 
     * @param csvFiles list of CSV files to process (some may be invalid)
     * @param configFactory function to create mapping configurations
     * @return a CompletableFuture containing results with error handling
     */
    public CompletableFuture<AsyncProcessingResult> processWithErrorHandling(
            List<String> csvFiles,
            Function<String, MappingConfiguration[]> configFactory) {
        
        logger.info("Starting error-resilient async processing of {} files", csvFiles.size());
        
        List<CompletableFuture<FileProcessingResult>> futures = csvFiles.stream()
            .map(csvFile -> processFileWithErrorHandling(csvFile, configFactory.apply(csvFile)))
            .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<FileProcessingResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
                
                return new AsyncProcessingResult(results);
            });
    }
    
    /**
     * Processes a single file with comprehensive error handling.
     */
    private CompletableFuture<FileProcessingResult> processFileWithErrorHandling(
            String csvFile, MappingConfiguration[] configs) {
        
        // Compose rather than join: joining here would hold a pool thread while waiting for
        // processFileAsync's task, which needs a pool thread of its own.
        return processFileAsync(csvFile, configs).handle((metrics, failure) -> {
            if (failure == null && metrics.isSuccessful()) {
                return new FileProcessingResult(csvFile, true, metrics, null);
            }
            String error = failure != null ? failure.getMessage() : metrics.getErrorMessage();
            if (error == null) {
                error = "Processing failed: " + csvFile;
            }
            logger.warn("Failed to process file {}: {}", csvFile, error);
            return new FileProcessingResult(csvFile, false, null, error);
        });
    }
    
    /**
     * Shuts down the async processor and waits for completion.
     * 
     * @param timeoutSeconds maximum time to wait for shutdown
     * @throws InterruptedException if interrupted while waiting
     */
    public void shutdown(long timeoutSeconds) throws InterruptedException {
        logger.info("Shutting down async data processor...");
        
        executorService.shutdown();
        
        if (!executorService.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
            logger.warn("Executor did not terminate within {} seconds, forcing shutdown", timeoutSeconds);
            executorService.shutdownNow();
            
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.error("Executor did not terminate after forced shutdown");
            }
        }
        
        logger.info("Async data processor shutdown completed");
    }
    
    /**
     * Result of processing a single file.
     */
    public static class FileProcessingResult {
        private final String fileName;
        private final boolean successful;
        private final PerformanceMetrics metrics;
        private final String errorMessage;
        
        public FileProcessingResult(String fileName, boolean successful, 
                                  PerformanceMetrics metrics, String errorMessage) {
            this.fileName = fileName;
            this.successful = successful;
            this.metrics = metrics;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public String getFileName() { return fileName; }
        public boolean isSuccessful() { return successful; }
        public PerformanceMetrics getMetrics() { return metrics; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Result of async processing operation containing all file results.
     */
    public static class AsyncProcessingResult {
        private final List<FileProcessingResult> results;
        private final long successfulCount;
        private final long failedCount;
        
        public AsyncProcessingResult(List<FileProcessingResult> results) {
            this.results = results;
            this.successfulCount = results.stream().mapToLong(r -> r.isSuccessful() ? 1 : 0).sum();
            this.failedCount = results.size() - successfulCount;
        }
        
        // Getters
        public List<FileProcessingResult> getResults() { return results; }
        public long getSuccessfulCount() { return successfulCount; }
        public long getFailedCount() { return failedCount; }
        public long getTotalCount() { return results.size(); }
        
        public List<FileProcessingResult> getSuccessfulResults() {
            return results.stream().filter(FileProcessingResult::isSuccessful).collect(Collectors.toList());
        }
        
        public List<FileProcessingResult> getFailedResults() {
            return results.stream().filter(r -> !r.isSuccessful()).collect(Collectors.toList());
        }
    }
}
