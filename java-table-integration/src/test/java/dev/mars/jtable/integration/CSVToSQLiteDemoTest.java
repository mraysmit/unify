package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
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
        SQLiteTestUtils.readFromCSV(table, TEST_CSV_FILE);

        // Verify data was read correctly
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals("John Doe", table.getValueAt(0, "fullName"), "First row name should be John Doe");
        assertEquals("25", table.getValueAt(1, "personAge"), "Second row age should be 25");
        assertEquals("General", table.getValueAt(0, "department"), "First row department should be General");
        logger.info("Successfully verified CSV data was read correctly");

        // Write to SQLite database
        logger.info("Writing data to SQLite database: {}", TEST_DB_URL);
        SQLiteTestUtils.writeToSQLiteDatabase(table, TEST_DB_URL, TEST_TABLE_NAME);

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



}
