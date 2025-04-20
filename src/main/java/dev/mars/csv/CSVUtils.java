package dev.mars.csv;

import dev.mars.adapter.CSVTableAdapter;
import dev.mars.mapping.MappingConfiguration;
import dev.mars.model.ITable;

/**
 * Utility class for CSV operations.
 * This class provides static methods for reading from and writing to CSV files.
 * It delegates to the new CSV module for the actual implementation.
 */
public class CSVUtils {
    private static final ICSVWriter csvWriter = new CSVWriter();
    private static final ICSVReader csvReader = new CSVReader();
    private static final CSVMappingReader csvMappingReader = new CSVMappingReader();

    /**
     * Writes data from a table to a CSV file.
     *
     * @param table the table to write from
     * @param fileName the name of the file to write to
     * @param withHeaderRow whether to include a header row in the CSV file
     */
    public static void writeToCSV(ITable table, String fileName, boolean withHeaderRow) {
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
    public static void readFromCSV(ITable table, String fileName, boolean hasHeaderRow, boolean allowEmptyValues) {
        CSVTableAdapter adapter = new CSVTableAdapter(table);
        csvReader.readFromCSV(adapter, fileName, hasHeaderRow, allowEmptyValues);
    }

    /**
     * Reads data from a CSV file into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     */
    public static void readFromCSV(ITable table, MappingConfiguration config) {
        csvMappingReader.readFromCSV(table, config);
    }
}
