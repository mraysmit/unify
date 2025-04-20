package dev.mars.jdbc;

import dev.mars.datasource.IDataSource;
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
     * Reads data from a source into a data source.
     * This method is part of the IDataReader interface.
     *
     * @param dataSource the data source to read into
     * @param source the source to read from (e.g., connection string)
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, String source, Map<String, Object> options) {
        // Convert the generic dataSource to a JDBC-specific dataSource
        IJDBCDataSource jdbcDataSource;
        if (dataSource instanceof IJDBCDataSource) {
            jdbcDataSource = (IJDBCDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IJDBCDataSource");
        }

        // Extract options
        String tableName = options != null && options.containsKey("tableName") ? (String) options.get("tableName") : null;
        String query = options != null && options.containsKey("query") ? (String) options.get("query") : null;
        String username = options != null && options.containsKey("username") ? (String) options.get("username") : "";
        String password = options != null && options.containsKey("password") ? (String) options.get("password") : "";

        // Call the appropriate JDBC-specific method
        if (query != null) {
            readFromQuery(jdbcDataSource, source, query, username, password);
        } else if (tableName != null) {
            readFromDatabase(jdbcDataSource, source, tableName, username, password);
        } else {
            throw new IllegalArgumentException("Either 'tableName' or 'query' must be specified in options");
        }
    }

    /**
     * Reads data from a database table into a data source.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param tableName the name of the table to read from
     * @param username the database username
     * @param password the database password
     */
    @Override
    public void readFromDatabase(IJDBCDataSource dataSource, String connectionString, String tableName, String username, String password) {
        String query = "SELECT * FROM " + tableName;
        readFromQuery(dataSource, connectionString, query, username, password);
    }

    /**
     * Reads data from a SQL query into a data source.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param query the SQL query to execute
     * @param username the database username
     * @param password the database password
     */
    @Override
    public void readFromQuery(IJDBCDataSource dataSource, String connectionString, String query, String username, String password) {
        try (Connection connection = DriverManager.getConnection(connectionString, username, password);
             Statement statement = connection.createStatement();
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
        } catch (SQLException e) {
            System.err.println("Error reading from database: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing database data: " + e.getMessage());
        }
    }

    /**
     * Maps SQL types to Table types.
     *
     * @param sqlType the SQL type code
     * @return the corresponding Table type
     */
    private String mapSqlTypeToTableType(int sqlType) {
        // Map SQL types to Table types
        // See java.sql.Types for SQL type codes
        switch (sqlType) {
            case java.sql.Types.INTEGER:
            case java.sql.Types.SMALLINT:
            case java.sql.Types.TINYINT:
            case java.sql.Types.BIGINT:
                return "int";
            case java.sql.Types.FLOAT:
            case java.sql.Types.DOUBLE:
            case java.sql.Types.DECIMAL:
            case java.sql.Types.NUMERIC:
            case java.sql.Types.REAL:
                return "double";
            case java.sql.Types.BOOLEAN:
            case java.sql.Types.BIT:
                return "boolean";
            case java.sql.Types.DATE:
            case java.sql.Types.TIME:
            case java.sql.Types.TIMESTAMP:
            case java.sql.Types.CHAR:
            case java.sql.Types.VARCHAR:
            case java.sql.Types.LONGVARCHAR:
            case java.sql.Types.NCHAR:
            case java.sql.Types.NVARCHAR:
            case java.sql.Types.LONGNVARCHAR:
            default:
                return "string";
        }
    }
}