package dev.mars.jtable.io.json;

import dev.mars.jtable.io.datasource.IDataReader;

/**
 * Interface for reading data from JSON files.
 * This interface extends the generic IDataReader interface and adds JSON-specific methods.
 */
public interface IJSONReader extends IDataReader {
    /**
     * Reads data from a JSON file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param rootElement the name of the root element in the JSON file (optional)
     */
    void readFromJSON(IJSONDataSource dataSource, String fileName, String rootElement);
}