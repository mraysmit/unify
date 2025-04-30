package dev.mars.jtable.io.files.jsbc;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.adapter.DataSourceTableAdapter;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for reading and writing tables from/to databases via JDBC.
 */
public class JDBCTableAdapter extends DataSourceTableAdapter implements IJDBCDataSource {
    private String connectionString;
    private String username;
    private String password;
    private String tableName;
    private String query;

    /**
     * Creates a new JDBC table adapter with an existing table.
     *
     * @param table the table to adapt
     */
    public JDBCTableAdapter(ITable table) {
        super(table);
    }

    /**
     * Creates a new JDBC table adapter for a specific table.
     *
     * @param connectionString the JDBC connection string
     * @param tableName the name of the table
     * @param username the database username
     * @param password the database password
     */
    public JDBCTableAdapter(String connectionString, String tableName, String username, String password) {
        super(null);
        this.connectionString = connectionString;
        this.tableName = tableName;
        this.username = username;
        this.password = password;
    }

    /**
     * Creates a new JDBC table adapter for a specific query.
     *
     * @param connectionString the JDBC connection string
     * @param query the SQL query
     * @param username the database username
     * @param password the database password
     */
    public JDBCTableAdapter(String connectionString, String query, String username, String password, boolean isQuery) {
        super(null);
        this.connectionString = connectionString;
        this.query = query;
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the JDBC connection string.
     *
     * @return the JDBC connection string
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Gets the database username.
     *
     * @return the database username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the database password.
     *
     * @return the database password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }



    public ITable readTable() {
        // Create a JDBC data source
        IJDBCDataSource dataSource = createJDBCDataSource();

        // Create a JDBC connection using the factory
        IDataSourceConnection connection = DataSourceConnectionFactory.createDatabaseConnection(
                connectionString, username, password);

        // Create options map
        Map<String, Object> options = new HashMap<>();
        if (tableName != null) {
            options.put("tableName", tableName);
        } else if (query != null) {
            options.put("query", query);
        }

        // Read data using the reader
        JDBCReader reader = new JDBCReader();
        reader.readData(dataSource, connection, options);

        // Convert the data source to a table
        return convertDataSourceToTable(dataSource);
    }

    // Helper method to create a JDBC data source
    private IJDBCDataSource createJDBCDataSource() {
        return this;
    }

    // Helper method to convert a data source to a table
    private ITable convertDataSourceToTable(IJDBCDataSource dataSource) {
        return getTable();
    }
}
