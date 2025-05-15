package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for date, time, and datetime data types.
 * This class tests that the integration between CSV files and databases properly handles date, time, and datetime types.
 */
public class DateTimeTypeIntegrationTest {

    private static final String TEST_CSV_FILE = "test_datetime_data.csv";
    private static final String TEST_H2_DB_URL = "jdbc:h2:mem:test_datetime;DB_CLOSE_DELAY=-1";
    private static final String TEST_SQLITE_DB_URL = "jdbc:sqlite:test_datetime.db";

    @BeforeEach
    void setUp() throws Exception {
        // Create a test CSV file with date, time, and datetime columns
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("id,name,birth_date,start_time,created_at\n");
        csvContent.append("1,Alice,1990-01-15,09:30:00,2023-05-20T14:30:00\n");
        csvContent.append("2,Bob,1985-03-22,10:45:00,2023-05-21T09:15:00\n");

        Files.write(Paths.get(TEST_CSV_FILE), csvContent.toString().getBytes());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Delete the test CSV file
        Files.deleteIfExists(Paths.get(TEST_CSV_FILE));

        // Delete the SQLite database file
        Files.deleteIfExists(Paths.get("test_datetime.db"));
    }

    @Test
    void testCSVToH2WithDateTimeTypes() throws Exception {
        // Create a table to hold the data
        ITable table = new Table();

        // Create a mapping configuration for reading from CSV
        MappingConfiguration csvConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("name", "NAME", "string"))
                .addColumnMapping(new ColumnMapping("birth_date", "BIRTH_DATE", "date"))
                .addColumnMapping(new ColumnMapping("start_time", "START_TIME", "time"))
                .addColumnMapping(new ColumnMapping("created_at", "CREATED_AT", "datetime"));

        // Read from CSV
        CSVToH2Demo.readFromCSV(table, TEST_CSV_FILE, csvConfig);

        // Verify the data was read correctly
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(5, table.getColumnCount(), "Table should have 5 columns");

        // Check row values for Alice
        assertEquals("1", table.getValueAt(0, "ID"), "First row, ID column should be '1'");
        assertEquals("Alice", table.getValueAt(0, "NAME"), "First row, NAME column should be 'Alice'");
        assertEquals("1990-01-15", table.getValueAt(0, "BIRTH_DATE"), "First row, BIRTH_DATE column should be '1990-01-15'");
        assertEquals("09:30:00", table.getValueAt(0, "START_TIME"), "First row, START_TIME column should be '09:30:00'");
        assertEquals("2023-05-20T14:30:00", table.getValueAt(0, "CREATED_AT"), "First row, CREATED_AT column should be '2023-05-20T14:30:00'");

        // Create a mapping configuration for writing to H2
        MappingConfiguration h2Config = new MappingConfiguration()
                .setSourceLocation(TEST_H2_DB_URL)
                .setOption("tableName", "DATETIME_TEST")
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "ID", "int"))
                .addColumnMapping(new ColumnMapping("NAME", "NAME", "string"))
                .addColumnMapping(new ColumnMapping("BIRTH_DATE", "BIRTH_DATE", "date"))
                .addColumnMapping(new ColumnMapping("START_TIME", "START_TIME", "time"))
                .addColumnMapping(new ColumnMapping("CREATED_AT", "CREATED_AT", "datetime"));

        // Write to H2 database
        try (Connection conn = DriverManager.getConnection(TEST_H2_DB_URL)) {
            // Drop the table if it exists
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS DATETIME_TEST");
            }
        }

        // Create a database connection for H2
        String username = "";
        String password = "";
        DbConnection dbConnection = new DbConnection(TEST_H2_DB_URL, username, password);

        try {
            if (!dbConnection.connect()) {
                throw new SQLException("Failed to connect to H2 database: " + TEST_H2_DB_URL);
            }

            // Use the existing h2Config

            // Use the CSVToH2Demo to write to the database with proper dependency injection
            CSVToH2Demo.writeToH2Database(table, dbConnection, h2Config);
        } finally {
            // Ensure connection is closed even if an exception occurs
            if (dbConnection != null && dbConnection.isConnected()) {
                dbConnection.disconnect();
            }
        }

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_H2_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM DATETIME_TEST ORDER BY ID")) {

            // Check the first row (Alice)
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("ID"), "First row ID should be 1");
            assertEquals("Alice", rs.getString("NAME"), "First row NAME should be Alice");
            assertEquals("1990-01-15", rs.getDate("BIRTH_DATE").toString(), "First row BIRTH_DATE should be 1990-01-15");
            assertEquals("09:30:00", rs.getTime("START_TIME").toString(), "First row START_TIME should be 09:30:00");

            // The CREATED_AT column might be returned as a timestamp with or without the 'T'
            String createdAt = rs.getTimestamp("CREATED_AT").toString();
            assertTrue(createdAt.contains("2023-05-20") && createdAt.contains("14:30:00"), 
                    "First row CREATED_AT should contain '2023-05-20' and '14:30:00'");

            // Check the second row (Bob)
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(2, rs.getInt("ID"), "Second row ID should be 2");
            assertEquals("Bob", rs.getString("NAME"), "Second row NAME should be Bob");
            assertEquals("1985-03-22", rs.getDate("BIRTH_DATE").toString(), "Second row BIRTH_DATE should be 1985-03-22");
            assertEquals("10:45:00", rs.getTime("START_TIME").toString(), "Second row START_TIME should be 10:45:00");

            // The CREATED_AT column might be returned as a timestamp with or without the 'T'
            createdAt = rs.getTimestamp("CREATED_AT").toString();
            assertTrue(createdAt.contains("2023-05-21") && createdAt.contains("09:15:00"), 
                    "Second row CREATED_AT should contain '2023-05-21' and '09:15:00'");

            // There should be no more rows
            assertFalse(rs.next(), "Result set should have exactly two rows");
        }
    }

    @Test
    void testCSVToSQLiteWithDateTimeTypes() throws Exception {
        // Create a table to hold the data
        ITable table = new Table();

        // Create a mapping configuration for reading from CSV
        MappingConfiguration csvConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("name", "NAME", "string"))
                .addColumnMapping(new ColumnMapping("birth_date", "BIRTH_DATE", "date"))
                .addColumnMapping(new ColumnMapping("start_time", "START_TIME", "time"))
                .addColumnMapping(new ColumnMapping("created_at", "CREATED_AT", "datetime"));

        // Read from CSV
        CSVToSQLiteDemo.readFromCSV(table, TEST_CSV_FILE, csvConfig);

        // Verify the data was read correctly
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(5, table.getColumnCount(), "Table should have 5 columns");

        // Check row values for Alice
        assertEquals("1", table.getValueAt(0, "ID"), "First row, ID column should be '1'");
        assertEquals("Alice", table.getValueAt(0, "NAME"), "First row, NAME column should be 'Alice'");
        assertEquals("1990-01-15", table.getValueAt(0, "BIRTH_DATE"), "First row, BIRTH_DATE column should be '1990-01-15'");
        assertEquals("09:30:00", table.getValueAt(0, "START_TIME"), "First row, START_TIME column should be '09:30:00'");
        assertEquals("2023-05-20T14:30:00", table.getValueAt(0, "CREATED_AT"), "First row, CREATED_AT column should be '2023-05-20T14:30:00'");

        // Create a database connection for SQLite
        String username = "";  // SQLite typically doesn't use username/password
        String password = "";
        DbConnection connection = new DbConnection(TEST_SQLITE_DB_URL, username, password);

        try {
            if (!connection.connect()) {
                throw new SQLException("Failed to connect to SQLite database: " + TEST_SQLITE_DB_URL);
            }

            // Create a mapping configuration for SQLite with datetime support
            MappingConfiguration sqliteConfig = new MappingConfiguration()
                .setSourceLocation(TEST_SQLITE_DB_URL)
                .setOption("tableName", "datetime_test")
                .setOption("username", username)
                .setOption("password", password)
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "id", "int"))
                .addColumnMapping(new ColumnMapping("NAME", "name", "string"))
                .addColumnMapping(new ColumnMapping("BIRTH_DATE", "birth_date", "date"))
                .addColumnMapping(new ColumnMapping("START_TIME", "start_time", "time"))
                .addColumnMapping(new ColumnMapping("CREATED_AT", "created_at", "datetime"));

            // Use the CSVToSQLiteDemo to write to the database with proper dependency injection
            CSVToSQLiteDemo.writeToSQLiteDatabase(table, connection, sqliteConfig);

            // Verify the data was written correctly
            try (Connection conn = DriverManager.getConnection(TEST_SQLITE_DB_URL);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM datetime_test ORDER BY id")) {

                // Check the first row (Alice)
                assertTrue(rs.next(), "Result set should have at least one row");
                assertEquals(1, rs.getInt("id"), "First row id should be 1");
                assertEquals("Alice", rs.getString("name"), "First row name should be Alice");

                // SQLite stores dates, times, and datetimes as strings, so we need to check the string values
                assertEquals("1990-01-15", rs.getString("birth_date"), "First row birth_date should be 1990-01-15");
                assertEquals("09:30:00", rs.getString("start_time"), "First row start_time should be 09:30:00");

                // The created_at column might be stored with or without the 'T'
                String createdAt = rs.getString("created_at");
                assertTrue(createdAt.contains("2023-05-20") && createdAt.contains("14:30:00"), 
                        "First row created_at should contain '2023-05-20' and '14:30:00'");

                // Check the second row (Bob)
                assertTrue(rs.next(), "Result set should have at least two rows");
                assertEquals(2, rs.getInt("id"), "Second row id should be 2");
                assertEquals("Bob", rs.getString("name"), "Second row name should be Bob");
                assertEquals("1985-03-22", rs.getString("birth_date"), "Second row birth_date should be 1985-03-22");
                assertEquals("10:45:00", rs.getString("start_time"), "Second row start_time should be 10:45:00");

                // The created_at column might be stored with or without the 'T'
                createdAt = rs.getString("created_at");
                assertTrue(createdAt.contains("2023-05-21") && createdAt.contains("09:15:00"), 
                        "Second row created_at should contain '2023-05-21' and '09:15:00'");

                // There should be no more rows
                assertFalse(rs.next(), "Result set should have exactly two rows");
            }
        } finally {
            // Ensure connection is closed even if an exception occurs
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
            }
        }
    }
}
