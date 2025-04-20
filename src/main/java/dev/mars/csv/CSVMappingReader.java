package dev.mars.csv;

import dev.mars.mapping.ColumnMapping;
import dev.mars.mapping.MappingConfiguration;
import dev.mars.model.ITable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader for CSV files using a mapping configuration.
 * This class reads data from a CSV file according to a mapping configuration.
 */
public class CSVMappingReader {
    /**
     * Reads data from a CSV file into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     */
    public void readFromCSV(ITable table, MappingConfiguration config) {
        String fileName = config.getSourceLocation();
        boolean hasHeaderRow = (boolean) config.getOption("hasHeaderRow", false);
        boolean allowEmptyValues = (boolean) config.getOption("allowEmptyValues", false);

        try {
            // Set up the table columns based on the mapping configuration
            table.setColumns(config.createColumnDefinitions());

            // Read the CSV file
            List<String[]> rows = readCSVFile(fileName, hasHeaderRow, allowEmptyValues);
            if (rows.isEmpty()) {
                return; // No data to process
            }

            // Get the header row if available
            String[] headers = hasHeaderRow ? rows.get(0) : null;

            // Process each data row
            for (int i = hasHeaderRow ? 1 : 0; i < rows.size(); i++) {
                String[] values = rows.get(i);
                Map<String, String> rowData = new HashMap<>();

                // Process each column mapping
                for (ColumnMapping mapping : config.getColumnMappings()) {
                    String value = null;

                    // Get the value from the source column
                    if (mapping.usesSourceColumnName() && headers != null) {
                        // Find the index of the source column by name
                        int index = findColumnIndex(headers, mapping.getSourceColumnName());
                        if (index >= 0 && index < values.length) {
                            value = values[index];
                        }
                    } else if (mapping.usesSourceColumnIndex()) {
                        int index = mapping.getSourceColumnIndex();
                        if (index >= 0 && index < values.length) {
                            value = values[index];
                        }
                    }

                    // Use default value if the value is null or empty
                    if (value == null || value.isEmpty()) {
                        value = mapping.getDefaultValue();
                    }

                    // Add the value to the row data
                    if (value != null) {
                        rowData.put(mapping.getTargetColumnName(), value);
                    }
                }

                // Add the row to the table
                table.addRow(rowData);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
        }
    }

    /**
     * Reads a CSV file into a list of string arrays.
     *
     * @param fileName the name of the file to read
     * @param hasHeaderRow whether the CSV file has a header row
     * @param allowEmptyValues whether to allow empty values in the CSV file
     * @return a list of string arrays, where each array represents a row in the CSV file
     * @throws IOException if there is an error reading the file
     */
    private List<String[]> readCSVFile(String fileName, boolean hasHeaderRow, boolean allowEmptyValues) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", allowEmptyValues ? -1 : 0);
                rows.add(values);
            }
        }
        return rows;
    }

    /**
     * Finds the index of a column in a header row.
     *
     * @param headers the header row
     * @param columnName the name of the column to find
     * @return the index of the column, or -1 if not found
     */
    private int findColumnIndex(String[] headers, String columnName) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].equals(columnName)) {
                return i;
            }
        }
        return -1;
    }
}
