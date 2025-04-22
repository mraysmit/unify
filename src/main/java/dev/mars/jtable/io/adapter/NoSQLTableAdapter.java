package dev.mars.jtable.io.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.nosql.NoSQLConnection;
import dev.mars.jtable.io.nosql.NoSQLReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for reading tables from NoSQL databases.
 */
public class NoSQLTableAdapter extends DataSourceTableAdapter {
    private String connectionString;
    private String database;
    private String collection;
    private String username;
    private String password;
    private String query;
    private Integer limit;

    /**
     * Creates a new NoSQL table adapter with an existing table.
     *
     * @param table the table to adapt
     */
    public NoSQLTableAdapter(ITable table) {
        super(table);
    }

    /**
     * Creates a new NoSQL table adapter.
     *
     * @param connectionString the connection string
     * @param database the database name
     * @param collection the collection name
     */
    public NoSQLTableAdapter(String connectionString, String database, String collection) {
        super(new Table());
        this.connectionString = connectionString;
        this.database = database;
        this.collection = collection;
    }

    /**
     * Creates a new NoSQL table adapter with authentication.
     *
     * @param connectionString the connection string
     * @param database the database name
     * @param collection the collection name
     * @param username the username
     * @param password the password
     */
    public NoSQLTableAdapter(String connectionString, String database, String collection, String username, String password) {
        this(connectionString, database, collection);
        this.username = username;
        this.password = password;
    }

    /**
     * Sets the query to use.
     *
     * @param query the query
     * @return this adapter for method chaining
     */
    public NoSQLTableAdapter withQuery(String query) {
        this.query = query;
        return this;
    }

    /**
     * Sets the limit for the number of results.
     *
     * @param limit the limit
     * @return this adapter for method chaining
     */
    public NoSQLTableAdapter withLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Reads a table from the NoSQL database.
     *
     * @return the table
     */
    public ITable readTable() {
        // Create a NoSQL connection using the factory
        IDataSourceConnection connection;
        if (username != null && !username.isEmpty()) {
            connection = DataSourceConnectionFactory.createNoSQLConnection(
                    connectionString, database, collection, username, password);
        } else {
            connection = DataSourceConnectionFactory.createNoSQLConnection(
                    connectionString, database, collection);
        }

        // Create options map
        Map<String, Object> options = new HashMap<>();
        if (query != null) {
            options.put("query", query);
        }
        if (limit != null) {
            options.put("limit", limit);
        }

        // Read data using the reader
        NoSQLReader reader = new NoSQLReader();
        reader.readData(this, connection, options);

        // Return the table
        return getTable();
    }
}