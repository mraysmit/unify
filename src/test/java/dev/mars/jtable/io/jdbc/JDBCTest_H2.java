package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.JDBCTableAdapter;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.datasource.IDataSourceConnection;

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

    // Connection details for H2 in-memory database
    String connectionString = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    String username = "";
    String password = "";

    @BeforeEach
    @Override
    void setUp() throws Exception {
        // Initialize the table, reader, and writer
        table = new Table();
        reader = new JDBCReader();
        writer = new JDBCWriter();


        // Initialize the adapter with the table
        adapter = new JDBCTableAdapter(table);

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
        // Create a JDBC connection
        IDataSourceConnection jdbcConnection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", testTableName);

        // Use the reader to read data from the database into the adapter
        reader.readData(adapter, jdbcConnection, options);

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
        assertEquals("true", table.getValueAt(0, "ISEMPLOYED"));

        assertEquals("Eve", table.getValueAt(4, "NAME"));
        assertEquals("22", table.getValueAt(4, "AGE"));
        assertEquals("Intern", table.getValueAt(4, "OCCUPATION"));
        assertEquals("45000.0", table.getValueAt(4, "SALARY"));
        assertEquals("false", table.getValueAt(4, "ISEMPLOYED"));
    }

    @Test
    void testReadFromQuery() {
        String query = "SELECT * FROM " + testTableName + " WHERE Age > 25";

        // Create a JDBC connection
        IDataSourceConnection jdbcConnection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("query", query);

        // Use the reader to read data from the database into the adapter
        reader.readData(adapter, jdbcConnection, options);

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

        // Create a new adapter with the table and connection parameters
        JDBCTableAdapter writeAdapter = new JDBCTableAdapter(table);

        // Create a JDBC connection
        IDataSourceConnection jdbcConnection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", newTableName);
        options.put("createTable", true);

        // Use the writer to write data to the database
        writer.writeData(writeAdapter, jdbcConnection, options);

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
        // Create a JDBC connection for reading
        IDataSourceConnection jdbcConnectionForReading = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map for reading
        Map<String, Object> readOptions = new HashMap<>();
        readOptions.put("tableName", testTableName);

        // Read data from the database using the reader
        reader.readData(adapter, jdbcConnectionForReading, readOptions);

        String newTableName = "round_trip_table";

        // Create a JDBC connection
        IDataSourceConnection jdbcConnection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", newTableName);
        options.put("createTable", true);

        // Write data to the database using the writer
        writer.writeData(adapter, jdbcConnection, options);

        // Create a new table and adapter for reading from the new table
        Table newTable = new Table();
        JDBCTableAdapter newAdapter = new JDBCTableAdapter(newTable);

        // Create a JDBC connection for reading from the new table
        IDataSourceConnection jdbcConnectionForNewTable = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map for reading from the new table
        Map<String, Object> readNewTableOptions = new HashMap<>();
        readNewTableOptions.put("tableName", newTableName);

        // Read data from the new table using the reader
        reader.readData(newAdapter, jdbcConnectionForNewTable, readNewTableOptions);

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

        // Create a new adapter with the table
        JDBCTableAdapter batchAdapter = new JDBCTableAdapter(table);

        String sqlTemplate = "INSERT INTO " + batchTableName + " (Name, Age) VALUES (:Name, :Age)";

        // Create a JDBC connection
        IDataSourceConnection jdbcConnection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("sqlTemplate", sqlTemplate);

        // Use the writer to execute the batch with the adapter as the data source
        writer.writeData(batchAdapter, jdbcConnection, options);

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
