package dev.mars.csv;

import dev.mars.Table;
import dev.mars.adapter.CSVTableAdapter;

/**
 * Utility class for CSV operations.
 * This class provides static methods for reading from and writing to CSV files.
 * It delegates to the new CSV module for the actual implementation.
 */
public class CSVUtils {
    private static final ICSVWriter csvWriter = new CSVWriter();
    private static final ICSVReader csvReader = new CSVReader();

    /**
     * Writes data from a table to a CSV file.
     *
     * @param table the table to write from
     * @param fileName the name of the file to write to
     * @param withHeaderRow whether to include a header row in the CSV file
     */
    public static void writeToCSV(Table table, String fileName, boolean withHeaderRow) {
        CSVTableAdapter adapter = new CSVTableAdapter(table);
        csvWriter.writeToCSV(adapter, fileName, withHeaderRow);
    }

    /**
     * Reads data from a CSV file into a table.
     *
     * @param table the table to read into
     * @param fileName the name of the file to read from
     * @param hasHeaderRow whether the CSV file has a header row
     * @param allowEmptyValues whether to allow empty values in the CSV file
     */
    public static void readFromCSV(Table table, String fileName, boolean hasHeaderRow, boolean allowEmptyValues) {
        CSVTableAdapter adapter = new CSVTableAdapter(table);
        csvReader.readFromCSV(adapter, fileName, hasHeaderRow, allowEmptyValues);
    }
}
