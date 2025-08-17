package dev.mars.jtable.io.common.datasource;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for writing data from a data source to various destinations.
 * This interface defines the common methods that all data writers must implement.
 */
public interface IDataWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     * @throws IOException if there is an error writing to the destination
     * @throws IllegalArgumentException if there is an error with the data source or connection
     */
    void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) throws IOException, IllegalArgumentException;
}
