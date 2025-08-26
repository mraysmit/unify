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

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.config.MappingConfigurationManager;
import dev.mars.jtable.integration.csv.CSVProcessor;
import dev.mars.jtable.integration.db.DbConnectionManager;
import dev.mars.jtable.integration.db.SQLiteProcessor;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.JSONMappingSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of performance benchmarking for data integration operations.
 * This class shows how to use the performance benchmarking utilities to measure
 * and analyze the performance of various data integration scenarios.
 */
public class IntegrationBenchmarkDemo {
    private static final Logger logger = LoggerFactory.getLogger(IntegrationBenchmarkDemo.class);
    
    private final PerformanceBenchmark benchmark;
    private final CSVProcessor csvProcessor;
    private final SQLiteProcessor sqliteProcessor;
    private final DbConnectionManager connectionManager;
    private final MappingConfigurationManager configManager;
    
    public IntegrationBenchmarkDemo() {
        this.benchmark = new PerformanceBenchmark();
        this.csvProcessor = new CSVProcessor();
        this.sqliteProcessor = new SQLiteProcessor();
        this.connectionManager = new DbConnectionManager();
        this.configManager = new MappingConfigurationManager(new JSONMappingSerializer());
    }
    
    /**
     * Main method to run the benchmark demonstration.
     */
    public static void main(String[] args) {
        IntegrationBenchmarkDemo demo = new IntegrationBenchmarkDemo();
        
        try {
            logger.info("Starting Integration Performance Benchmark Demo");
            
            // Run various benchmarks
            demo.benchmarkCSVReading();
            demo.benchmarkDatabaseWriting();
            demo.benchmarkEndToEndIntegration();
            
            logger.info("Integration Performance Benchmark Demo completed");
            
        } catch (Exception e) {
            logger.error("Error during benchmark demo: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Benchmarks CSV reading performance with different file sizes.
     */
    public void benchmarkCSVReading() throws IOException {
        logger.info("=== CSV Reading Performance Benchmark ===");
        
        // Create test CSV files of different sizes
        String smallFile = createTestCSV("small_test.csv", 100);
        String mediumFile = createTestCSV("medium_test.csv", 1000);
        String largeFile = createTestCSV("large_test.csv", 10000);
        
        try {
            // Benchmark small file
            benchmarkCSVFile(smallFile, 100);
            
            // Benchmark medium file
            benchmarkCSVFile(mediumFile, 1000);
            
            // Benchmark large file
            benchmarkCSVFile(largeFile, 10000);
            
        } finally {
            // Cleanup test files
            cleanupFile(smallFile);
            cleanupFile(mediumFile);
            cleanupFile(largeFile);
        }
    }
    
    /**
     * Benchmarks database writing performance.
     */
    public void benchmarkDatabaseWriting() throws SQLException, IOException {
        logger.info("=== Database Writing Performance Benchmark ===");
        
        // Create test data
        ITable testTable = createTestTable(1000);
        
        // Create database connection
        DbConnection connection = connectionManager.createSQLiteConnection(new DatabaseProperties());
        
        try {
            // Create mapping configuration
            MappingConfiguration config = createTestMappingConfiguration(connection);
            
            // Benchmark database writing
            PerformanceMetrics metrics = benchmark.benchmarkDataOperation(
                "SQLite Database Write (1000 records)",
                testTable.getRowCount(),
                estimateTableSizeBytes(testTable),
                () -> {
                    try {
                        sqliteProcessor.writeToSQLiteDatabase(testTable, connection, config);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
            
            logger.info("Database write performance: {:.2f} records/sec, {:.2f} MB/sec",
                       metrics.getRecordsPerSecond(), metrics.getMegabytesPerSecond());
            
        } finally {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        }
    }
    
    /**
     * Benchmarks end-to-end CSV to database integration.
     */
    public void benchmarkEndToEndIntegration() throws IOException, SQLException {
        logger.info("=== End-to-End Integration Performance Benchmark ===");
        
        String csvFile = createTestCSV("integration_test.csv", 5000);
        
        try {
            PerformanceMetrics metrics = benchmark.benchmarkDataOperation(
                "CSV to SQLite Integration (5000 records)",
                5000,
                Files.size(Paths.get(csvFile)),
                () -> {
                    try {
                        performEndToEndIntegration(csvFile);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
            
            logger.info("End-to-end integration performance: {:.2f} records/sec, {:.2f} MB/sec",
                       metrics.getRecordsPerSecond(), metrics.getMegabytesPerSecond());
            
        } finally {
            cleanupFile(csvFile);
        }
    }
    
    private void benchmarkCSVFile(String filename, int expectedRecords) throws IOException {
        ITable table = new TableCore("BenchmarkTable");
        
        // Create mapping configuration
        MappingConfiguration csvConfig = createCSVMappingConfiguration(filename);
        
        long fileSize = Files.size(Paths.get(filename));
        
        PerformanceMetrics metrics = benchmark.benchmarkDataOperation(
            String.format("CSV Read (%d records)", expectedRecords),
            expectedRecords,
            fileSize,
            () -> {
                try {
                    csvProcessor.readFromCSV(table, filename, csvConfig);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        );
        
        logger.info("CSV read performance: {:.2f} records/sec, {:.2f} MB/sec",
                   metrics.getRecordsPerSecond(), metrics.getMegabytesPerSecond());
    }
    
    private void performEndToEndIntegration(String csvFile) throws IOException, SQLException {
        // Create table and read CSV
        ITable table = new TableCore("IntegrationTable");
        MappingConfiguration csvConfig = createCSVMappingConfiguration(csvFile);
        csvProcessor.readFromCSV(table, csvFile, csvConfig);
        
        // Write to database
        DbConnection connection = connectionManager.createSQLiteConnection(new DatabaseProperties());
        try {
            MappingConfiguration dbConfig = createTestMappingConfiguration(connection);
            sqliteProcessor.writeToSQLiteDatabase(table, connection, dbConfig);
        } finally {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        }
    }
    
    private String createTestCSV(String filename, int recordCount) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("id,name,email,age,salary\n");
        
        for (int i = 1; i <= recordCount; i++) {
            content.append(String.format("%d,User%d,user%d@example.com,%d,%.2f\n",
                          i, i, i, 20 + (i % 50), 30000.0 + (i % 70000)));
        }
        
        Files.write(Paths.get(filename), content.toString().getBytes());
        logger.debug("Created test CSV file: {} with {} records", filename, recordCount);
        return filename;
    }
    
    private ITable createTestTable(int recordCount) {
        ITable table = new TableCore("TestTable");
        
        // Add some sample data
        for (int i = 1; i <= recordCount; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("id", String.valueOf(i));
            row.put("name", "User" + i);
            row.put("email", "user" + i + "@example.com");
            row.put("age", String.valueOf(20 + (i % 50)));
            row.put("salary", String.format("%.2f", 30000.0 + (i % 70000)));
            table.addRow(row);
        }
        
        return table;
    }
    
    private MappingConfiguration createCSVMappingConfiguration(String filename) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(filename);
        
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("id", "id", "int"));
        mappings.add(new ColumnMapping("name", "name", "string"));
        mappings.add(new ColumnMapping("email", "email", "string"));
        mappings.add(new ColumnMapping("age", "age", "int"));
        mappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(mappings);
        
        Map<String, Object> options = new HashMap<>();
        options.put("hasHeader", true);
        options.put("delimiter", ",");
        config.setOptions(options);
        
        return config;
    }
    
    private MappingConfiguration createTestMappingConfiguration(DbConnection connection) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(connection.getConnectionString());
        
        List<ColumnMapping> mappings = new ArrayList<>();
        mappings.add(new ColumnMapping("id", "id", "int"));
        mappings.add(new ColumnMapping("name", "name", "string"));
        mappings.add(new ColumnMapping("email", "email", "string"));
        mappings.add(new ColumnMapping("age", "age", "int"));
        mappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(mappings);
        
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", "benchmark_test_" + System.currentTimeMillis());
        options.put("createTable", true);
        config.setOptions(options);
        
        return config;
    }
    
    private long estimateTableSizeBytes(ITable table) {
        // Rough estimation: assume average of 50 bytes per cell
        return table.getRowCount() * table.getColumnCount() * 50L;
    }
    
    private void cleanupFile(String filename) {
        try {
            Files.deleteIfExists(Paths.get(filename));
            logger.debug("Cleaned up test file: {}", filename);
        } catch (IOException e) {
            logger.warn("Failed to cleanup test file: {}", filename, e);
        }
    }
}
