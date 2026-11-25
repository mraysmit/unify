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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of CSV to PostgreSQL database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to a PostgreSQL database using MappingConfiguration
 * 3. Save and load mapping configurations using JSON serialization
 * 
 * Prerequisites:
 * - PostgreSQL server running and accessible
 * - Database and user credentials configured in db.properties
 * - CSV file with sample data
 */
public class CSVToPostgreSQLDemo {
    private static final Logger logger = LoggerFactory.getLogger(CSVToPostgreSQLDemo.class);

    private final CSVProcessor csvProcessor;
    private final PostgreSQLProcessor postgreSQLProcessor;
    private final DbConnectionManager connectionManager;
    private final MappingConfigurationManager configManager;

    public CSVToPostgreSQLDemo() {
        this.csvProcessor = new CSVProcessor();
        this.postgreSQLProcessor = new PostgreSQLProcessor();
        this.connectionManager = new DbConnectionManager();
        this.configManager = new MappingConfigurationManager(new JSONMappingSerializer());
    }

    /**
     * Main method to run the CSV to PostgreSQL integration demo.
     */
    public static void main(String[] args) {
        CSVToPostgreSQLDemo demo = new CSVToPostgreSQLDemo();
        
        try {
            logger.info("Starting CSV to PostgreSQL Integration Demo");
            
            // Run the integration demo
            demo.runIntegrationDemo();
            
            logger.info("CSV to PostgreSQL Integration Demo completed successfully");
            
        } catch (Exception e) {
            logger.error("Error during CSV to PostgreSQL integration demo: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Runs the complete integration demonstration.
     */
    public void runIntegrationDemo() throws IOException, SQLException {
        String csvFile = "demo_data.csv";
        String csvMappingFile = "csv_to_postgresql_mapping.json";
        String dbMappingFile = "postgresql_mapping.json";

        // Step 1: Create a table to hold the data
        ITable table = new TableCore("CSVToPostgreSQLDemo-InputTable");
        logger.info("Created table: {}", table.getName());

        // Step 2: Create CSV mapping configuration
        MappingConfiguration csvMappingConfig = createCSVMappingConfiguration(csvFile);
        
        // Save CSV mapping configuration
        configManager.saveMappingConfiguration(csvMappingConfig, csvMappingFile);
        logger.info("Saved CSV mapping configuration to: {}", csvMappingFile);

        // Step 3: Read data from CSV file
        csvProcessor.readFromCSV(table, csvFile, csvMappingConfig);
        logger.info("Read {} rows from CSV file: {}", table.getRowCount(), csvFile);

        // Step 4: Create database connection
        DbConnection dbConnection = connectionManager.createPostgreSQLConnection(new DatabaseProperties());
        logger.info("Created PostgreSQL database connection");

        try {
            // Step 5: Create database mapping configuration
            MappingConfiguration dbMappingConfig = createDatabaseMappingConfiguration(dbConnection);
            
            // Save database mapping configuration
            configManager.saveMappingConfiguration(dbMappingConfig, dbMappingFile);
            logger.info("Saved PostgreSQL mapping configuration to: {}", dbMappingFile);

            // Step 6: Write data to PostgreSQL database
            postgreSQLProcessor.writeToPostgreSQLDatabase(table, dbConnection, dbMappingConfig);
            logger.info("Successfully wrote {} rows to PostgreSQL database", table.getRowCount());

            // Step 7: Verify the data was written correctly
            verifyDatabaseData(dbConnection, dbMappingConfig);

            // Step 8: Demonstrate loading configurations from files
            demonstrateConfigurationLoading(csvMappingFile, dbMappingFile);

        } finally {
            // Clean up database connection
            if (dbConnection.isConnected()) {
                dbConnection.disconnect();
                logger.info("Disconnected from PostgreSQL database");
            }
        }
    }

    /**
     * Creates a mapping configuration for reading CSV data.
     */
    private MappingConfiguration createCSVMappingConfiguration(String csvFile) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(csvFile);

        // Define column mappings
        List<ColumnMapping> columnMappings = new ArrayList<>();
        columnMappings.add(new ColumnMapping("id", "id", "int"));
        columnMappings.add(new ColumnMapping("name", "name", "string"));
        columnMappings.add(new ColumnMapping("email", "email", "string"));
        columnMappings.add(new ColumnMapping("age", "age", "int"));
        columnMappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(columnMappings);

        // Set CSV-specific options
        Map<String, Object> options = new HashMap<>();
        options.put("hasHeaderRow", true);
        options.put("delimiter", ",");
        options.put("encoding", "UTF-8");
        config.setOptions(options);

        return config;
    }

    /**
     * Creates a mapping configuration for writing to PostgreSQL database.
     */
    private MappingConfiguration createDatabaseMappingConfiguration(DbConnection connection) {
        MappingConfiguration config = new MappingConfiguration();
        config.setSourceLocation(connection.getConnectionString());

        // Define column mappings (same as CSV for this demo)
        List<ColumnMapping> columnMappings = new ArrayList<>();
        columnMappings.add(new ColumnMapping("id", "id", "int"));
        columnMappings.add(new ColumnMapping("name", "name", "string"));
        columnMappings.add(new ColumnMapping("email", "email", "string"));
        columnMappings.add(new ColumnMapping("age", "age", "int"));
        columnMappings.add(new ColumnMapping("salary", "salary", "double"));
        config.setColumnMappings(columnMappings);

        // Set PostgreSQL-specific options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", "demo_employees_postgresql");
        options.put("createTable", true);
        options.put("batchSize", 1000);
        config.setOptions(options);

        return config;
    }

    /**
     * Verifies that data was correctly written to the database.
     */
    private void verifyDatabaseData(DbConnection connection, MappingConfiguration config) throws SQLException {
        String tableName = (String) config.getOptions().get("tableName");
        
        // Check if table exists
        if (postgreSQLProcessor.tableExists(connection, tableName)) {
            logger.info("Verified that PostgreSQL table '{}' exists", tableName);
            
            // Create a new table to read the data back
            ITable verificationTable = new TableCore("VerificationTable");
            
            // Create read configuration
            MappingConfiguration readConfig = new MappingConfiguration();
            readConfig.setSourceLocation(connection.getConnectionString());
            readConfig.setColumnMappings(config.getColumnMappings());
            
            Map<String, Object> readOptions = new HashMap<>();
            readOptions.put("query", "SELECT * FROM " + tableName + " ORDER BY id");
            readConfig.setOptions(readOptions);
            
            // Read data back
            postgreSQLProcessor.readFromPostgreSQLDatabase(verificationTable, connection, readConfig);
            logger.info("Verification: Read back {} rows from PostgreSQL table", verificationTable.getRowCount());
            
        } else {
            logger.warn("PostgreSQL table '{}' does not exist", tableName);
        }
    }

    /**
     * Demonstrates loading mapping configurations from saved files.
     */
    private void demonstrateConfigurationLoading(String csvMappingFile, String dbMappingFile) throws IOException {
        logger.info("Demonstrating configuration loading from files...");

        // Load CSV mapping configuration
        MappingConfiguration loadedCSVConfig = configManager.loadMappingConfiguration(csvMappingFile);
        logger.info("Loaded CSV mapping configuration from: {}", csvMappingFile);
        logger.info("CSV source location: {}", loadedCSVConfig.getSourceLocation());
        logger.info("CSV column mappings count: {}", loadedCSVConfig.getColumnMappings().size());

        // Load database mapping configuration
        MappingConfiguration loadedDBConfig = configManager.loadMappingConfiguration(dbMappingFile);
        logger.info("Loaded PostgreSQL mapping configuration from: {}", dbMappingFile);
        logger.info("PostgreSQL source location: {}", loadedDBConfig.getSourceLocation());
        logger.info("PostgreSQL table name: {}", loadedDBConfig.getOptions().get("tableName"));
    }
}
