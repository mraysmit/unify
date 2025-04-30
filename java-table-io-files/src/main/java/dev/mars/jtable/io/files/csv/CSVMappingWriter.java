package dev.mars.jtable.io.files.csv;

import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Writer for CSV files using a mapping configuration.
 * This class writes data to a CSV file according to a mapping configuration.
 */
public class CSVMappingWriter {
    /**
     * Writes data from a table to a CSV file according to a mapping configuration.
     *
     * @param table the table to write from
     * @param config the mapping configuration
     * @throws IllegalArgumentException if table or config is null, or if config has invalid settings
     * @throws IOException if there is an error writing to the file
     * @throws FileNotFoundException if the file cannot be created or accessed
     */
    public void writeToCSV(ITable table, MappingConfiguration config) throws IOException, FileNotFoundException {
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

        boolean withHeaderRow = (boolean) config.getOption("withHeaderRow", false);

        // Validate file access
        File file = new File(fileName);
        File parentDir = file.getParentFile();

        // Check if parent directory exists and is writable
        if (parentDir != null && !parentDir.exists()) {
            String errorMsg = "Parent directory does not exist: " + parentDir.getAbsolutePath();
            System.err.println(errorMsg);
            throw new FileNotFoundException(errorMsg);
        }

        if (parentDir != null && !parentDir.canWrite()) {
            String errorMsg = "Cannot write to parent directory: " + parentDir.getAbsolutePath();
            System.err.println(errorMsg);
            throw new IOException(errorMsg);
        }

        // Check if file exists and is writable
        if (file.exists() && !file.canWrite()) {
            String errorMsg = "Cannot write to file: " + fileName;
            System.err.println(errorMsg);
            throw new IOException(errorMsg);
        }

        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the header row if requested
            if (withHeaderRow) {
                for (int i = 0; i < columnMappings.size(); i++) {
                    ColumnMapping mapping = columnMappings.get(i);
                    // For the header, we use the target column name from the mapping
                    String columnName = mapping.getTargetColumnName();
                    if (columnName == null) {
                        String errorMsg = "Target column name cannot be null at index " + i;
                        System.err.println(errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                    writer.append(columnName);
                    if (i < columnMappings.size() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            // Write each row
            for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                for (int i = 0; i < columnMappings.size(); i++) {
                    ColumnMapping mapping = columnMappings.get(i);
                    String value = "";

                    // Get the value from the source column
                    if (mapping.usesSourceColumnName()) {
                        String sourceColumnName = mapping.getSourceColumnName();
                        if (sourceColumnName != null && !sourceColumnName.isEmpty()) {
                            try {
                                value = table.getValueAt(rowIndex, sourceColumnName);
                            } catch (Exception e) {
                                System.err.println("Warning: Error getting value for column '" + sourceColumnName + 
                                    "' at row " + rowIndex + ": " + e.getMessage());
                            }
                        } else {
                            System.err.println("Warning: Source column name is null or empty at index " + i);
                        }
                    } else if (mapping.usesSourceColumnIndex()) {
                        int sourceColumnIndex = mapping.getSourceColumnIndex();
                        if (sourceColumnIndex >= 0 && sourceColumnIndex < table.getColumnCount()) {
                            try {
                                String columnName = table.getColumnName(sourceColumnIndex);
                                value = table.getValueAt(rowIndex, columnName);
                            } catch (Exception e) {
                                System.err.println("Warning: Error getting value for column index " + sourceColumnIndex + 
                                    " at row " + rowIndex + ": " + e.getMessage());
                            }
                        } else {
                            System.err.println("Warning: Source column index " + sourceColumnIndex + 
                                " is out of bounds (0-" + (table.getColumnCount() - 1) + ")");
                        }
                    } else {
                        System.err.println("Warning: Mapping at index " + i + 
                            " does not specify a source column name or index");
                    }

                    // Use default value if the value is null or empty
                    if (value == null || value.isEmpty()) {
                        value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                    }

                    // Escape special characters in CSV
                    value = escapeCSV(value);

                    writer.append(value);
                    if (i < columnMappings.size() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
            throw e;
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Escapes special characters in a CSV value.
     * 
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }

        // If the value contains a comma, double quote, or newline, wrap it in double quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            // Replace double quotes with two double quotes
            value = value.replace("\"", "\"\"");
            // Wrap the value in double quotes
            value = "\"" + value + "\"";
        }

        return value;
    }
}
