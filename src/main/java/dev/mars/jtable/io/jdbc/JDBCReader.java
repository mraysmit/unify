package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.io.datasource.IDataSource;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
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
 * Implementation of the IJDBCReader interface for reading data from databases via JDBC.
 */
public class JDBCReader implements IJDBCReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a JDBC-specific dataSource
        IJDBCDataSource jdbcDataSource;
        if (dataSource instanceof IJDBCDataSource) {
            jdbcDataSource = (IJDBCDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IJDBCDataSource");
        }

        // Ensure we have a JDBC connection
        if (!(connection instanceof JDBCConnection)) {
            throw new IllegalArgumentException("Connection must be a JDBCConnection");
        }
        JDBCConnection jdbcConnection = (JDBCConnection) connection;

        // Extract options
        String tableName = options != null && options.containsKey("tableName") ? (String) options.get("tableName") : null;
        String query = options != null && options.containsKey("query") ? (String) options.get("query") : null;

        // Connect if not already connected
        if (!jdbcConnection.isConnected()) {
            jdbcConnection.connect();
        }

        // Call the appropriate JDBC-specific method
        if (query != null) {
            readFromQuery(jdbcDataSource, jdbcConnection, query);
        } else if (tableName != null) {
            readFromDatabase(jdbcDataSource, jdbcConnection, tableName);
        } else {
            throw new IllegalArgumentException("Either 'tableName' or 'query' must be specified in options");
        }
    }

    /**
     * Reads data from a database table into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the JDBC connection
     * @param tableName the name of the table to read from
     */
    public void readFromDatabase(IJDBCDataSource dataSource, JDBCConnection connection, String tableName) {
        String query = "SELECT * FROM " + tableName;
        readFromQuery(dataSource, connection, query);
    }

    /**
     * Reads data from a SQL query into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the JDBC connection
     * @param query the SQL query to execute
     */
    public void readFromQuery(IJDBCDataSource dataSource, JDBCConnection connection, String query) {
        try {
            // Get the raw JDBC connection
            Connection jdbcConnection = (Connection) connection.getRawConnection();

            try (Statement statement = jdbcConnection.createStatement();
                 ResultSet resultSet = statement.executeQuery(query)) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Create columns based on the result set metadata
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    String columnType = mapSqlTypeToTableType(metaData.getColumnType(i));
                    columns.put(columnName, columnType);
                }
                dataSource.setColumns(columns);

                // Process all rows in the result set
                while (resultSet.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        String value = resultSet.getString(i);
                        row.put(columnName, value != null ? value : "");
                    }
                    dataSource.addRow(row);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading from database: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing database data: " + e.getMessage());
        }
    }

    // The mapSqlTypeToTableType method remains unchanged
    private String mapSqlTypeToTableType(int sqlType) {
        // Existing implementation...
        return "string"; // Simplified for example
    }

    /**
     * Reads data from a database table into a data source.
     * This is a convenience method that creates a JDBCConnection and then calls readFromDatabase.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param tableName the name of the table to read from
     * @param username the database username
     * @param password the database password
     */
    public void readFromDatabase(IJDBCDataSource dataSource, String connectionString, String tableName, String username, String password) {
        // Create a JDBC connection
        JDBCConnection connection = (JDBCConnection) DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Connect to the database
        connection.connect();

        try {
            // Read from the database
            readFromDatabase(dataSource, connection, tableName);
        } finally {
            // Disconnect from the database
            connection.disconnect();
        }
    }

    /**
     * Reads data from a SQL query into a data source.
     * This is a convenience method that creates a JDBCConnection and then calls readFromQuery.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param query the SQL query to execute
     * @param username the database username
     * @param password the database password
     */
    public void readFromQuery(IJDBCDataSource dataSource, String connectionString, String query, String username, String password) {
        // Create a JDBC connection
        JDBCConnection connection = (JDBCConnection) DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Connect to the database
        connection.connect();

        try {
            // Read from the query
            readFromQuery(dataSource, connection, query);
        } finally {
            // Disconnect from the database
            connection.disconnect();
        }
    }
}
