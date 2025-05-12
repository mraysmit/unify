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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBCReader.
 * This class tests reading data from a database using JDBCReader.
 */
class JDBCReaderTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String TEST_TABLE = "test_table";
    private JDBCTableAdapter adapter;
    private JDBCReader reader;
    private JDBCConnection connection;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new Table instance for each test
        Table table = new Table();

        // Create the adapter with the table
        adapter = new JDBCTableAdapter(table);

        // Create the reader
        reader = new JDBCReader();

        // Create the connection
        connection = new JDBCConnection(TEST_DB_URL, "", "");
        assertTrue(connection.connect(), "Connection should be established");

        // Set up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create a test table
            stmt.execute("CREATE TABLE " + TEST_TABLE + " (id INT, name VARCHAR(255), age INT)");

            // Insert test data
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (1, 'Alice', 30)");
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (2, 'Bob', 25)");
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (3, 'Charlie', 35)");
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
    void testReadFromDatabase() {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", TEST_TABLE);

        // Read data from the database
        reader.readData((IJDBCDataSource) adapter, connection, options);

        // Verify the table content
        assertEquals(3, adapter.getRowCount(), "Table should have 3 rows");
        assertEquals(3, adapter.getColumnCount(), "Table should have 3 columns");

        // Check column names (H2 database converts column names to uppercase)
        assertEquals("ID", adapter.getColumnName(0), "First column should be 'ID'");
        assertEquals("NAME", adapter.getColumnName(1), "Second column should be 'NAME'");
        assertEquals("AGE", adapter.getColumnName(2), "Third column should be 'AGE'");

        // Check row values
        assertEquals("1", adapter.getValueAt(0, "ID"), "First row, ID column should be '1'");
        assertEquals("Alice", adapter.getValueAt(0, "NAME"), "First row, NAME column should be 'Alice'");
        assertEquals("30", adapter.getValueAt(0, "AGE"), "First row, AGE column should be '30'");

        assertEquals("2", adapter.getValueAt(1, "ID"), "Second row, ID column should be '2'");
        assertEquals("Bob", adapter.getValueAt(1, "NAME"), "Second row, NAME column should be 'Bob'");
        assertEquals("25", adapter.getValueAt(1, "AGE"), "Second row, AGE column should be '25'");

        assertEquals("3", adapter.getValueAt(2, "ID"), "Third row, ID column should be '3'");
        assertEquals("Charlie", adapter.getValueAt(2, "NAME"), "Third row, NAME column should be 'Charlie'");
        assertEquals("35", adapter.getValueAt(2, "AGE"), "Third row, AGE column should be '35'");
    }

    @Test
    void testReadFromQuery() {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("query", "SELECT * FROM " + TEST_TABLE + " WHERE age > 25");

        // Read data from the database
        reader.readData((IJDBCDataSource) adapter, connection, options);

        // Verify the table content
        assertEquals(2, adapter.getRowCount(), "Table should have 2 rows");
        assertEquals(3, adapter.getColumnCount(), "Table should have 3 columns");

        // Check row values for Alice (age 30)
        boolean foundAlice = false;
        boolean foundCharlie = false;

        for (int i = 0; i < adapter.getRowCount(); i++) {
            String name = adapter.getValueAt(i, "NAME");
            String age = adapter.getValueAt(i, "AGE");

            if ("Alice".equals(name)) {
                foundAlice = true;
                assertEquals("30", age, "Alice's age should be '30'");
            } else if ("Charlie".equals(name)) {
                foundCharlie = true;
                assertEquals("35", age, "Charlie's age should be '35'");
            }
        }

        assertTrue(foundAlice, "Alice should be in the result set");
        assertTrue(foundCharlie, "Charlie should be in the result set");
    }

    @Test
    void testInvalidOptions() {
        // Set up options without tableName or query
        Map<String, Object> options = new HashMap<>();

        // Read data from the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reader.readData((IJDBCDataSource) adapter, connection, options);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Either 'tableName' or 'query' must be specified"), 
                "Exception message should mention missing tableName or query");
    }

    @Test
    void testInvalidDataSource() {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", TEST_TABLE);

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

        // Read data from the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reader.readData(nonJdbcDataSource, connection, options);
        });

        // Verify the exception message
        assertTrue(exception.getMessage().contains("Data source must implement IJDBCDataSource"), 
                "Exception message should mention IJDBCDataSource requirement");
    }
}
