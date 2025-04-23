package dev.mars.jtable.io.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PostgreSQL database configuration that implements IJDBCDataSource.
 * This class provides connection details and methods to initialize and clean up the database.
 */
public class DatabaseTestConfig_PG implements IJDBCDataSource {

    private final String connectionString = "jdbc:postgresql://localhost/testdb";
    private final String username = "postgres_user";
    private final String password = "postgres_password";
    private final String testTableName = "test_table";

    private LinkedHashMap<String, String> columns = new LinkedHashMap<>();
    private Map<Integer, Map<String, String>> rows = new HashMap<>();
    private int rowCount = 0;

    public DatabaseTestConfig_PG() {
        // Initialize columns
        columns.put("name", "string");
        columns.put("age", "int");
        columns.put("occupation", "string");
        columns.put("salary", "double");
        columns.put("isemployed", "boolean");
    }

    /**
     * Gets the connection string for the PostgreSQL database.
     *
     * @return the connection string
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Gets the username for the PostgreSQL database.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the password for the PostgreSQL database.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Initializes the database by creating the test table and inserting test data.
     *
     * @throws SQLException if there is an error initializing the database
     */
    public void initializeDatabase() throws SQLException {
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

            // Load data into this data source
            loadDataFromDatabase();
        }
    }

    /**
     * Cleans up the database by dropping the test table.
     *
     * @throws SQLException if there is an error cleaning up the database
     */
    public void cleanUpDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS " + testTableName);
        }
    }

    /**
     * Loads data from the database into this data source.
     *
     * @throws SQLException if there is an error loading data from the database
     */
    private void loadDataFromDatabase() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + testTableName)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Clear existing data
            rows.clear();
            rowCount = 0;

            // Process all rows in the result set
            while (resultSet.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i).toLowerCase();
                    String value = resultSet.getString(i);
                    row.put(columnName, value != null ? value : "");
                }
                rows.put(rowCount++, row);
            }
        }
    }

    // IJDBCDataSource implementation

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("Column index out of bounds: " + index);
        }
        return (String) columns.keySet().toArray()[index];
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + rowIndex);
        }
        Map<String, String> row = rows.get(rowIndex);
        if (row == null) {
            return "";
        }
        String value = row.get(columnName.toLowerCase());
        return value != null ? value : "";
    }

    @Override
    public String inferType(String value) {
        if (value == null) {
            return "string";
        }

        try {
            Integer.parseInt(value);
            return "int";
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                return "double";
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    return "boolean";
                } else {
                    return "string";
                }
            }
        }
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> columns) {
        this.columns = new LinkedHashMap<>(columns);
    }

    @Override
    public void addRow(Map<String, String> row) {
        Map<String, String> newRow = new HashMap<>();
        for (Map.Entry<String, String> entry : row.entrySet()) {
            newRow.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        rows.put(rowCount++, newRow);
    }
}
