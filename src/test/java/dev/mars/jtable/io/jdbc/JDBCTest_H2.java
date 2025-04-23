package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.JDBCTableAdapter;

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

class JDBCTest_H2 extends AbstractDatabaseTest {

    @BeforeEach
    @Override
    void setUp() throws Exception {
        super.setUp();
        initializeDatabase();
    }

    @AfterEach
    void tearDown() throws Exception {
        cleanUpDatabase();
    }

    private void initializeDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + testTableName + " (" +
                    "Name VARCHAR(255), Age INTEGER, Occupation VARCHAR(255), Salary DOUBLE PRECISION, IsEmployed BOOLEAN)");

            statement.executeUpdate("INSERT INTO " + testTableName + " VALUES " +
                    "('Alice', 30, 'Engineer', 75000.50, TRUE), " +
                    "('Bob', 25, 'Designer', 65000.75, TRUE), " +
                    "('Charlie', 35, 'Manager', 85000.25, TRUE), " +
                    "('David', 28, 'Developer', 72000.00, TRUE), " +
                    "('Eve', 22, 'Intern', 45000.00, FALSE)");
        }
    }

    private void cleanUpDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + testTableName);
        }
    }


    @Test
    void testReadFromDatabase() {
        reader.readFromDatabase(adapter, connectionString, testTableName, username, password);

        // Verify data
        assertEquals(5, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify column names
        assertEquals("NAME", table.getColumnName(0));
        assertEquals("AGE", table.getColumnName(1));
        assertEquals("OCCUPATION", table.getColumnName(2));
        assertEquals("SALARY", table.getColumnName(3));
        assertEquals("ISEMPLOYED", table.getColumnName(4));

        // Verify first and last rows
        assertEquals("Alice", table.getValueAt(0, "NAME"));
        assertEquals("30", table.getValueAt(0, "AGE"));
        assertEquals("Engineer", table.getValueAt(0, "OCCUPATION"));
        assertEquals("75000.5", table.getValueAt(0, "SALARY"));
        assertEquals("TRUE", table.getValueAt(0, "ISEMPLOYED"));

        assertEquals("Eve", table.getValueAt(4, "NAME"));
        assertEquals("22", table.getValueAt(4, "AGE"));
        assertEquals("Intern", table.getValueAt(4, "OCCUPATION"));
        assertEquals("45000.0", table.getValueAt(4, "SALARY"));
        assertEquals("FALSE", table.getValueAt(4, "ISEMPLOYED"));
    }

    @Test
    void testReadFromQuery() {
        String query = "SELECT * FROM " + testTableName + " WHERE Age > 25";
        reader.readFromQuery(adapter, connectionString, query, username, password);

        // Verify data
        assertEquals(3, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        for (int i = 0; i < table.getRowCount(); i++) {
            int age = Integer.parseInt(table.getValueAt(i, "AGE"));
            assertTrue(age > 25);
        }
    }

    @Test
    void testWriteToDatabase() throws SQLException {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        columns.put("Salary", "double");
        columns.put("IsEmployed", "boolean");
        table.setColumns(columns);

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

        String newTableName = "new_table";
        writer.writeToDatabase(adapter, connectionString, newTableName, username, password, true);

        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + newTableName)) {
            resultSet.next();
            assertEquals(2, resultSet.getInt(1));
        }

        // Clean up
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + newTableName);
        }
    }

    @Test
    void testRoundTrip() throws SQLException {
        reader.readFromDatabase(adapter, connectionString, testTableName, username, password);

        String newTableName = "round_trip_table";
        writer.writeToDatabase(adapter, connectionString, newTableName, username, password, true);

        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        reader.readFromDatabase(newAdapter, connectionString, newTableName, username, password);

        assertEquals(table.getRowCount(), newTable.getRowCount());
        assertEquals(table.getColumnCount(), newTable.getColumnCount());

        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                String columnName = table.getColumnName(j);
                assertEquals(table.getValueAt(i, columnName), newTable.getValueAt(i, columnName));
            }
        }

        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + newTableName);
        }
    }

    @Test
    void testExecuteBatch() throws SQLException {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        table.setColumns(columns);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Frank");
        row1.put("Age", "40");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Grace");
        row2.put("Age", "35");
        table.addRow(row2);

        String batchTableName = "batch_table";
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + batchTableName + " (Name VARCHAR(255), Age INTEGER)");
        }

        String sqlTemplate = "INSERT INTO " + batchTableName + " (Name, Age) VALUES (:Name, :Age)";
        writer.executeBatch(adapter, connectionString, sqlTemplate, username, password);

        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + batchTableName)) {
            resultSet.next();
            assertEquals(2, resultSet.getInt(1));
        }

        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + batchTableName);
        }
    }


}
