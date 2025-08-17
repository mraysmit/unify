package dev.mars.jtable.io.files.jdbc;

import dev.mars.jtable.io.common.datasource.IDataReader;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;

import java.util.Map;

/**
 * Interface for reading data from databases via JDBC.
 * This interface extends the generic IDataReader interface and adds JDBC-specific methods.
 */
public interface IJDBCReader extends IDataReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     * This method can be used to read data from a database table or execute a SQL query.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading:
     *               - "tableName" (String): the name of the table to read from
     *               - "query" (String): the SQL query to execute
     */
    void readData(IJDBCDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options);

}
