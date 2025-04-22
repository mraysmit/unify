package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.datasource.IDataReader;
import dev.mars.jtable.io.file.FileConnection;

/**
 * Interface for reading data from CSV files.
 * This interface extends the generic IDataReader interface and adds CSV-specific methods.
 */
public interface ICSVReader extends IDataReader {
    /**
     * Reads data from a CSV file into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the file connection
     * @param hasHeaderRow whether the CSV file has a header row
     * @param allowEmptyValues whether to allow empty values in the CSV file
     */
    void readFromCSV(ICSVDataSource dataSource, FileConnection connection, boolean hasHeaderRow, boolean allowEmptyValues);
}