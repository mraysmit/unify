package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.io.datasource.IDataReader;

/**
 * Interface for reading data from databases via JDBC.
 * This interface extends the generic IDataReader interface and adds JDBC-specific methods.
 */
public interface IJDBCReader extends IDataReader {
    /**
     * Reads data from a database table into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the JDBC connection
     * @param tableName the name of the table to read from
     */
    void readFromDatabase(IJDBCDataSource dataSource, JDBCConnection connection, String tableName);

    /**
     * Reads data from a SQL query into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the JDBC connection
     * @param query the SQL query to execute
     */
    void readFromQuery(IJDBCDataSource dataSource, JDBCConnection connection, String query);
}