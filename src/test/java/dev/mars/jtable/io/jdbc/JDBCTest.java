package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.JDBCTableAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBC reading and writing functionality.
 * This class tests the JDBCReader and JDBCWriter classes using an embedded H2 database.
 */
class JDBCTest {

    private Table table;
    private JDBCTableAdapter adapter;
    private JDBCReader reader;
    private JDBCWriter writer;
    private final String connectionString = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private final String username = "";
    private final String password = "";
    private final String testTableName = "test_table";

    @BeforeEach
    void setUp() throws SQLException {
        // Create a new Table instance for each test
        table = new Table();

        // Create the adapter with the table
        adapter = new JDBCTableAdapter(table);

        // Create reader and writer instances
        reader = new JDBCReader();
        writer = new JDBCWriter();

        // Set up the test database
        setupTestDatabase();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean up the test database
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + testTableName);
        }
    }

    /**
     * Sets up the test database with sample data.
     */
    private void setupTestDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            // Create the test table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + testTableName + " (" +
                    "Name VARCHAR(255), " +
                    "Age INTEGER, " +
                    "Occupation VARCHAR(255), " +
                    "Salary DOUBLE PRECISION, " +
                    "IsEmployed BOOLEAN)");

            // Insert sample data
            statement.executeUpdate("INSERT INTO " + testTableName + " VALUES " +
                    "('Alice', 30, 'Engineer', 75000.50, TRUE), " +
                    "('Bob', 25, 'Designer', 65000.75, TRUE), " +
                    "('Charlie', 35, 'Manager', 85000.25, TRUE), " +
                    "('David', 28, 'Developer', 72000.00, TRUE), " +
                    "('Eve', 22, 'Intern', 45000.00, FALSE)");
        }
    }

    @Test
    void testReadFromDatabase() {
        // Read data from the test database table
        reader.readFromDatabase(adapter, connectionString, testTableName, username, password);

        // Verify that the data was read correctly
        assertEquals(5, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify column names
        assertEquals("NAME", table.getColumnName(0));
        assertEquals("AGE", table.getColumnName(1));
        assertEquals("OCCUPATION", table.getColumnName(2));
        assertEquals("SALARY", table.getColumnName(3));
        assertEquals("ISEMPLOYED", table.getColumnName(4));

        // Verify first row values
        assertEquals("Alice", table.getValueAt(0, "NAME"));
        assertEquals("30", table.getValueAt(0, "AGE"));
        assertEquals("Engineer", table.getValueAt(0, "OCCUPATION"));
        assertEquals("75000.5", table.getValueAt(0, "SALARY"));
        assertEquals("true", table.getValueAt(0, "ISEMPLOYED"));

        // Verify last row values
        assertEquals("Eve", table.getValueAt(4, "NAME"));
        assertEquals("22", table.getValueAt(4, "AGE"));
        assertEquals("Intern", table.getValueAt(4, "OCCUPATION"));
        assertEquals("45000.0", table.getValueAt(4, "SALARY"));
        assertEquals("false", table.getValueAt(4, "ISEMPLOYED"));
    }

    @Test
    void testReadFromQuery() {
        // Read data from a SQL query
        String query = "SELECT * FROM " + testTableName + " WHERE Age > 25";
        reader.readFromQuery(adapter, connectionString, query, username, password);

        // Verify that the data was read correctly
        assertEquals(3, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify that only rows with Age > 25 were read
        for (int i = 0; i < table.getRowCount(); i++) {
            int age = Integer.parseInt(table.getValueAt(i, "AGE"));
            assertTrue(age > 25);
        }
    }

    @Test
    void testWriteToDatabase() throws SQLException {
        // Set up a table with data
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        columns.put("Salary", "double");
        columns.put("IsEmployed", "boolean");
        table.setColumns(columns);

        // Add rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Frank");
        row1.put("Age", "40");
        row1.put("Occupation", "CEO");
        row1.put("Salary", "100000.00");
        row1.put("IsEmployed", "true");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Grace");
        row2.put("Age", "35");
        row2.put("Occupation", "CTO");
        row2.put("Salary", "95000.00");
        row2.put("IsEmployed", "true");
        table.addRow(row2);

        // Write the table to a new database table
        String newTableName = "new_table";
        writer.writeToDatabase(adapter, connectionString, newTableName, username, password, true);

        // Verify that the data was written correctly
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + newTableName)) {
            resultSet.next();
            assertEquals(2, resultSet.getInt(1));
        }

        // Clean up the new table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + newTableName);
        }
    }

    @Test
    void testRoundTrip() throws SQLException {
        // Read data from the test database table
        reader.readFromDatabase(adapter, connectionString, testTableName, username, password);

        // Write the data to a new database table
        String newTableName = "round_trip_table";
        writer.writeToDatabase(adapter, connectionString, newTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the new database table
        reader.readFromDatabase(newAdapter, connectionString, newTableName, username, password);

        // Verify that the data is the same
        assertEquals(table.getRowCount(), newTable.getRowCount());
        assertEquals(table.getColumnCount(), newTable.getColumnCount());

        // Verify all values
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                String columnName = table.getColumnName(j);
                assertEquals(table.getValueAt(i, columnName), newTable.getValueAt(i, columnName));
            }
        }

        // Clean up the new table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + newTableName);
        }
    }

    @Test
    void testExecuteBatch() throws SQLException {
        // Set up a table with data
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        table.setColumns(columns);

        // Add rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Frank");
        row1.put("Age", "40");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Grace");
        row2.put("Age", "35");
        table.addRow(row2);

        // Create a new table for batch operations
        String batchTableName = "batch_table";
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + batchTableName + " (Name VARCHAR(255), Age INTEGER)");
        }

        // Execute a batch of SQL statements
        String sqlTemplate = "INSERT INTO " + batchTableName + " (Name, Age) VALUES (:Name, :Age)";
        writer.executeBatch(adapter, connectionString, sqlTemplate, username, password);

        // Verify that the data was written correctly
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + batchTableName)) {
            resultSet.next();
            assertEquals(2, resultSet.getInt(1));
        }

        // Clean up the batch table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + batchTableName);
        }
    }

    @Test
    void testEmptyTable() throws SQLException {
        // Create an empty table
        Table emptyTable = new Table();
        JDBCTableAdapter emptyAdapter = new JDBCTableAdapter(emptyTable);

        // Set up columns but don't add any rows
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        emptyTable.setColumns(columns);

        // Write the empty table to a database table
        String emptyTableName = "empty_table";
        writer.writeToDatabase(emptyAdapter, connectionString, emptyTableName, username, password, true);

        // Verify that the table was created but is empty
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + emptyTableName)) {
            resultSet.next();
            assertEquals(0, resultSet.getInt(1));
        }

        // Clean up the empty table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + emptyTableName);
        }
    }

    @Test
    void testSpecialCharacters() throws SQLException {
        // Set up a table with special characters
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Description", "string");
        table.setColumns(columns);

        // Add a row with special characters
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Special \"Quoted\" Name");
        row.put("Description", "Contains special characters: ' \" \\ % _");
        table.addRow(row);

        // Write the table to a database table
        String specialTableName = "special_table";
        writer.writeToDatabase(adapter, connectionString, specialTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the database table
        reader.readFromDatabase(newAdapter, connectionString, specialTableName, username, password);

        // Verify that the special characters were preserved
        assertEquals("Special \"Quoted\" Name", newTable.getValueAt(0, "NAME"));
        assertEquals("Contains special characters: ' \" \\ % _", newTable.getValueAt(0, "DESCRIPTION"));

        // Clean up the special table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + specialTableName);
        }
    }

    @Test
    void testSingleRowTable() throws SQLException {
        // Set up a table with a single row
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        table.setColumns(columns);

        // Add a single row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Charlie");
        row.put("Age", "40");
        row.put("Occupation", "Artist");
        table.addRow(row);

        // Write the table to a database table
        String singleRowTableName = "single_row_table";
        writer.writeToDatabase(adapter, connectionString, singleRowTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the database table
        reader.readFromDatabase(newAdapter, connectionString, singleRowTableName, username, password);

        // Verify that the data was read correctly
        assertEquals(1, newTable.getRowCount());
        assertEquals(3, newTable.getColumnCount());
        assertEquals("Charlie", newTable.getValueAt(0, "NAME"));
        assertEquals("40", newTable.getValueAt(0, "AGE"));
        assertEquals("Artist", newTable.getValueAt(0, "OCCUPATION"));

        // Clean up the single row table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + singleRowTableName);
        }
    }

    @Test
    void testSingleColumnTable() throws SQLException {
        // Set up a table with a single column
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        table.setColumns(columns);

        // Add rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        table.addRow(row2);

        // Write the table to a database table
        String singleColumnTableName = "single_column_table";
        writer.writeToDatabase(adapter, connectionString, singleColumnTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the database table
        reader.readFromDatabase(newAdapter, connectionString, singleColumnTableName, username, password);

        // Verify that the data was read correctly
        assertEquals(2, newTable.getRowCount());
        assertEquals(1, newTable.getColumnCount());
        assertEquals("Alice", newTable.getValueAt(0, "NAME"));
        assertEquals("Bob", newTable.getValueAt(1, "NAME"));

        // Clean up the single column table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + singleColumnTableName);
        }
    }

    @Test
    void testEmptyValues() throws SQLException {
        // Set up a table with empty values
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        table.setColumns(columns);

        // Add rows with empty values
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", ""); // Empty value
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        table.addRow(row2);

        // Write the table to a database table
        String emptyValuesTableName = "empty_values_table";
        writer.writeToDatabase(adapter, connectionString, emptyValuesTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the database table
        reader.readFromDatabase(newAdapter, connectionString, emptyValuesTableName, username, password);

        // Verify that the empty values were preserved
        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "NAME"));
        assertEquals("30", newTable.getValueAt(0, "AGE"));
        assertEquals("", newTable.getValueAt(0, "OCCUPATION"));
        assertEquals("", newTable.getValueAt(1, "NAME"));
        assertEquals("25", newTable.getValueAt(1, "AGE"));
        assertEquals("Designer", newTable.getValueAt(1, "OCCUPATION"));

        // Clean up the empty values table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + emptyValuesTableName);
        }
    }

    @Test
    void testNumericAndDecimalValues() throws SQLException {
        // Set up a table with numeric and decimal values
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("IntValue", "int");
        columns.put("DoubleValue", "double");
        columns.put("BooleanValue", "boolean");
        table.setColumns(columns);

        // Add rows with various numeric values
        Map<String, String> row1 = new HashMap<>();
        row1.put("IntValue", "123");
        row1.put("DoubleValue", "456.78");
        row1.put("BooleanValue", "true");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("IntValue", "-987");
        row2.put("DoubleValue", "-654.32");
        row2.put("BooleanValue", "false");
        table.addRow(row2);

        // Write the table to a database table
        String numericTableName = "numeric_table";
        writer.writeToDatabase(adapter, connectionString, numericTableName, username, password, true);

        // Create a new table and adapter
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Read the data back from the database table
        reader.readFromDatabase(newAdapter, connectionString, numericTableName, username, password);

        // Verify that the numeric values were preserved
        assertEquals(2, newTable.getRowCount());
        assertEquals(3, newTable.getColumnCount());

        assertEquals("123", newTable.getValueAt(0, "INTVALUE"));
        assertEquals("456.78", newTable.getValueAt(0, "DOUBLEVALUE"));
        assertEquals("true", newTable.getValueAt(0, "BOOLEANVALUE"));

        assertEquals("-987", newTable.getValueAt(1, "INTVALUE"));
        assertEquals("-654.32", newTable.getValueAt(1, "DOUBLEVALUE"));
        assertEquals("false", newTable.getValueAt(1, "BOOLEANVALUE"));

        // Clean up the numeric table
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + numericTableName);
        }
    }
}
