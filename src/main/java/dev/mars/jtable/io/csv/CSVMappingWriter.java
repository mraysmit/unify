package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;

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
     */
    public void writeToCSV(ITable table, MappingConfiguration config) {
        String fileName = config.getSourceLocation();
        boolean withHeaderRow = (boolean) config.getOption("withHeaderRow", false);

        try (FileWriter writer = new FileWriter(fileName)) {
            // Get the column mappings
            List<ColumnMapping> columnMappings = config.getColumnMappings();
            if (columnMappings.isEmpty()) {
                return; // No columns to write
            }

            // Write the header row if requested
            if (withHeaderRow) {
                for (int i = 0; i < columnMappings.size(); i++) {
                    ColumnMapping mapping = columnMappings.get(i);
                    // For the header, we use the target column name from the mapping
                    String columnName = mapping.getTargetColumnName();
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
                            value = table.getValueAt(rowIndex, sourceColumnName);
                        }
                    } else if (mapping.usesSourceColumnIndex()) {
                        int sourceColumnIndex = mapping.getSourceColumnIndex();
                        if (sourceColumnIndex >= 0 && sourceColumnIndex < table.getColumnCount()) {
                            String columnName = table.getColumnName(sourceColumnIndex);
                            value = table.getValueAt(rowIndex, columnName);
                        }
                    }

                    // Use default value if the value is null or empty
                    if (value == null || value.isEmpty()) {
                        value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                    }

                    writer.append(value);
                    if (i < columnMappings.size() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
        }
    }
}
