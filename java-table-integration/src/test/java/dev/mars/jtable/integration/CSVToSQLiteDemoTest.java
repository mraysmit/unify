package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for CSVToSQLiteDemo.
 * This class tests the integration between CSV files and SQLite database.
 */
public class CSVToSQLiteDemoTest {

    private static final Logger logger = LoggerFactory.getLogger(CSVToSQLiteDemoTest.class);

    private static final String TEST_CSV_FILE = "test_data.csv";
    private static final String TEST_DB_URL = "jdbc:sqlite:test_sqlite.db";
    private static final String TEST_TABLE_NAME = "test_person";

    @BeforeEach
    public void setUp() throws Exception {
        logger.info("Setting up test environment");

        // Create test CSV file
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("id,name,email,age\n");
        csvContent.append("1,John Doe,john.doe@example.com,30\n");
        csvContent.append("2,Jane Smith,jane.smith@example.com,25\n");

        Files.write(Paths.get(TEST_CSV_FILE), csvContent.toString().getBytes());
        logger.debug("Created test CSV file: {}", TEST_CSV_FILE);

        // Clean up any existing test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);
            logger.debug("Dropped existing test table if it existed: {}", TEST_TABLE_NAME);
        } catch (Exception e) {
            logger.warn("Could not drop test table: {}", e.getMessage());
            // Ignore if table doesn't exist
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.info("Tearing down test environment");

        // Delete test CSV file
        boolean deleted = Files.deleteIfExists(Paths.get(TEST_CSV_FILE));
        logger.debug("Deleted test CSV file: {} (success: {})", TEST_CSV_FILE, deleted);

        // Clean up test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);
            logger.debug("Dropped test table: {}", TEST_TABLE_NAME);
        } catch (Exception e) {
            logger.warn("Could not drop test table: {}", e.getMessage());
            // Ignore if table doesn't exist
        }

        // Delete the SQLite database file
        try {
            Files.deleteIfExists(Paths.get("test_sqlite.db"));
            logger.debug("Deleted SQLite database file");
        } catch (Exception e) {
            logger.warn("Could not delete SQLite database file: {}", e.getMessage());
        }
    }

    @Test
    public void testCSVToSQLiteIntegration() throws Exception {
        logger.info("Starting CSV to SQLite integration test");

        // Create a table to hold the data
        ITable table = new TableCore();
        logger.debug("Created empty table for testing");

        // Read from CSV
        logger.info("Reading data from CSV file: {}", TEST_CSV_FILE);
        readFromCSV(table, TEST_CSV_FILE);

        // Verify data was read correctly
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals("John Doe", table.getValueAt(0, "fullName"), "First row name should be John Doe");
        assertEquals("25", table.getValueAt(1, "personAge"), "Second row age should be 25");
        assertEquals("General", table.getValueAt(0, "department"), "First row department should be General");
        logger.info("Successfully verified CSV data was read correctly");

        // Write to SQLite database
        logger.info("Writing data to SQLite database: {}", TEST_DB_URL);
        writeToSQLiteDatabase(table, TEST_DB_URL, TEST_TABLE_NAME);

        // Verify data was written correctly
        logger.info("Verifying data was written correctly to database");
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TEST_TABLE_NAME)) {

            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(2, rs.getInt(1), "Database table should have 2 rows");
            logger.debug("Verified database table has correct number of rows: {}", rs.getInt(1));
        }

        // Verify specific data in the database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE_NAME + " WHERE person_id = 1")) {

            assertTrue(rs.next(), "Result set should have a row with person_id = 1");
            assertEquals("John Doe", rs.getString("full_name"), "Name should be John Doe");
            assertEquals("john.doe@example.com", rs.getString("email_address"), "Email should be john.doe@example.com");
            assertEquals(30, rs.getInt("age"), "Age should be 30");
            assertEquals("General", rs.getString("department"), "Department should be General");
            logger.debug("Verified specific data in database for person_id=1");
        }

        logger.info("CSV to SQLite integration test completed successfully");
    }

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     *
     * @param table       the table to read into
     * @param csvFilePath the path to the CSV file
     * @param csvConfig   the mapping configuration to use
     * @throws Exception if there is an error reading the file
     */
    void readFromCSV(ITable table, String csvFilePath, MappingConfiguration csvConfig) throws Exception {
        logger.debug("Reading from CSV file: {}", csvFilePath);

        // Call the method in CSVToSQLiteDemo
        CSVToSQLiteDemo.readFromCSV(table, csvFilePath, csvConfig);
        logger.debug("Successfully read data from CSV file");
    }

    /**
     * Reads data from a CSV file into a table using a default MappingConfiguration.
     *
     * @param table       the table to read into
     * @param csvFilePath the path to the CSV file
     * @throws Exception if there is an error reading the file
     */
    void readFromCSV(ITable table, String csvFilePath) throws Exception {
        logger.debug("Reading from CSV file with default configuration: {}", csvFilePath);

        // Create a FileConnection to get the location
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
        if (!connection.connect()) {
            throw new Exception("Failed to connect to CSV file: " + csvFilePath);
        }

        // Create a default mapping configuration
        MappingConfiguration csvConfig = CSVToSQLiteDemo.createDemoCSVMappingConfiguration(connection.getLocation());
        connection.disconnect();

        // Call the method with the configuration
        readFromCSV(table, csvFilePath, csvConfig);
    }

    /**
     * Writes data from a table to a SQLite database.
     *
     * @param table     the table to write from
     * @param dbUrl     the database URL
     * @param tableName the table name
     * @throws Exception if there is an error writing to the database
     */
    void writeToSQLiteDatabase(ITable table, String dbUrl, String tableName) throws Exception {
        logger.debug("Writing to SQLite database: {} table: {}", dbUrl, tableName);

        // Create a database connection
        String username = "";  // SQLite typically doesn't use username/password
        String password = "";
        DbConnection connection = new DbConnection(dbUrl, username, password);

        try {
            if (!connection.connect()) {
                throw new SQLException("Failed to connect to SQLite database: " + dbUrl);
            }
            logger.debug("Successfully connected to SQLite database");

            // Create a mapping configuration
            MappingConfiguration sqliteConfig = new MappingConfiguration()
                .setSourceLocation(dbUrl)
                .setOption("tableName", tableName)
                .setOption("username", username)
                .setOption("password", password)
                .setOption("createTable", true);

            // Add column mappings to match our transformed input columns
            sqliteConfig.addColumnMapping(new ColumnMapping("personId", "person_id", "int")
                        .setDefaultValue("0"))
                    .addColumnMapping(new ColumnMapping("fullName", "full_name", "string")
                        .setDefaultValue("Unknown"))
                    .addColumnMapping(new ColumnMapping("emailAddress", "email_address", "string")
                        .setDefaultValue("no-email@example.com"))
                    .addColumnMapping(new ColumnMapping("personAge", "age", "int")
                        .setDefaultValue("0"))
                    .addColumnMapping(new ColumnMapping("department", "department", "string")
                        .setDefaultValue("General"));

            // Call the method in CSVToSQLiteDemo with proper dependency injection
            CSVToSQLiteDemo.writeToSQLiteDatabase(table, connection, sqliteConfig);
            logger.debug("Successfully wrote data to SQLite database");
        } finally {
            // Ensure connection is closed even if an exception occurs
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from SQLite database");
            }
        }
    }



}
