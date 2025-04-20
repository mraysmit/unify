package dev.mars.jdbc;

import dev.mars.datasource.IDataWriter;

/**
 * Interface for writing data to databases via JDBC.
 * This interface extends the generic IDataWriter interface and adds JDBC-specific methods.
 */
public interface IJDBCWriter extends IDataWriter {
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
    void writeToDatabase(IJDBCDataSource dataSource, String connectionString, String tableName, String username, String password, boolean createTable);

    /**
     * Executes a batch of SQL statements using data from a data source.
     *
     * @param dataSource the data source to get data from
     * @param connectionString the JDBC connection string
     * @param sqlTemplate the SQL template to use for each row
     * @param username the database username
     * @param password the database password
     */
    void executeBatch(IJDBCDataSource dataSource, String connectionString, String sqlTemplate, String username, String password);
}