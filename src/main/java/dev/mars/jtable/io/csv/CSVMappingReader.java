package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
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
     * @throws IllegalArgumentException if table or config is null, or if config has invalid settings
     * @throws IOException if there is an error reading the file
     * @throws FileNotFoundException if the file does not exist
     */
    public void readFromCSV(ITable table, MappingConfiguration config) throws IOException, FileNotFoundException {
        // Validate input parameters
        if (table == null) {
            String errorMsg = "Table cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (config == null) {
            String errorMsg = "Mapping configuration cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        String fileName = config.getSourceLocation();
        if (fileName == null || fileName.trim().isEmpty()) {
            String errorMsg = "Source location in mapping configuration cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate column mappings
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            String errorMsg = "Column mappings cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        boolean hasHeaderRow = (boolean) config.getOption("hasHeaderRow", false);
        boolean allowEmptyValues = (boolean) config.getOption("allowEmptyValues", false);

        try {
            // Validate file existence
            File file = new File(fileName);
            if (!file.exists()) {
                String errorMsg = "CSV file does not exist: " + fileName;
                System.err.println(errorMsg);
                throw new FileNotFoundException(errorMsg);
            }

            if (!file.isFile()) {
                String errorMsg = "Path is not a file: " + fileName;
                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }

            if (!file.canRead()) {
                String errorMsg = "Cannot read file: " + fileName;
                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }

            // Set up the table columns based on the mapping configuration
            table.setColumns(config.createColumnDefinitions());

            // Read the CSV file
            List<String[]> rows = readFromCSVFile(fileName, hasHeaderRow, allowEmptyValues);
            if (rows.isEmpty()) {
                System.err.println("Warning: No data found in CSV file: " + fileName);
                return; // No data to process
            }

            // Get the header row if available
            String[] headers = hasHeaderRow ? rows.get(0) : null;

            // Validate header row if expected
            if (hasHeaderRow && (headers == null || headers.length == 0)) {
                String errorMsg = "Expected header row but found none in file: " + fileName;
                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }

            // Process each data row
            for (int i = hasHeaderRow ? 1 : 0; i < rows.size(); i++) {
                String[] values = rows.get(i);
                Map<String, String> rowData = new HashMap<>();

                // Process each column mapping
                for (ColumnMapping mapping : columnMappings) {
                    String value = null;

                    // Get the value from the source column
                    if (mapping.usesSourceColumnName() && headers != null) {
                        // Find the index of the source column by name
                        int index = findColumnIndex(headers, mapping.getSourceColumnName());
                        if (index >= 0 && index < values.length) {
                            value = values[index];
                        } else {
                            System.err.println("Warning: Column '" + mapping.getSourceColumnName() + 
                                "' not found in CSV file or index out of bounds. Using default value if available.");
                        }
                    } else if (mapping.usesSourceColumnIndex()) {
                        int index = mapping.getSourceColumnIndex();
                        if (index >= 0 && index < values.length) {
                            value = values[index];
                        } else {
                            System.err.println("Warning: Column index " + index + 
                                " out of bounds. Using default value if available.");
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
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
            throw e;
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
     * @throws FileNotFoundException if the file does not exist
     * @throws IllegalArgumentException if fileName is null or empty
     */
    private List<String[]> readFromCSVFile(String fileName, boolean hasHeaderRow, boolean allowEmptyValues) throws IOException {
        // Validate input parameters
        if (fileName == null || fileName.trim().isEmpty()) {
            String errorMsg = "File name cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    System.err.println("Warning: Empty line found at line " + lineNumber + ", skipping");
                    continue;
                }

                try {
                    String[] values = line.split(",", allowEmptyValues ? -1 : 0);

                    // Validate that we have at least one value
                    if (values.length == 0) {
                        System.err.println("Warning: No values found at line " + lineNumber + ", skipping");
                        continue;
                    }

                    rows.add(values);
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                    throw new IOException("Error parsing CSV line " + lineNumber + ": " + e.getMessage(), e);
                }
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + fileName);
            throw e;
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + fileName + " - " + e.getMessage());
            throw e;
        }

        return rows;
    }

    /**
     * Finds the index of a column in a header row.
     *
     * @param headers the header row
     * @param columnName the name of the column to find
     * @return the index of the column, or -1 if not found
     * @throws IllegalArgumentException if headers or columnName is null
     */
    private int findColumnIndex(String[] headers, String columnName) {
        // Validate input parameters
        if (headers == null) {
            String errorMsg = "Headers array cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (columnName == null) {
            String errorMsg = "Column name cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // If headers array is empty, return -1
        if (headers.length == 0) {
            System.err.println("Warning: Headers array is empty");
            return -1;
        }

        // If column name is empty, log a warning but still try to find it
        if (columnName.trim().isEmpty()) {
            System.err.println("Warning: Column name is empty");
        }

        // Search for the column name in the headers
        for (int i = 0; i < headers.length; i++) {
            // Skip null headers
            if (headers[i] == null) {
                System.err.println("Warning: Null header at index " + i);
                continue;
            }

            if (headers[i].equals(columnName)) {
                return i;
            }
        }

        // Column not found
        System.err.println("Warning: Column '" + columnName + "' not found in headers");
        return -1;
    }
}
