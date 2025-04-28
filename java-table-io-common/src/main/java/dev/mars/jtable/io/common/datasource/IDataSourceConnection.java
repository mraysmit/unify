package dev.mars.jtable.io.common.datasource;

import java.util.Map;

/**
 * Interface representing a connection to a data source.
 * This abstraction handles the details of connecting to various types of data sources
 * such as databases, files (CSV, JSON, XML), web services, etc.
 */
public interface IDataSourceConnection {
    /**
     * Establishes a connection to the data source.
     *
     * @return true if connection was successful, false otherwise
     */
    boolean connect();

    /**
     * Closes the connection to the data source.
     */
    void disconnect();

    /**
     * Checks if the connection is currently active.
     *
     * @return true if connected, false otherwise
     */
    boolean isConnected();

    /**
     * Gets the connection type (e.g., "jdbc", "csv", "json", "xml", "http", etc.)
     *
     * @return the connection type as a string
     */
    String getConnectionType();

    /**
     * Gets the raw connection object if applicable (e.g., JDBC Connection object)
     *
     * @return the raw connection object, or null if not applicable
     */
    Object getRawConnection();

    /**
     * Gets connection-specific properties.
     *
     * @return a map of connection properties
     */
    Map<String, Object> getProperties();
}