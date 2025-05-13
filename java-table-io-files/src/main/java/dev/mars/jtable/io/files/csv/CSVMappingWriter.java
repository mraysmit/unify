package dev.mars.jtable.io.files.csv;

import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(CSVMappingWriter.class);
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
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (config == null) {
            String errorMsg = "Mapping configuration cannot be null";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        String fileName = config.getSourceLocation();
        if (fileName == null || fileName.trim().isEmpty()) {
            String errorMsg = "Source location in mapping configuration cannot be null or empty";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate column mappings
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            String errorMsg = "Column mappings cannot be null or empty";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        boolean withHeaderRow = (boolean) config.getOption("withHeaderRow", false);

        // Validate file access
        File file = new File(fileName);
        File parentDir = file.getParentFile();

        // Check if parent directory exists and is writable
        if (parentDir != null && !parentDir.exists()) {
            String errorMsg = "Parent directory does not exist: " + parentDir.getAbsolutePath();
            logger.error(errorMsg);
            throw new FileNotFoundException(errorMsg);
        }

        if (parentDir != null && !parentDir.canWrite()) {
            String errorMsg = "Cannot write to parent directory: " + parentDir.getAbsolutePath();
            logger.error(errorMsg);
            throw new IOException(errorMsg);
        }

        // Check if file exists and is writable
        if (file.exists() && !file.canWrite()) {
            String errorMsg = "Cannot write to file: " + fileName;
            logger.error(errorMsg);
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
                        logger.error(errorMsg);
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
                                logger.warn("Error getting value for column '{}' at row {}: {}", sourceColumnName, 
                                    rowIndex, e.getMessage());
                            }
                        } else {
                            logger.warn("Source column name is null or empty at index {}", i);
                        }
                    } else if (mapping.usesSourceColumnIndex()) {
                        int sourceColumnIndex = mapping.getSourceColumnIndex();
                        if (sourceColumnIndex >= 0 && sourceColumnIndex < table.getColumnCount()) {
                            try {
                                String columnName = table.getColumnName(sourceColumnIndex);
                                value = table.getValueAt(rowIndex, columnName);
                            } catch (Exception e) {
                                logger.warn("Error getting value for column index {} at row {}: {}", sourceColumnIndex, 
                                    rowIndex, e.getMessage());
                            }
                        } else {
                            logger.warn("Source column index {} is out of bounds (0-{})", sourceColumnIndex, 
                                (table.getColumnCount() - 1));
                        }
                    } else {
                        logger.warn("Mapping at index {} does not specify a source column name or index", i);
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
            logger.error("File not found: {}", e.getMessage());
            throw e;
        } catch (IOException e) {
            logger.error("Error writing CSV file: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("Error processing CSV data: {}", e.getMessage());
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
