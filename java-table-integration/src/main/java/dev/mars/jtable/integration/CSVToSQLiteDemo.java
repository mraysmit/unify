package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.jTableJDBCConnection;
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
 * Demonstration of CSV to SQLite database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to a SQLite database using MappingConfiguration
 */
public class CSVToSQLiteDemo {

    private static final Logger logger = LoggerFactory.getLogger(CSVToSQLiteDemo.class);

    /**
     * Main method to run the demonstration.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting CSV to SQLite demonstration");

            // Create a demo CSV file if it doesn't exist
            String csvFilePath = "demo_data.csv";
            createDemoCSVIfNotExists(csvFilePath);
            logger.debug("Demo CSV file created or already exists: {}", csvFilePath);

            // Create a table to hold the data
            ITable table = new TableCore();
            logger.debug("Created empty table");

            // Create a FileConnection for CSV
            FileConnection csvConnection = null;
            try {
                csvConnection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
                if (!csvConnection.connect()) {
                    throw new IOException("Failed to connect to CSV file: " + csvFilePath);
                }
                logger.debug("Successfully connected to CSV file: {}", csvFilePath);

                // Create a mapping configuration for reading from CSV
                MappingConfiguration csvConfig = createCSVMappingConfiguration(csvConnection);
                logger.debug("Created CSV mapping configuration");

                // Read from CSV
                readFromCSV(table, csvFilePath, csvConfig);
                logger.info("Successfully read data from CSV file: {}", csvFilePath);
                logger.debug("Using CSV mapping configuration: {}", csvConfig);
                logger.info("Table contains {} rows", table.getRowCount());

                // Create a JDBCConnection for SQLite
                String connectionString = DatabaseProperties.getSqliteConnectionString();
                String username = DatabaseProperties.getSqliteUsername();
                String password = DatabaseProperties.getSqlitePassword();
                logger.info("Creating JDBCConnection for SQLite database: {}", connectionString);


                jTableJDBCConnection sqliteConnection = null;
                try {
                    sqliteConnection = new jTableJDBCConnection(connectionString, username, password);
                    if (!sqliteConnection.connect()) {
                        throw new SQLException("Failed to connect to SQLite database: " + connectionString);
                    }
                    logger.debug("Successfully connected to SQLite database");

                    // Create a mapping configuration for writing to SQLite database
                    MappingConfiguration sqliteConfig = createSQLiteMappingConfiguration(sqliteConnection, username, password);
                    logger.debug("Created SQLite mapping configuration");

                    // Write to SQLite database
                    writeToSQLiteDatabase(table, sqliteConfig);
                    logger.info("Successfully wrote data to SQLite database");
                } finally {
                    // Ensure SQLite connection is closed even if an exception occurs
                    if (sqliteConnection != null && sqliteConnection.isConnected()) {
                        sqliteConnection.disconnect();
                        logger.debug("Disconnected from SQLite database");
                    }
                }
            } finally {
                // Ensure CSV connection is closed even if an exception occurs
                if (csvConnection != null && csvConnection.isConnected()) {
                    csvConnection.disconnect();
                    logger.debug("Disconnected from CSV file");
                }
            }
        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates a mapping configuration for reading from CSV.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Transforming column names (e.g., "name" to "fullName")
     * - Setting default values for missing columns
     * - Handling edge cases
     *
     * @param connection the FileConnection to use
     * @return the mapping configuration
     */
    static MappingConfiguration createCSVMappingConfiguration(FileConnection connection) {
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

        logger.debug("Created CSV mapping configuration with column transformations and default values");

        return csvConfig;
    }

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using a mapping configuration for reading from CSV
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @param csvConfig the mapping configuration to use
     * @throws IOException if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath, MappingConfiguration csvConfig) throws IOException {
        logger.info("Reading from CSV file: {}", csvFilePath);

        // Read from CSV using the mapping configuration
        CSVMappingReader csvReader = new CSVMappingReader();
        csvReader.readFromCSV(table, csvConfig);

        logger.info("Successfully read data from CSV file with {} rows and {} columns", 
                table.getRowCount(), table.getColumnCount());
    }

    /**
     * Creates a mapping configuration for writing to SQLite database.
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
    static MappingConfiguration createSQLiteMappingConfiguration(jTableJDBCConnection connection, String username, String password) {
        // Create a mapping configuration for writing to SQLite database
        MappingConfiguration sqliteConfig = new MappingConfiguration()
                .setSourceLocation(connection.getConnectionString())
                .setOption("tableName", "person_data")  // Changed table name to be more descriptive
                .setOption("username", username)
                .setOption("password", password)
                .setOption("createTable", true);

        // Add column mappings with transformations to match our transformed input columns
        // Note: We're mapping from the transformed column names we used in readFromCSV
        // SQLite preserves case sensitivity, so we'll use snake_case for column names
        sqliteConfig.addColumnMapping(new ColumnMapping("personId", "person_id", "int")
                    .setDefaultValue("0"))  // Map personId -> person_id with default value
                .addColumnMapping(new ColumnMapping("fullName", "full_name", "string")
                    .setDefaultValue("Unknown"))  // Map fullName -> full_name with default value
                .addColumnMapping(new ColumnMapping("emailAddress", "email_address", "string")
                    .setDefaultValue("no-email@example.com"))  // Map emailAddress -> email_address with default value
                .addColumnMapping(new ColumnMapping("personAge", "age", "int")
                    .setDefaultValue("0"))  // Map personAge -> age with default value
                .addColumnMapping(new ColumnMapping("department", "department", "string")
                    .setDefaultValue("General"));  // Map department -> department with default value

        logger.debug("Created SQLite mapping configuration with column transformations and default values");

        return sqliteConfig;
    }

    /**
     * Writes data from a table to a SQLite database using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using a mapping configuration for writing to SQLite database
     *
     * @param table the table to write from
     * @param sqliteConfig the mapping configuration to use
     * @throws SQLException if there is an error writing to the database
     */
    static void writeToSQLiteDatabase(ITable table, MappingConfiguration sqliteConfig) throws SQLException {
        logger.info("Writing to SQLite database: {}", sqliteConfig.getSourceLocation());

        // Write to SQLite database
        JDBCMappingWriter sqliteWriter = new JDBCMappingWriter();
        sqliteWriter.writeToDatabase(table, sqliteConfig);

        logger.info("Successfully wrote data to SQLite database table: {}", sqliteConfig.getOption("tableName", "person_data"));
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
