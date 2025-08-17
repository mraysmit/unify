package dev.mars.jtable.io.common.datasource;

import java.io.IOException;
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
     * @throws IOException if there is an error reading from the source
     * @throws IllegalArgumentException if there is an error with the data source or connection
     */
    void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) throws IOException, IllegalArgumentException;
}
