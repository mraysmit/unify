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
package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.files.csv.CSVMappingReader;
import dev.mars.jtable.io.files.jdbc.JDBCMappingWriter;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.IMappingSerializer;
import dev.mars.jtable.io.common.mapping.MappingSerializerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

/**
 * Demonstration of CSV to H2 database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to an H2 database using MappingConfiguration
 * 3. Save and load mapping configurations using JSON serialization
 */
public class CSVToH2Demo {

    private static final Logger logger = LoggerFactory.getLogger(CSVToH2Demo.class);

    // File names for mapping configurations
    private static final String CSV_MAPPING_FILE = "csv_mapping.json";
    private static final String H2_MAPPING_FILE = "h2_mapping.json";

    // Path to the mappings directory in resources
    private static final String MAPPINGS_DIR = "mappings";

    /**
     * Main method to run the demonstration.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting CSV to H2 demonstration");

            // Create a demo CSV file if it doesn't exist
            String csvFilePath = "demo_data.csv";
            createDemoCSVIfNotExists(csvFilePath);

            // Create a table to hold the data
            ITable table = new TableCore("CSVToH2Demo-InputTable");
            logger.debug("Created empty table with name '{}'", table.getName());

            // Create a FileConnection to get the location
            FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new IOException("Failed to connect to CSV file: " + csvFilePath);
            }

            // Prepare the mapping configuration
            MappingConfiguration csvConfig = prepareCSVMappingConfiguration(connection.getLocation());
            connection.disconnect();

            // Read from CSV
            readFromCSV(table, csvFilePath, csvConfig);
            logger.info("Successfully read data from CSV file: {}", csvFilePath);
            logger.info("Table contains {} rows", table.getRowCount());

            // Create a database connection for H2
            String connectionString = DatabaseProperties.getH2ConnectionString();
            String username = DatabaseProperties.getH2Username();
            String password = DatabaseProperties.getH2Password();
            logger.info("Creating JDBCConnection for H2 database: {}", connectionString);

            DbConnection dbConnection = null;
            try {
                // Use the factory method to create a connection from properties file if specified
                String propertiesFile = System.getProperty("db.properties.file");
                if (propertiesFile != null && !propertiesFile.isEmpty()) {
                    logger.info("Creating H2 connection from properties file: {}", propertiesFile);
                    dbConnection = DbConnection.createH2Connection(propertiesFile);
                } else {
                    // Fall back to the traditional way if no properties file is specified
                    dbConnection = new DbConnection(connectionString, username, password);
                }

                if (!dbConnection.connect()) {
                    throw new SQLException("Failed to connect to H2 database: " + dbConnection.getConnectionString());
                }
                logger.debug("Successfully connected to H2 database");

                // Prepare the mapping configuration
                MappingConfiguration h2Config = prepareH2MappingConfiguration(dbConnection, 
                        dbConnection.getUsername(), dbConnection.getPassword());

                // Write to H2 database using dependency injection
                writeToH2Database(table, dbConnection, h2Config);
                logger.info("Successfully wrote data to H2 database");
            } finally {
                // Ensure connection is closed even if an exception occurs
                if (dbConnection != null && dbConnection.isConnected()) {
                    dbConnection.disconnect();
                    logger.debug("Disconnected from H2 database");
                }
            }

        } catch (IOException e) {
            logger.error("I/O error in demonstration: {}", e.getMessage(), e);
        } catch (SQLException e) {
            logger.error("SQL error in demonstration: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a mapping configuration for reading from CSV file.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Transforming column names (e.g., "name" to "fullName")
     * - Setting default values for missing columns
     * - Handling edge cases
     *
     * @param fileLocation the location of the CSV file
     * @return the mapping configuration
     */
    static MappingConfiguration createCSVMappingConfiguration(String fileLocation) {
        // Create a mapping configuration for reading from CSV
        MappingConfiguration csvConfig = new MappingConfiguration()
                .setSourceLocation(fileLocation)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", true);

        // Add column mappings with transformations and default values
        csvConfig.addColumnMapping(new ColumnMapping("id", "personId", "int")
                    .setDefaultValue("0"))  // Transform id -> personId with default value
                .addColumnMapping(new ColumnMapping("name", "fullName", "string")
                    .setDefaultValue("Unknown"))  // Transform name -> fullName with default value
                .addColumnMapping(new ColumnMapping("email", "emailAddress", "string")
                    .setDefaultValue("no-email@example.com"))  // Transform email -> emailAddress with default value
                .addColumnMapping(new ColumnMapping("age", "personAge", "int")
                    .setDefaultValue("0"))  // Transform age -> personAge with default value
                .addColumnMapping(new ColumnMapping("department", "department", "string")
                    .setDefaultValue("General"));  // Add a column that might not exist in CSV

        logger.debug("Created CSV mapping configuration with column transformations and default values");

        return csvConfig;
    }

    /**
     * Prepares a CSV mapping configuration, either by loading an existing one or creating a new one.
     *
     * @param fileLocation the location of the CSV file
     * @return the prepared mapping configuration
     * @throws IOException if there is an error with the mapping configuration
     */
    static MappingConfiguration prepareCSVMappingConfiguration(String fileLocation) throws IOException {
        // Try to load the mapping configuration from a file
        MappingConfiguration csvConfig = loadMappingConfiguration(CSV_MAPPING_FILE);

        // If the configuration doesn't exist, create it and save it
        if (csvConfig == null) {
            logger.info("Creating new CSV mapping configuration");
            csvConfig = createCSVMappingConfiguration(fileLocation);

            // Save the configuration to a file
            saveMappingConfiguration(csvConfig, CSV_MAPPING_FILE);
        } else {
            logger.info("Using existing CSV mapping configuration from file");

            // Update the source location to the current file
            csvConfig.setSourceLocation(fileLocation);
        }

        return csvConfig;
    }

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using FileConnection from DataSourceConnectionFactory
     * - Using dependency injection for MappingConfiguration
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @param csvConfig the mapping configuration to use
     * @throws IOException if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath, MappingConfiguration csvConfig) throws IOException {
        logger.info("Creating FileConnection for CSV file: {}", csvFilePath);

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = null;
        try {
            connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new IOException("Failed to connect to CSV file: " + csvFilePath);
            }
            logger.debug("Successfully connected to CSV file");

            // Update the source location to the current file
            csvConfig.setSourceLocation(connection.getLocation());

            // Read from CSV
            CSVMappingReader csvReader = new CSVMappingReader();
            csvReader.readFromCSV(table, csvConfig);

            logger.info("Successfully read data from CSV file with {} rows and {} columns", 
                    table.getRowCount(), table.getColumnCount());

        } finally {
            // Ensure connection is closed even if an exception occurs
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from CSV file");
            }
        }
    }

    /**
     * Creates a mapping configuration for writing to H2 database.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Transforming column names to match the database schema
     * - Setting default values for missing columns
     * - Using advanced database options
     *
     * @param connection the JDBCConnection to use
     * @param username the username for the database
     * @param password the password for the database
     * @return the mapping configuration
     */
    static MappingConfiguration createH2MappingConfiguration(DbConnection connection, String username, String password) {
        // Create a mapping configuration for writing to H2 database
        MappingConfiguration h2Config = new MappingConfiguration()
                .setSourceLocation(connection.getConnectionString())
                .setOption("tableName", "PERSON_DATA")  // Changed table name to be more descriptive
                .setOption("username", username)
                .setOption("password", password)
                .setOption("createTable", true);

        // Add column mappings with transformations to match our transformed input columns
        // Note: We're mapping from the transformed column names we used in readFromCSV
        h2Config.addColumnMapping(new ColumnMapping("personId", "PERSON_ID", "int")
                    .setDefaultValue("0"))  // Map personId -> PERSON_ID with default value
                .addColumnMapping(new ColumnMapping("fullName", "FULL_NAME", "string")
                    .setDefaultValue("Unknown"))  // Map fullName -> FULL_NAME with default value
                .addColumnMapping(new ColumnMapping("emailAddress", "EMAIL_ADDRESS", "string")
                    .setDefaultValue("no-email@example.com"))  // Map emailAddress -> EMAIL_ADDRESS with default value
                .addColumnMapping(new ColumnMapping("personAge", "AGE", "int")
                    .setDefaultValue("0"))  // Map personAge -> AGE with default value
                .addColumnMapping(new ColumnMapping("department", "DEPARTMENT", "string")
                    .setDefaultValue("General"));  // Map department -> DEPARTMENT with default value

        logger.debug("Created H2 mapping configuration with column transformations and default values");

        return h2Config;
    }

    /**
     * Creates a mapping configuration for writing datetime data to H2 database.
     * This method is specifically for handling date, time, and datetime data types.
     *
     * @param connection the JDBCConnection to use
     * @param username the username for the database
     * @param password the password for the database
     * @param tableName the name of the table to write to
     * @return the mapping configuration
     */
    static MappingConfiguration createH2DateTimeMappingConfiguration(DbConnection connection, String username, String password, String tableName) {
        // Create a mapping configuration for writing to H2 database
        MappingConfiguration h2Config = new MappingConfiguration()
                .setSourceLocation(connection.getConnectionString())
                .setOption("tableName", tableName)
                .setOption("username", username)
                .setOption("password", password)
                .setOption("createTable", true);

        // Add column mappings for datetime types
        h2Config.addColumnMapping(new ColumnMapping("ID", "ID", "int"))
                .addColumnMapping(new ColumnMapping("NAME", "NAME", "string"))
                .addColumnMapping(new ColumnMapping("BIRTH_DATE", "BIRTH_DATE", "date"))
                .addColumnMapping(new ColumnMapping("START_TIME", "START_TIME", "time"))
                .addColumnMapping(new ColumnMapping("CREATED_AT", "CREATED_AT", "datetime"));

        logger.debug("Created H2 mapping configuration for datetime types");

        return h2Config;
    }

    static MappingConfiguration prepareH2MappingConfiguration(DbConnection connection, String username, String password) throws IOException {
        // Try to load the mapping configuration from a file
        MappingConfiguration h2Config = loadMappingConfiguration(H2_MAPPING_FILE);

        // If the configuration doesn't exist, create it and save it
        if (h2Config == null) {
            logger.info("Creating new H2 mapping configuration");

            // Create a mapping configuration for writing to H2 database
            h2Config = createH2MappingConfiguration(connection, username, password);
            logger.debug("Created H2 mapping configuration");

            // Save the configuration to a file
            saveMappingConfiguration(h2Config, H2_MAPPING_FILE);
        } else {
            logger.info("Using existing H2 mapping configuration from file");

            // Update the connection information
            h2Config.setSourceLocation(connection.getConnectionString())
                    .setOption("username", username)
                    .setOption("password", password);
        }

        // Check for system property that can override the default table name (for testing)
        String tableName = System.getProperty("h2.table");
        if (tableName != null && !tableName.isEmpty()) {
            logger.info("Overriding table name with system property: {}", tableName);
            h2Config.setOption("tableName", tableName);

            // If the table name is DATETIME_TEST, use the datetime mapping configuration
            if ("DATETIME_TEST".equals(tableName)) {
                h2Config = createH2DateTimeMappingConfiguration(connection, username, password, tableName);
            }
        }

        return h2Config;
    }

    /**
     * Writes data from a table to an H2 database using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using JDBCConnection for database access
     * - Saving and loading mapping configurations using JSON serialization
     *
     * @param table the table to write from
     * @param connection the database connection to use
     * @param h2Config the mapping configuration to use
     * @throws SQLException if there is an error writing to the database
     * @throws IOException if there is an error with the mapping configuration
     */
    static void writeToH2Database(ITable table, DbConnection connection, MappingConfiguration h2Config) throws SQLException, IOException {
        if (table == null) {
            throw new IllegalArgumentException("Table cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Database connection cannot be null");
        }
        if (h2Config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }

        try {
            // Ensure the connection is established
            if (!connection.isConnected() && !connection.connect()) {
                throw new SQLException("Failed to connect to H2 database: " + connection.getConnectionString());
            }
            logger.debug("Successfully connected to H2 database");

            // Write to H2 database
            JDBCMappingWriter h2Writer = new JDBCMappingWriter();
            h2Writer.writeToDatabase(table, h2Config);

            logger.info("Successfully wrote data to H2 database table: {}", h2Config.getOption("tableName", "PERSON_DATA"));
        } finally {
            // We don't disconnect here because the connection was passed in from outside
            // The caller is responsible for managing the connection lifecycle
        }
    }

    /**
     * Creates a demo CSV file if it doesn't exist.
     *
     * @param csvFilePath the path to the CSV file
     * @throws IOException if there is an error creating the file
     */
    private static void createDemoCSVIfNotExists(String csvFilePath) throws IOException {
        File csvFile = new File(csvFilePath);
        if (!csvFile.exists()) {
            logger.info("Creating demo CSV file: {}", csvFilePath);

            // Create a simple CSV file with header and some data
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("id,name,email,age\n");
            csvContent.append("1,John Doe,john.doe@example.com,30\n");
            csvContent.append("2,Jane Smith,jane.smith@example.com,25\n");
            csvContent.append("3,Bob Johnson,bob.johnson@example.com,40\n");

            // Write the content to the file
            java.nio.file.Files.write(csvFile.toPath(), csvContent.toString().getBytes());

            logger.info("Demo CSV file created successfully");
        } else {
            logger.info("Using existing CSV file: {}", csvFilePath);
        }
    }

    /**
     * Gets the path to a mapping configuration file in the resources/mappings directory.
     *
     * @param fileName the name of the mapping configuration file
     * @return the path to the mapping configuration file
     */
    private static String getMappingFilePath(String fileName) {
        try {
            // Get the path to the resources directory
            URL resourceUrl = CSVToH2Demo.class.getClassLoader().getResource("");
            if (resourceUrl == null) {
                // Fallback to the current directory if resources directory is not found
                return new File(System.getProperty("user.dir"), "src\\main\\resources\\" + MAPPINGS_DIR + "\\" + fileName).getAbsolutePath();
            }

            // Construct the path to the mapping file using File to handle path separators correctly
            File resourcesDir = new File(resourceUrl.toURI());
            File mappingsDir = new File(resourcesDir, MAPPINGS_DIR);
            File mappingFile = new File(mappingsDir, fileName);

            return mappingFile.getAbsolutePath();
        } catch (Exception e) {
            // If there's any error, fallback to a simple path
            logger.warn("Error getting mapping file path: {}", e.getMessage());
            return new File(System.getProperty("user.dir"), "src\\main\\resources\\" + MAPPINGS_DIR + "\\" + fileName).getAbsolutePath();
        }
    }

    /**
     * Saves a mapping configuration to a JSON file in the resources/mappings directory.
     *
     * @param config the mapping configuration to save
     * @param fileName the name of the file to save to
     * @throws IOException if there is an error saving the configuration
     */
    private static void saveMappingConfiguration(MappingConfiguration config, String fileName) throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Get the path to the mapping file
        String filePath = getMappingFilePath(fileName);

        // Ensure the directory exists
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        // Write the configuration to the file
        serializer.writeToFile(config, filePath);

        logger.info("Saved mapping configuration to file: {}", filePath);
    }

    /**
     * Loads a mapping configuration from a JSON file in the resources/mappings directory.
     *
     * @param fileName the name of the file to load from
     * @return the loaded mapping configuration, or null if the file doesn't exist
     * @throws IOException if there is an error loading the configuration
     */
    private static MappingConfiguration loadMappingConfiguration(String fileName) throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Get the path to the mapping file
        String filePath = getMappingFilePath(fileName);

        // Check if the file exists
        File file = new File(filePath);
        if (!file.exists()) {
            logger.info("Mapping configuration file does not exist: {}", filePath);
            return null;
        }

        // Read the configuration from the file
        MappingConfiguration config = serializer.readFromFile(filePath);

        logger.info("Loaded mapping configuration from file: {}", filePath);

        return config;
    }
}
