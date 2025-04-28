package dev.mars.jtable.io.files.jsbc;



import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IDataWriter;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;

import java.util.Map;

/**
 * Interface for writing data to databases via JDBC.
 * This interface extends the generic IDataWriter interface and adds JDBC-specific methods.
 */
public interface IJDBCWriter extends IDataWriter {

    /**
     * Writes data from a data source to a destination using the provided connection.
     * This method can be used to write data to a database table or execute a batch of SQL statements.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing:
     *               - "tableName" (String): the name of the table to write to
     *               - "createTable" (Boolean): whether to create the table if it doesn't exist
     *               - "sqlTemplate" (String): the SQL template to use for each row
     *               - "username" (String): the database username
     *               - "password" (String): the database password
     */
    void writeData(IJDBCDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options);

}
