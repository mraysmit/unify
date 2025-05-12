package dev.mars.jtable.io.files.jsbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;
import dev.mars.jtable.io.common.datasource.JDBCConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBCWriter.
 * This class tests writing data to a database using JDBCWriter.
 */
class JDBCWriterTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String TEST_TABLE = "test_table";
    private JDBCTableAdapter adapter;
    private JDBCWriter writer;
    private JDBCConnection connection;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new Table instance for each test
        Table table = new Table();

        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("name", "string");
        columns.put("age", "int");
        table.setColumns(columns);

        // Add some rows to the table
        Map<String, String> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "Alice");
        row1.put("age", "30");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("id", "2");
        row2.put("name", "Bob");
        row2.put("age", "25");
        table.addRow(row2);

        Map<String, String> row3 = new HashMap<>();
        row3.put("id", "3");
        row3.put("name", "Charlie");
        row3.put("age", "35");
        table.addRow(row3);

        // Create the adapter with the table
        adapter = new JDBCTableAdapter(table);

        // Create the writer
        writer = new JDBCWriter();

        // Create the connection
        connection = new JDBCConnection(TEST_DB_URL, "", "");
        assertTrue(connection.connect(), "Connection should be established");

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

        // Disconnect
        connection.disconnect();
    }

    @Test
    void testWriteToDatabase() throws Exception {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", TEST_TABLE);
        options.put("createTable", true);

        // Write data to the database
        writer.writeData((IJDBCDataSource) adapter, connection, options);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " ORDER BY id")) {

            // Check the first row
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("id"), "First row id should be 1");
            assertEquals("Alice", rs.getString("name"), "First row name should be Alice");
            assertEquals(30, rs.getInt("age"), "First row age should be 30");

            // Check the second row
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(2, rs.getInt("id"), "Second row id should be 2");
            assertEquals("Bob", rs.getString("name"), "Second row name should be Bob");
            assertEquals(25, rs.getInt("age"), "Second row age should be 25");

            // Check the third row
            assertTrue(rs.next(), "Result set should have at least three rows");
            assertEquals(3, rs.getInt("id"), "Third row id should be 3");
            assertEquals("Charlie", rs.getString("name"), "Third row name should be Charlie");
            assertEquals(35, rs.getInt("age"), "Third row age should be 35");

            // There should be no more rows
            assertFalse(rs.next(), "Result set should have exactly three rows");
        }
    }

    @Test
    void testExecuteBatch() throws Exception {
        // Set up the test database with a table
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE " + TEST_TABLE + " (id INT, name VARCHAR(255), age INT)");
        }

        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("sqlTemplate", "INSERT INTO " + TEST_TABLE + " (id, name, age) VALUES (:id, :name, :age)");

        // Write data to the database
        writer.writeData((IJDBCDataSource) adapter, connection, options);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " ORDER BY id")) {

            // Check the first row
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("id"), "First row id should be 1");
            assertEquals("Alice", rs.getString("name"), "First row name should be Alice");
            assertEquals(30, rs.getInt("age"), "First row age should be 30");

            // Check the second row
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(2, rs.getInt("id"), "Second row id should be 2");
            assertEquals("Bob", rs.getString("name"), "Second row name should be Bob");
            assertEquals(25, rs.getInt("age"), "Second row age should be 25");

            // Check the third row
            assertTrue(rs.next(), "Result set should have at least three rows");
            assertEquals(3, rs.getInt("id"), "Third row id should be 3");
            assertEquals("Charlie", rs.getString("name"), "Third row name should be Charlie");
            assertEquals(35, rs.getInt("age"), "Third row age should be 35");

            // There should be no more rows
            assertFalse(rs.next(), "Result set should have exactly three rows");
        }
    }

    @Test
    void testInvalidOptions() {
        // Set up options without tableName or sqlTemplate
        Map<String, Object> options = new HashMap<>();

        // Write data to the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.writeData((IJDBCDataSource) adapter, connection, options);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Either 'tableName' or 'sqlTemplate' must be specified"), 
                "Exception message should mention missing tableName or sqlTemplate");
    }

    @Test
    void testInvalidDataSource() {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", TEST_TABLE);
        options.put("createTable", true);

        // Create a non-JDBC data source
        IDataSource nonJdbcDataSource = new IDataSource() {
            @Override
            public int getRowCount() {
                return 0;
            }

            @Override
            public int getColumnCount() {
                return 0;
            }

            @Override
            public String getColumnName(int columnIndex) {
                return null;
            }

            @Override
            public String getValueAt(int rowIndex, String columnName) {
                return null;
            }

            @Override
            public String inferType(String value) {
                return "string";
            }

            @Override
            public void setColumns(java.util.LinkedHashMap<String, String> columns) {
            }

            @Override
            public void addRow(Map<String, String> row) {
            }
        };

        // Write data to the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            writer.writeData(nonJdbcDataSource, connection, options);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Data source must implement IJDBCDataSource"), 
                "Exception message should mention IJDBCDataSource requirement");
    }
}
