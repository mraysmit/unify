package dev.mars.jtable.io.files.json;


import dev.mars.jtable.io.common.datasource.IDataWriter;
import dev.mars.jtable.io.common.datasource.IJSONDataSource;

/**
 * Interface for writing data to JSON files.
 * This interface extends the generic IDataWriter interface and adds JSON-specific methods.
 */
public interface IJSONWriter extends IDataWriter {
    /**
     * Writes data from a data source to a JSON file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param prettyPrint whether to format the JSON output for readability
     */
    void writeToJSON(IJSONDataSource dataSource, String fileName, boolean prettyPrint);
}