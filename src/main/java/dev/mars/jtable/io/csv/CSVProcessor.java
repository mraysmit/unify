package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.adapter.CSVTableAdapter;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.file.FileConnection;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for CSV operations.
 * This class provides static methods for reading from and writing to CSV files.
 * It delegates to the new CSV module for the actual implementation.
 */
public class CSVProcessor {
    private static final ICSVWriter csvWriter = new CSVWriter();
    private static final ICSVReader csvReader = new CSVReader();
    private static final CSVMappingReader csvMappingReader = new CSVMappingReader();
    private static final CSVMappingWriter csvMappingWriter = new CSVMappingWriter();

    /**
     * Writes data from a table to a CSV file.
     *
     * @param table the table to write from
     * @param fileName the name of the file to write to
     * @param withHeaderRow whether to include a header row in the CSV file
     */
    public static void writeToCSV(ITable table, String fileName, boolean withHeaderRow) {
        CSVTableAdapter adapter = new CSVTableAdapter(table);
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(fileName);
        connection.connect();

        // Use the writeData method with a connection and options
        Map<String, Object> options = new HashMap<>();
        options.put("withHeaderRow", Boolean.valueOf(withHeaderRow));
        csvWriter.writeData(adapter, connection, options);
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
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(fileName);
        connection.connect();
        csvReader.readFromCSV(adapter, connection, hasHeaderRow, allowEmptyValues);
    }

    /**
     * Reads data from a CSV file into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     */
    public static void readFromCSV(ITable table, MappingConfiguration config) {
        try {
            csvMappingReader.readFromCSV(table, config);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
    }

    /**
     * Writes data from a table to a CSV file according to a mapping configuration.
     *
     * @param table the table to write from
     * @param config the mapping configuration
     */
    public static void writeToCSV(ITable table, MappingConfiguration config) {
        try {
            csvMappingWriter.writeToCSV(table, config);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
}
