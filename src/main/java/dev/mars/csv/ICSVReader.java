package dev.mars.csv;

import dev.mars.datasource.IDataReader;

/**
 * Interface for reading data from CSV files.
 * This interface extends the generic IDataReader interface and adds CSV-specific methods.
 */
public interface ICSVReader extends IDataReader {
    /**
     * Reads data from a CSV file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param hasHeaderRow whether the CSV file has a header row
     * @param allowEmptyValues whether to allow empty values in the CSV file
     */
    void readFromCSV(ICSVDataSource dataSource, String fileName, boolean hasHeaderRow, boolean allowEmptyValues);
}
