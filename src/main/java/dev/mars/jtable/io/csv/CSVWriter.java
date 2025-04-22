package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.datasource.IDataSource;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.file.FileConnection;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Implementation of the ICSVWriter interface for writing data to CSV files.
 */
public class CSVWriter implements ICSVWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a CSV-specific dataSource
        ICSVDataSource csvDataSource;
        if (dataSource instanceof ICSVDataSource) {
            csvDataSource = (ICSVDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement ICSVDataSource");
        }

        // Ensure we have a file connection
        if (!(connection instanceof FileConnection)) {
            throw new IllegalArgumentException("Connection must be a FileConnection");
        }
        FileConnection fileConnection = (FileConnection) connection;

        // Extract options
        boolean withHeaderRow = options != null && options.containsKey("withHeaderRow") ? (Boolean) options.get("withHeaderRow") : false;

        // Connect if not already connected
        if (!fileConnection.isConnected()) {
            fileConnection.connect();
        }

        // Get the file path from the connection
        String destination = fileConnection.getLocation();

        // Call the CSV-specific method
        writeToCSV(csvDataSource, destination, withHeaderRow);
    }
    /**
     * Writes data from a data source to a CSV file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param withHeaderRow whether to include a header row in the CSV file
     */
    @Override
    public void writeToCSV(ICSVDataSource dataSource, String fileName, boolean withHeaderRow) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the header if withHeaderRow is true
            if (withHeaderRow) {
                for (int i = 0; i < dataSource.getColumnCount(); i++) {
                    String columnName = dataSource.getColumnName(i);
                    if (columnName == null || columnName.isEmpty()) {
                        columnName = "Column" + (i + 1);
                    }
                    writer.append(columnName);
                    if (i < dataSource.getColumnCount() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            // Write the data rows
            for (int i = 0; i < dataSource.getRowCount(); i++) {
                for (int j = 0; j < dataSource.getColumnCount(); j++) {
                    String columnName = dataSource.getColumnName(j);
                    String value = dataSource.getValueAt(i, columnName);
                    if (value == null || value.isEmpty()) {
                        writer.append("");
                    } else {
                        writer.append(value);
                    }
                    if (j < dataSource.getColumnCount() - 1) {
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
