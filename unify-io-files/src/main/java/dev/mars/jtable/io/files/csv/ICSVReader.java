package dev.mars.jtable.io.files.csv;


import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.ICSVDataSource;
import dev.mars.jtable.io.common.datasource.IDataReader;

import java.io.IOException;

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
     * @throws IOException if there is an error reading the file or if the CSV format is invalid
     * @throws IllegalArgumentException if there is an error processing the CSV data
     */
    void readFromCSV(ICSVDataSource dataSource, FileConnection connection, boolean hasHeaderRow, boolean allowEmptyValues) throws IOException, IllegalArgumentException;
}
