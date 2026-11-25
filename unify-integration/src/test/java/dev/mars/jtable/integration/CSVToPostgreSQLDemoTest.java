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
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.config.MappingConfigurationManager;
import dev.mars.jtable.integration.csv.CSVProcessor;
import dev.mars.jtable.integration.db.DbConnectionManager;
import dev.mars.jtable.integration.db.PostgreSQLProcessor;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.JSONMappingSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for CSV to PostgreSQL database operations.
 * 
 * This test requires a running PostgreSQL instance. It can be enabled by setting
 * the system property: -Dtest.postgresql.enabled=true
 * 
 * Make sure to configure the PostgreSQL connection details in db.properties
 * before running this test.
 */
@EnabledIfSystemProperty(named = "test.postgresql.enabled", matches = "true")
public class CSVToPostgreSQLDemoTest {
    private static final Logger logger = LoggerFactory.getLogger(CSVToPostgreSQLDemoTest.class);

    private CSVProcessor csvProcessor;
    private PostgreSQLProcessor postgreSQLProcessor;
    private DbConnectionManager connectionManager;
    private MappingConfigurationManager configManager;
    private DbConnection dbConnection;
    private String testTableName;

    @BeforeEach
    public void setUp() throws SQLException {
        logger.info("Setting up PostgreSQL integration test");
        
        csvProcessor = new CSVProcessor();
        postgreSQLProcessor = new PostgreSQLProcessor();
        connectionManager = new DbConnectionManager();
        configManager = new MappingConfigurationManager(new JSONMappingSerializer());
        
        // Create unique table name for this test
        testTableName = "test_employees_" + System.currentTimeMillis();
        
        // Create database connection
        try {
            dbConnection = connectionManager.createPostgreSQLConnection(new DatabaseProperties());
            logger.info("Successfully connected to PostgreSQL database");
        } catch (SQLException e) {
            logger.error("Failed to connect to PostgreSQL database. Make sure PostgreSQL is running and configured correctly.", e);
            throw e;
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        logger.info("Cleaning up PostgreSQL integration test");
        
        // Clean up test table
        if (dbConnection != null && dbConnection.isConnected()) {
            try {
                if (postgreSQLProcessor.tableExists(dbConnection, testTableName)) {
                    postgreSQLProcessor.executeQuery(dbConnection, "DROP TABLE " + testTableName);
                    logger.info("Dropped test table: {}", testTableName);
                }
            } catch (SQLException e) {
                logger.warn("Failed to drop test table: {}", testTableName, e);
            }
            
            // Disconnect from database
            dbConnection.disconnect();
            logger.info("Disconnected from PostgreSQL database");
        }
        
        // Clean up test files
        cleanupTestFiles();
    }

    @Test
    public void testCSVToPostgreSQLIntegration() throws Exception {
        logger.info("Starting CSV to PostgreSQL integration test");
        
        // Create a table to hold the data
        ITable table = new TableCore("CSVToPostgreSQLTest-InputTable");
        
        // Create test CSV file
        String csvFile = createTestCSVFile();
        
        try {
            // Step 1: Create CSV mapping configuration
            MappingConfiguration csvConfig = createCSVMappingConfiguration(csvFile);
            
            // Step 2: Read data from CSV
            csvProcessor.readFromCSV(table, csvFile, csvConfig);
            logger.info("Read {} rows from test CSV file", table.getRowCount());
            
            // Verify CSV data was read correctly
            assertTrue(table.getRowCount() > 0, "Should have read data from CSV");
            assertEquals(5, table.getColumnCount(), "Should have 5 columns");
            
            // Step 3: Create database mapping configuration
            MappingConfiguration dbConfig = createDatabaseMappingConfiguration();
            
            // Step 4: Write data to PostgreSQL database
            postgreSQLProcessor.writeToPostgreSQLDatabase(table, dbConnection, dbConfig);
            logger.info("Successfully wrote {} rows to PostgreSQL database", table.getRowCount());
            
            // Step 5: Verify data was written correctly
            verifyDatabaseData(table.getRowCount(), dbConfig);
            
            // Step 6: Test reading data back from database
            testReadingFromDatabase(dbConfig);
            
            logger.info("CSV to PostgreSQL integration test completed successfully");
            
        } finally {
            // Clean up test CSV file
            Files.deleteIfExists(Paths.get(csvFile));
        }
    }

    private String createTestCSVFile() throws IOException {
        String csvFile = "test_postgresql_data.csv";
        StringBuilder content = new StringBuilder();
        content.append("id,name,email,age,salary\n");
        content.append("1,John Doe,john.doe@example.com,30,50000.00\n");
        content.append("2,Jane Smith,jane.smith@example.com,25,45000.00\n");
        content.append("3,Bob Johnson,bob.johnson@example.com,35,60000.00\n");
        content.append("4,Alice Brown,alice.brown@example.com,28,52000.00\n");
        content.append("5,Charlie Wilson,charlie.wilson@example.com,32,55000.00\n");
        
        Files.write(Paths.get(csvFile), content.toString().getBytes());
        logger.debug("Created test CSV file: {}", csvFile);
        return csvFile;
    }

    private MappingConfiguration createCSVMappingConfiguration(String csvFile) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(csvFile);

        List<ColumnMapping> columnMappings = new ArrayList<>();
        columnMappings.add(new ColumnMapping("id", "id", "int"));
        columnMappings.add(new ColumnMapping("name", "name", "string"));
        columnMappings.add(new ColumnMapping("email", "email", "string"));
        columnMappings.add(new ColumnMapping("age", "age", "int"));
        columnMappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(columnMappings);

        Map<String, Object> options = new HashMap<>();
        options.put("hasHeaderRow", true);
        options.put("delimiter", ",");
        config.setOptions(options);

        return config;
    }

    private MappingConfiguration createDatabaseMappingConfiguration() {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(dbConnection.getConnectionString());

        List<ColumnMapping> columnMappings = new ArrayList<>();
        columnMappings.add(new ColumnMapping("id", "id", "int"));
        columnMappings.add(new ColumnMapping("name", "name", "string"));
        columnMappings.add(new ColumnMapping("email", "email", "string"));
        columnMappings.add(new ColumnMapping("age", "age", "int"));
        columnMappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(columnMappings);

        Map<String, Object> options = new HashMap<>();
        options.put("tableName", testTableName);
        options.put("createTable", true);
        config.setOptions(options);

        return config;
    }

    private void verifyDatabaseData(int expectedRowCount, MappingConfiguration config) throws SQLException {
        // Check if table exists
        assertTrue(postgreSQLProcessor.tableExists(dbConnection, testTableName), 
                  "PostgreSQL table should exist after writing data");
        
        logger.info("Verified that PostgreSQL table '{}' exists", testTableName);
    }

    private void testReadingFromDatabase(MappingConfiguration originalConfig) throws SQLException {
        // Create a new table to read data back
        ITable readTable = new TableCore("ReadBackTable");
        
        // Create read configuration
        MappingConfiguration readConfig = new MappingConfiguration();
        readConfig.setSourceLocation(dbConnection.getConnectionString());
        readConfig.setColumnMappings(originalConfig.getColumnMappings());
        
        Map<String, Object> readOptions = new HashMap<>();
        readOptions.put("query", "SELECT * FROM " + testTableName + " ORDER BY id");
        readConfig.setOptions(readOptions);
        
        // Read data back from database
        postgreSQLProcessor.readFromPostgreSQLDatabase(readTable, dbConnection, readConfig);
        
        // Verify read data
        assertTrue(readTable.getRowCount() > 0, "Should have read data back from PostgreSQL");
        assertEquals(5, readTable.getColumnCount(), "Should have 5 columns when reading back");
        
        logger.info("Successfully read back {} rows from PostgreSQL database", readTable.getRowCount());
    }

    private void cleanupTestFiles() {
        String[] testFiles = {
            "test_postgresql_data.csv",
            "csv_to_postgresql_mapping.json",
            "postgresql_mapping.json"
        };
        
        for (String file : testFiles) {
            try {
                Files.deleteIfExists(Paths.get(file));
                logger.debug("Cleaned up test file: {}", file);
            } catch (IOException e) {
                logger.warn("Failed to cleanup test file: {}", file, e);
            }
        }
    }
}
