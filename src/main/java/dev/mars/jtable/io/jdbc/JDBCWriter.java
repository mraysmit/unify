package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.io.datasource.IDataSource;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Implementation of the IJDBCWriter interface for writing data to databases via JDBC.
 */
public class JDBCWriter implements IJDBCWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     * This method is part of the IDataWriter interface.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a JDBC-specific dataSource
        IJDBCDataSource jdbcDataSource;
        if (dataSource instanceof IJDBCDataSource) {
            jdbcDataSource = (IJDBCDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IJDBCDataSource");
        }

        // Extract options
        String tableName = options != null && options.containsKey("tableName") ? (String) options.get("tableName") : null;
        String sqlTemplate = options != null && options.containsKey("sqlTemplate") ? (String) options.get("sqlTemplate") : null;
        String username = options != null && options.containsKey("username") ? (String) options.get("username") : "";
        String password = options != null && options.containsKey("password") ? (String) options.get("password") : "";
        boolean createTable = options != null && options.containsKey("createTable") ? (Boolean) options.get("createTable") : false;

        // Ensure we have a JDBC connection
        if (!(connection instanceof JDBCConnection)) {
            throw new IllegalArgumentException("Connection must be a JDBCConnection");
        }
        JDBCConnection jdbcConnection = (JDBCConnection) connection;

        // Get the connection string
        String connectionString = jdbcConnection.getConnectionString();

        // Call the appropriate JDBC-specific method
        if (sqlTemplate != null) {
            executeBatch(jdbcDataSource, connectionString, sqlTemplate, username, password);
        } else if (tableName != null) {
            writeToDatabase(jdbcDataSource, connectionString, tableName, username, password, createTable);
        } else {
            throw new IllegalArgumentException("Either 'tableName' or 'sqlTemplate' must be specified in options");
        }
    }

    /**
     * Writes data from a data source to a database table.
     *
     * @param dataSource the data source to write from
     * @param connectionString the JDBC connection string
     * @param tableName the name of the table to write to
     * @param username the database username
     * @param password the database password
     * @param createTable whether to create the table if it doesn't exist
     */
    @Override
    public void writeToDatabase(IJDBCDataSource dataSource, String connectionString, String tableName, String username, String password, boolean createTable) {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password)) {
            // Create the table if requested
            if (createTable) {
                createTable(connection, tableName, dataSource);
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
            try (PreparedStatement statement = connection.prepareStatement(insertSql.toString())) {
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
    }

    /**
     * Executes a batch of SQL statements using data from a data source.
     *
     * @param dataSource the data source to get data from
     * @param connectionString the JDBC connection string
     * @param sqlTemplate the SQL template to use for each row
     * @param username the database username
     * @param password the database password
     */
    @Override
    public void executeBatch(IJDBCDataSource dataSource, String connectionString, String sqlTemplate, String username, String password) {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement()) {

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
