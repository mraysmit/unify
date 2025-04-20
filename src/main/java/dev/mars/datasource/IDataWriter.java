package dev.mars.datasource;

/**
 * Interface for writing data from a data source to various destinations.
 * This interface defines the common methods that all data writers must implement.
 */
public interface IDataWriter {
    /**
     * Writes data from a data source to a destination.
     *
     * @param dataSource the data source to write from
     * @param destination the destination to write to (e.g., file name, URL, etc.)
     * @param options additional options for writing (implementation-specific)
     */
    void writeData(IDataSource dataSource, String destination, java.util.Map<String, Object> options);
}