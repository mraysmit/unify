package dev.mars.jdbc;

import dev.mars.datasource.IDataReader;

/**
 * Interface for reading data from databases via JDBC.
 * This interface extends the generic IDataReader interface and adds JDBC-specific methods.
 */
public interface IJDBCReader extends IDataReader {
    /**
     * Reads data from a database table into a data source.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param tableName the name of the table to read from
     * @param username the database username
     * @param password the database password
     */
    void readFromDatabase(IJDBCDataSource dataSource, String connectionString, String tableName, String username, String password);

    /**
     * Reads data from a SQL query into a data source.
     *
     * @param dataSource the data source to read into
     * @param connectionString the JDBC connection string
     * @param query the SQL query to execute
     * @param username the database username
     * @param password the database password
     */
    void readFromQuery(IJDBCDataSource dataSource, String connectionString, String query, String username, String password);
}