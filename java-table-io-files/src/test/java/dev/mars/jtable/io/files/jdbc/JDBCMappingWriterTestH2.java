package dev.mars.jtable.io.files.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBCMappingWriter.
 * This class tests writing data to a database using JDBCMappingWriter with mapping configurations.
 */
class JDBCMappingWriterTestH2 {

    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String TEST_TABLE = "test_table";
    private Table table;
    private JDBCMappingWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new Table instance for each test
        table = new Table();

        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "int");
        columns.put("Name", "string");
        columns.put("Age", "int");
        table.setColumns(columns);

        // Add some rows to the table
        Map<String, String> row1 = new HashMap<>();
        row1.put("ID", "1");
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("ID", "2");
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        table.addRow(row2);

        Map<String, String> row3 = new HashMap<>();
        row3.put("ID", "3");
        row3.put("Name", "Charlie");
        row3.put("Age", "35");
        table.addRow(row3);

        // Create the writer
        writer = new JDBCMappingWriter();

        // Set up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            // Drop the table if it exists
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
        }
    }

    @Test
    void testWriteToDatabaseWithMapping() throws Exception {
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "id", "int"))
                .addColumnMapping(new ColumnMapping("Name", "full_name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "years", "int"));

        // Write data to the database
        writer.writeToDatabase(table, config);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " ORDER BY id")) {

            // Check the first row
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("id"), "First row id should be 1");
            assertEquals("Alice", rs.getString("full_name"), "First row full_name should be Alice");
            assertEquals(30, rs.getInt("years"), "First row years should be 30");

            // Check the second row
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(2, rs.getInt("id"), "Second row id should be 2");
            assertEquals("Bob", rs.getString("full_name"), "Second row full_name should be Bob");
            assertEquals(25, rs.getInt("years"), "Second row years should be 25");

            // Check the third row
            assertTrue(rs.next(), "Result set should have at least three rows");
            assertEquals(3, rs.getInt("id"), "Third row id should be 3");
            assertEquals("Charlie", rs.getString("full_name"), "Third row full_name should be Charlie");
            assertEquals(35, rs.getInt("years"), "Third row years should be 35");

            // There should be no more rows
            assertFalse(rs.next(), "Result set should have exactly three rows");
        }
    }

    @Test
    void testWriteToDatabaseWithDefaultValues() throws Exception {
        // Create a new table with an additional column
        Table newTable = new Table();

        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "int");
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Job", "string");
        newTable.setColumns(columns);

        // Add rows with job values
        Map<String, String> row1 = new HashMap<>();
        row1.put("ID", "1");
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Job", "Engineer");
        newTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("ID", "2");
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Job", "Designer");
        newTable.addRow(row2);

        Map<String, String> row3 = new HashMap<>();
        row3.put("ID", "3");
        row3.put("Name", "Charlie");
        row3.put("Age", "35");
        row3.put("Job", "Manager");
        newTable.addRow(row3);

        // Use the new table for the test
        table = newTable;

        // Create a mapping configuration that doesn't include the Job column
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "id", "int"))
                .addColumnMapping(new ColumnMapping("Name", "full_name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "years", "int"));

        // Write data to the database
        writer.writeToDatabase(table, config);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " ORDER BY id")) {

            // Check the first row
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("id"), "First row id should be 1");
            assertEquals("Alice", rs.getString("full_name"), "First row full_name should be Alice");
            assertEquals(30, rs.getInt("years"), "First row years should be 30");

            // Check the second row
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(2, rs.getInt("id"), "Second row id should be 2");
            assertEquals("Bob", rs.getString("full_name"), "Second row full_name should be Bob");
            assertEquals(25, rs.getInt("years"), "Second row years should be 25");

            // Check the third row
            assertTrue(rs.next(), "Result set should have at least three rows");
            assertEquals(3, rs.getInt("id"), "Third row id should be 3");
            assertEquals("Charlie", rs.getString("full_name"), "Third row full_name should be Charlie");
            assertEquals(35, rs.getInt("years"), "Third row years should be 35");

            // There should be no more rows
            assertFalse(rs.next(), "Result set should have exactly three rows");

            // Verify that the table doesn't have a job column
            try {
                rs.findColumn("job");
                fail("Table should not have a job column");
            } catch (SQLException e) {
                // Expected exception
            }
        }
    }

    @Test
    void testInvalidConfiguration() {
        // Create a mapping configuration without tableName
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "id", "int"))
                .addColumnMapping(new ColumnMapping("Name", "full_name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "years", "int"));

        // Write data to the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.writeToDatabase(table, config);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("'tableName' must be specified"), 
                "Exception message should mention missing tableName");
    }

    @Test
    void testNullTable() {
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("ID", "id", "int"))
                .addColumnMapping(new ColumnMapping("Name", "full_name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "years", "int"));

        // Write data to the database with a null table should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.writeToDatabase(null, config);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Table cannot be null"), 
                "Exception message should mention null table");
    }

    @Test
    void testNullConfiguration() {
        // Write data to the database with a null configuration should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.writeToDatabase(table, null);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Mapping configuration cannot be null"), 
                "Exception message should mention null configuration");
    }
}
