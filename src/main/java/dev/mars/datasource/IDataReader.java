package dev.mars.datasource;

/**
 * Interface for reading data from various sources into a data source.
 * This interface defines the common methods that all data readers must implement.
 */
public interface IDataReader {
    /**
     * Reads data from a source into a data source.
     *
     * @param dataSource the data source to read into
     * @param source the source to read from (e.g., file name, URL, etc.)
     * @param options additional options for reading (implementation-specific)
     */
    void readData(IDataSource dataSource, String source, java.util.Map<String, Object> options);
}