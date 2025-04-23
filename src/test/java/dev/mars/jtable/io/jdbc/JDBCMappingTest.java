package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
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

class JDBCMappingTest extends AbstractDatabaseTest {

    private JDBCMappingReader mappingReader;
    private JDBCMappingWriter mappingWriter;
    private MappingConfiguration mappingConfig;
    private JDBCConnection jdbcConnection;

    @BeforeEach
    @Override
    void setUp() throws Exception {
        super.setUp();
        initializeDatabase();
        
        mappingReader = new JDBCMappingReader();
        mappingWriter = new JDBCMappingWriter();

        // Create a JDBC connection using the connection details from the parent class
        jdbcConnection = new JDBCConnection(
            connectionString,
            username,
            password
        );
        jdbcConnection.connect();

        // Create a basic mapping configuration
        mappingConfig = new MappingConfiguration()
                .setSourceLocation(connectionString)
                .setOption("username", username)
                .setOption("password", password)
                .setOption("tableName", testTableName);

        // Add column mappings
        mappingConfig.addColumnMapping(new ColumnMapping("NAME", "Name", "string"));
        mappingConfig.addColumnMapping(new ColumnMapping("AGE", "Age", "int"));
        mappingConfig.addColumnMapping(new ColumnMapping("OCCUPATION", "Job", "string"));
        mappingConfig.addColumnMapping(new ColumnMapping("SALARY", "Income", "double"));
        mappingConfig.addColumnMapping(new ColumnMapping("ISEMPLOYED", "Employed", "boolean"));
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
            statement.executeUpdate("DROP TABLE IF EXISTS mapped_table");
        }
    }

    @Test
    void testReadFromDatabase() throws SQLException {
        // Read data using the mapping reader
        mappingReader.readFromDatabase(table, mappingConfig);

        // Verify data
        assertEquals(5, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify column names (should be the target names from the mapping)
        assertEquals("Name", table.getColumnName(0));
        assertEquals("Age", table.getColumnName(1));
        assertEquals("Job", table.getColumnName(2));
        assertEquals("Income", table.getColumnName(3));
        assertEquals("Employed", table.getColumnName(4));

        // Verify first row
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Job"));
        assertEquals("75000.5", table.getValueAt(0, "Income"));
        assertEquals("true", table.getValueAt(0, "Employed").toLowerCase());
    }

    @Test
    void testWriteToDatabase() throws SQLException {
        // Set up a table with data
        Table sourceTable = new Table();

        // Add columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Job", "string");
        columns.put("Income", "double");
        columns.put("Employed", "boolean");
        sourceTable.setColumns(columns);

        // Add a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "John");
        row.put("Age", "35");
        row.put("Job", "Developer");
        row.put("Income", "85000.0");
        row.put("Employed", "true");
        sourceTable.addRow(row);

        // Create a mapping configuration for writing
        MappingConfiguration writeConfig = new MappingConfiguration()
                .setSourceLocation(connectionString)
                .setOption("username", username)
                .setOption("password", password)
                .setOption("tableName", "mapped_table")
                .setOption("createTable", true);

        // Add column mappings (source column name -> target DB column name)
        writeConfig.addColumnMapping(new ColumnMapping("Name", "EMPLOYEE_NAME", "string"));
        writeConfig.addColumnMapping(new ColumnMapping("Age", "EMPLOYEE_AGE", "int"));
        writeConfig.addColumnMapping(new ColumnMapping("Job", "POSITION", "string"));
        writeConfig.addColumnMapping(new ColumnMapping("Income", "SALARY", "double"));
        writeConfig.addColumnMapping(new ColumnMapping("Employed", "IS_ACTIVE", "boolean"));

        // Write the data
        mappingWriter.writeToDatabase(sourceTable, writeConfig);

        // Verify the data was written correctly and clean up
        try (Connection connection = (Connection) jdbcConnection.getRawConnection();
             Statement statement = connection.createStatement()) {

            // Verify the data
            try (ResultSet resultSet = statement.executeQuery("SELECT * FROM mapped_table")) {
                assertTrue(resultSet.next());
                assertEquals("John", resultSet.getString("EMPLOYEE_NAME"));
                assertEquals(35, resultSet.getInt("EMPLOYEE_AGE"));
                assertEquals("Developer", resultSet.getString("POSITION"));
                assertEquals(85000.0, resultSet.getDouble("SALARY"));
                assertEquals(true, resultSet.getBoolean("IS_ACTIVE"));

                // Should only be one row
                assertFalse(resultSet.next());
            }
        }
    }

    @Test
    void testQueryWithMapping() throws SQLException {
        // Create a mapping configuration with a query
        MappingConfiguration queryConfig = new MappingConfiguration()
                .setSourceLocation(connectionString)
                .setOption("username", username)
                .setOption("password", password)
                .setOption("query", "SELECT * FROM " + testTableName + " WHERE AGE > 25");

        // Add column mappings
        queryConfig.addColumnMapping(new ColumnMapping("NAME", "EmployeeName", "string"));
        queryConfig.addColumnMapping(new ColumnMapping("AGE", "EmployeeAge", "int"));

        // Read data using the mapping reader
        Table queryTable = new Table();
        mappingReader.readFromDatabase(queryTable, queryConfig);

        // Verify data
        assertEquals(3, queryTable.getRowCount()); // Only 3 people are older than 25
        assertEquals(2, queryTable.getColumnCount()); // Only mapped 2 columns

        // Verify column names
        assertEquals("EmployeeName", queryTable.getColumnName(0));
        assertEquals("EmployeeAge", queryTable.getColumnName(1));

        // Verify all ages are > 25
        for (int i = 0; i < queryTable.getRowCount(); i++) {
            int age = Integer.parseInt(queryTable.getValueAt(i, "EmployeeAge"));
            assertTrue(age > 25);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (jdbcConnection != null && jdbcConnection.isConnected()) {
            jdbcConnection.disconnect();
        }
        cleanUpDatabase();
    }
}