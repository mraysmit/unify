package dev.mars.jtable.io.files.csv;


import dev.mars.jtable.io.common.datasource.ICSVDataSource;
import dev.mars.jtable.io.common.datasource.IDataWriter;

import java.io.IOException;

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
     * @throws IOException if there is an error writing to the file
     * @throws IllegalArgumentException if there is an error with the data source
     */
    void writeToCSV(ICSVDataSource dataSource, String fileName, boolean withHeaderRow) throws IOException, IllegalArgumentException;
}
