package dev.mars.jtable.io.files.csv;


import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.adapter.CSVTableAdapter;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;

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
    public static void writeToCSV(ITable table, String fileName, boolean withHeaderRow) throws IOException {
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
    public static void readFromCSV(ITable table, String fileName, boolean hasHeaderRow, boolean allowEmptyValues) throws IOException {
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
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if there is an error reading the file
     */
    public static void readFromCSV(ITable table, MappingConfiguration config) throws FileNotFoundException, IOException {
        csvMappingReader.readFromCSV(table, config);
    }

    /**
     * Writes data from a table to a CSV file according to a mapping configuration.
     *
     * @param table the table to write from
     * @param config the mapping configuration
     * @throws FileNotFoundException if the file cannot be created or accessed
     * @throws IOException if there is an error writing to the file
     */
    public static void writeToCSV(ITable table, MappingConfiguration config) throws FileNotFoundException, IOException {
        csvMappingWriter.writeToCSV(table, config);
    }
}
