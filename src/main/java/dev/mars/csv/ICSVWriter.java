package dev.mars.csv;

import dev.mars.datasource.IDataWriter;

/**
 * Interface for writing data to CSV files.
 * This interface extends the generic IDataWriter interface and adds CSV-specific methods.
 */
public interface ICSVWriter extends IDataWriter {
    /**
     * Writes data from a data source to a CSV file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param withHeaderRow whether to include a header row in the CSV file
     */
    void writeToCSV(ICSVDataSource dataSource, String fileName, boolean withHeaderRow);
}
