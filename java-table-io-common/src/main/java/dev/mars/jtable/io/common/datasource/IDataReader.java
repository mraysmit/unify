package dev.mars.jtable.io.common.datasource;

import java.util.Map;

/**
 * Interface for reading data from various sources into a data source.
 * This interface defines the common methods that all data readers must implement.
 */
public interface IDataReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options);
}