package dev.mars.jtable.io.files.jdbc;



import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;
import dev.mars.jtable.io.common.datasource.JDBCConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the IJDBCWriter interface for writing data to databases via JDBC.
 */
public class JDBCWriter implements IJDBCWriter {

    /**
     * Writes data from a JDBCTableAdapter to a database table.
     * This is a convenience method that uses the adapter's connection information.
     *
     * @param adapter the JDBCTableAdapter to write from
     * @param tableName the name of the table to write to
     * @param createTable whether to create the table if it doesn't exist
     * @throws IllegalArgumentException if the adapter's connection information is incomplete
     */
    public void writeToDatabase(JDBCTableAdapter adapter, String tableName, boolean createTable) {
        if (adapter.getConnectionString() == null) {
            throw new IllegalArgumentException("JDBCTableAdapter must have a connection string");
        }

        // Create a JDBC connection
        JDBCConnection jdbcConnection = new JDBCConnection(
                adapter.getConnectionString(),
                adapter.getUsername() != null ? adapter.getUsername() : "",
                adapter.getPassword() != null ? adapter.getPassword() : "");

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", tableName);
        options.put("createTable", createTable);

        // Write data using the new method
        writeData(adapter, jdbcConnection, options);
    }

    /**
     * Executes a batch of SQL statements using data from a JDBCTableAdapter.
     * This is a convenience method that uses the adapter's connection information.
     *
     * @param adapter the JDBCTableAdapter to get data from
     * @param sqlTemplate the SQL template to use for each row
     * @throws IllegalArgumentException if the adapter's connection information is incomplete
     */
    public void executeBatch(JDBCTableAdapter adapter, String sqlTemplate) {
        if (adapter.getConnectionString() == null) {
            throw new IllegalArgumentException("JDBCTableAdapter must have a connection string");
        }

        // Create a JDBC connection
        JDBCConnection jdbcConnection = new JDBCConnection(
                adapter.getConnectionString(),
                adapter.getUsername() != null ? adapter.getUsername() : "",
                adapter.getPassword() != null ? adapter.getPassword() : "");

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("sqlTemplate", sqlTemplate);

        // Write data using the new method
        writeData(adapter, jdbcConnection, options);
    }

    @Override
    public void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Check if dataSource is a JDBC data source
        if (dataSource instanceof IJDBCDataSource) {
            writeData((IJDBCDataSource) dataSource, connection, options);
        } else {
            throw new IllegalArgumentException("Data source must implement IJDBCDataSource");
        }
    }

    /**
     * Writes data from a data source to a destination using the provided connection.
     * This method is part of the IDataWriter interface.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IJDBCDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Ensure we have a JDBC connection
        if (!(connection instanceof JDBCConnection)) {
            throw new IllegalArgumentException("Connection must be a JDBCConnection");
        }
        JDBCConnection jdbcConnection = (JDBCConnection) connection;

        // Extract common options
        String tableName = null;
        String sqlTemplate = null;
        String username = "";
        String password = "";
        boolean createTable = false;

        // Get options from the map
        if (options != null) {
            tableName = (String) options.getOrDefault("tableName", null);
            sqlTemplate = (String) options.getOrDefault("sqlTemplate", null);
            username = (String) options.getOrDefault("username", "");
            password = (String) options.getOrDefault("password", "");
            createTable = (Boolean) options.getOrDefault("createTable", false);
        }

        // If dataSource is a JDBCTableAdapter, use its properties if not specified in options
        if (dataSource instanceof JDBCTableAdapter) {
            JDBCTableAdapter adapter = (JDBCTableAdapter) dataSource;

            // Use adapter properties as fallbacks
            if (tableName == null) {
                tableName = adapter.getTableName();
            }

            // For JDBCTableAdapter, we can use the convenience methods if we have all adapter properties
            if (adapter.getConnectionString() != null) {
                if (sqlTemplate != null) {
                    executeBatch(adapter, sqlTemplate);
                    return;
                } else if (tableName != null) {
                    writeToDatabase(adapter, tableName, createTable);
                    return;
                }
            }

            // If we couldn't use the convenience methods, fall back to using the adapter's connection info
            if (username.isEmpty()) {
                username = adapter.getUsername();
            }
            if (password.isEmpty()) {
                password = adapter.getPassword();
            }
        }

        // Get the connection string
        String connectionString = jdbcConnection.getConnectionString();

        // Execute the appropriate action based on the options
        if (sqlTemplate != null) {
            // Execute a batch of SQL statements
            try (Connection sqlConnection = DriverManager.getConnection(connectionString, username, password);
                 Statement statement = sqlConnection.createStatement()) {

                // Execute the SQL template for each row
                for (int i = 0; i < dataSource.getRowCount(); i++) {
                    String sql = sqlTemplate;
                    for (int j = 0; j < dataSource.getColumnCount(); j++) {
                        String columnName = dataSource.getColumnName(j);
                        String value = dataSource.getValueAt(i, columnName);
                        sql = sql.replace(":" + columnName, "'" + value.replace("'", "''") + "'");
                    }
                    statement.addBatch(sql);
                }
                statement.executeBatch();
            } catch (SQLException e) {
                System.err.println("Error executing batch: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Error processing database data: " + e.getMessage());
            }
        } else if (tableName != null) {
            // Write data to a database table
            try (Connection sqlConnection = DriverManager.getConnection(connectionString, username, password)) {
                // Create the table if requested
                if (createTable) {
                    createTable(sqlConnection, tableName, dataSource);
                }

                // Generate the INSERT statement
                StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
                StringBuilder placeholders = new StringBuilder(") VALUES (");

                for (int i = 0; i < dataSource.getColumnCount(); i++) {
                    String columnName = dataSource.getColumnName(i);
                    insertSql.append(columnName);
                    placeholders.append("?");

                    if (i < dataSource.getColumnCount() - 1) {
                        insertSql.append(", ");
                        placeholders.append(", ");
                    }
                }
                insertSql.append(placeholders).append(")");

                // Insert the data
                try (PreparedStatement statement = sqlConnection.prepareStatement(insertSql.toString())) {
                    for (int i = 0; i < dataSource.getRowCount(); i++) {
                        for (int j = 0; j < dataSource.getColumnCount(); j++) {
                            String columnName = dataSource.getColumnName(j);
                            String value = dataSource.getValueAt(i, columnName);
                            statement.setString(j + 1, value);
                        }
                        statement.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error writing to database: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Error processing database data: " + e.getMessage());
            }
        } else {
            throw new IllegalArgumentException("Either 'tableName' or 'sqlTemplate' must be specified in options");
        }
    }



    /**
     * Creates a table in the database based on the data source's columns.
     *
     * @param connection the database connection
     * @param tableName the name of the table to create
     * @param dataSource the data source to get column information from
     * @throws SQLException if there is an error creating the table
     */
    private void createTable(Connection connection, String tableName, IJDBCDataSource dataSource) throws SQLException {
        StringBuilder createSql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        for (int i = 0; i < dataSource.getColumnCount(); i++) {
            String columnName = dataSource.getColumnName(i);
            // If there are no rows, use the column name to infer the type
            String columnType;
            if (dataSource.getRowCount() > 0) {
                columnType = mapTableTypeToSqlType(dataSource.inferType(dataSource.getValueAt(0, columnName)));
            } else {
                // For empty tables, use a default type based on the column name
                // This is a simple heuristic and might need to be improved
                if (columnName.toLowerCase().contains("id") || columnName.toLowerCase().endsWith("count") || 
                    columnName.toLowerCase().equals("age")) {
                    columnType = "INTEGER";
                } else if (columnName.toLowerCase().contains("price") || columnName.toLowerCase().contains("amount") || 
                           columnName.toLowerCase().contains("salary") || columnName.toLowerCase().contains("value") && 
                           columnName.toLowerCase().contains("double")) {
                    columnType = "DOUBLE";
                } else if (columnName.toLowerCase().contains("is") || columnName.toLowerCase().contains("has") || 
                           columnName.toLowerCase().contains("boolean")) {
                    columnType = "BOOLEAN";
                } else {
                    columnType = "VARCHAR(255)";
                }
            }
            createSql.append(columnName).append(" ").append(columnType);

            if (i < dataSource.getColumnCount() - 1) {
                createSql.append(", ");
            }
        }
        createSql.append(")");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql.toString());
        }
    }

    /**
     * Maps Table types to SQL types.
     *
     * @param tableType the Table type
     * @return the corresponding SQL type
     */
    private String mapTableTypeToSqlType(String tableType) {
        switch (tableType) {
            case "int":
                return "INTEGER";
            case "double":
                return "DOUBLE";
            case "boolean":
                return "BOOLEAN";
            case "string":
            default:
                return "VARCHAR(255)";
        }
    }



}
