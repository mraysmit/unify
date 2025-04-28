package dev.mars.jtable.io.common.adapter;


import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.rest.RESTReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter for reading tables from REST APIs.
 */
public class RESTTableAdapter extends DataSourceTableAdapter {
    private String endpoint;
    private String authToken;
    private String method;
    private String responseFormat;

    /**
     * Creates a new REST table adapter with an existing table.
     *
     * @param table the table to adapt
     */
    public RESTTableAdapter(ITable table) {
        super(table);
    }

    /**
     * Creates a new REST table adapter.
     *
     * @param endpoint the REST API endpoint URL
     */
    public RESTTableAdapter(String endpoint) {
        super(new Table());
        this.endpoint = endpoint;
        this.method = "GET";
        this.responseFormat = "json";
    }

    /**
     * Creates a new REST table adapter with authentication.
     *
     * @param endpoint the REST API endpoint URL
     * @param authToken the authentication token
     */
    public RESTTableAdapter(String endpoint, String authToken) {
        this(endpoint);
        this.authToken = authToken;
    }

    /**
     * Sets the HTTP method to use.
     *
     * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @return this adapter for method chaining
     */
    public RESTTableAdapter withMethod(String method) {
        this.method = method;
        return this;
    }

    /**
     * Sets the response format to expect.
     *
     * @param responseFormat the response format (json, xml, csv)
     * @return this adapter for method chaining
     */
    public RESTTableAdapter withResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
        return this;
    }

    /**
     * Reads a table from the REST API.
     *
     * @return the table
     */
    public ITable readTable() {
        // Create a REST connection using the factory
        IDataSourceConnection connection;
        if (authToken != null && !authToken.isEmpty()) {
            connection = DataSourceConnectionFactory.createRESTConnection(endpoint, authToken);
        } else {
            connection = DataSourceConnectionFactory.createRESTConnection(endpoint);
        }

        // Create options map
        Map<String, Object> options = new HashMap<>();
        options.put("method", method);
        options.put("responseFormat", responseFormat);

        // Read data using the reader
        RESTReader reader = new RESTReader();
        reader.readData(this, connection, options);

        // Return the table
        return getTable();
    }
}
