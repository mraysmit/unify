package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.JDBCConnection;
import dev.mars.jtable.io.files.csv.CSVMappingReader;
import dev.mars.jtable.io.files.jdbc.JDBCMappingWriter;
import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Demonstration of CSV to H2 database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to an H2 database using MappingConfiguration
 */
public class CSVToH2Demo {

    private static final Logger logger = LoggerFactory.getLogger(CSVToH2Demo.class);

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
            ITable table = new TableCore();
            logger.debug("Created empty table");

            // Read from CSV
            readFromCSV(table, csvFilePath);
            logger.info("Successfully read data from CSV file: {}", csvFilePath);
            logger.info("Table contains {} rows", table.getRowCount());

            // Write to H2 database
            writeToH2Database(table);
            logger.info("Successfully wrote data to H2 database");

        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using FileConnection from DataSourceConnectionFactory
     * - Transforming column names (e.g., "name" to "fullName")
     * - Setting default values for missing columns
     * - Handling edge cases
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @throws IOException if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath) throws IOException {
        logger.info("Creating FileConnection for CSV file: {}", csvFilePath);

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = null;
        try {
            connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new IOException("Failed to connect to CSV file: " + csvFilePath);
            }
            logger.debug("Successfully connected to CSV file");

            // Create a mapping configuration for reading from CSV
            MappingConfiguration csvConfig = new MappingConfiguration()
                    .setSourceLocation(connection.getLocation())
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

            logger.debug("Created mapping configuration with column transformations and default values");

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
     * Writes data from a table to an H2 database using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using JDBCConnection for database access
     * - Transforming column names to match the database schema
     * - Setting default values for missing columns
     * - Using advanced database options
     *
     * @param table the table to write from
     * @throws SQLException if there is an error writing to the database
     */
    static void writeToH2Database(ITable table) throws SQLException {
        String connectionString = "jdbc:h2:./demo_db";
        String username = "sa";
        String password = "";
        logger.info("Creating JDBCConnection for H2 database: {}", connectionString);

        // Create a JDBCConnection
        JDBCConnection connection = null;
        try {
            connection = new JDBCConnection(connectionString, username, password);
            if (!connection.connect()) {
                throw new SQLException("Failed to connect to H2 database: " + connectionString);
            }
            logger.debug("Successfully connected to H2 database");

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

            logger.debug("Created mapping configuration with column transformations and default values");

            // Write to H2 database
            JDBCMappingWriter h2Writer = new JDBCMappingWriter();
            h2Writer.writeToDatabase(table, h2Config);

            logger.info("Successfully wrote data to H2 database table: PERSON_DATA");

        } finally {
            // Ensure connection is closed even if an exception occurs
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from H2 database");
            }
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
}
