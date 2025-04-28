package dev.mars.jtable.io.files.csv;


import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.ICSVDataSource;
import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of the ICSVReader interface for reading data from CSV files.
 */
public class CSVReader implements ICSVReader {



    /**
     * Reads data from a source into a data source.
     * This method is part of the IDataReader interface.
     *
     * @param dataSource the data source to read into
     * @param connection the source to read from (e.g., file name, URL, etc.)
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a CSV-specific dataSource
        ICSVDataSource csvDataSource;
        if (dataSource instanceof ICSVDataSource) {
            csvDataSource = (ICSVDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement ICSVDataSource");
        }

        // Check if connection is a FileConnection
        if (!(connection instanceof FileConnection)) {
            throw new IllegalArgumentException("Connection must be a FileConnection for CSV reading");
        }
        FileConnection fileConnection = (FileConnection) connection;

        // Extract options
        boolean hasHeaderRow = options != null && options.containsKey("hasHeaderRow") ? (Boolean) options.get("hasHeaderRow") : false;
        boolean allowEmptyValues = options != null && options.containsKey("allowEmptyValues") ? (Boolean) options.get("allowEmptyValues") : false;

        // Call the CSV-specific method
        readFromCSV(csvDataSource, fileConnection, hasHeaderRow, allowEmptyValues);
    }



    /**
     * Reads data from a CSV file into a data source.
     *
     * @param dataSource the data source to read into
     * @param connection the file connection to read from
     * @param hasHeaderRow whether the CSV file has a header row
     * @param allowEmptyValues whether to allow empty values in the CSV file
     */
    @Override
    public void readFromCSV(ICSVDataSource dataSource, FileConnection connection, boolean hasHeaderRow, boolean allowEmptyValues) {
        String line;
        String[] headers = new String[0];
        var columnNames = new LinkedHashMap<String, String>();
        var colNames = new ArrayList<String>();
        var colTypes = new ArrayList<String>();

        // Make sure connection is established
        if (!connection.isConnected()) {
            connection.connect();
        }

        // Extract the file name from the connection
        String fileName = connection.getLocation();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            if (hasHeaderRow) {
                line = br.readLine();
                if (line != null) {
                    headers = line.split(",");
                    for (int i = 0; i < headers.length; i++) {
                        colNames.add(headers[i]);
                    }
                }
            } else {
                line = br.readLine();
                if (line != null) {
                    headers = line.split(",");
                    for (int i = 0; i < headers.length; i++) {
                        colNames.add("Column" + (i + 1));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return;
        }

        if (headers.length == 0) {
            return; // No data to process
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            // Skip the header row if it exists
            if (hasHeaderRow) {
                br.readLine();
            }

            // Read the first data row to infer types
            line = br.readLine();
            if (line == null) {
                return; // No data rows to process
            }
            String[] firstRowValues = line.split(",", allowEmptyValues ? -1 : headers.length);
            if (firstRowValues.length != headers.length) {
                throw new IOException("CSV format error: number of values in the first row does not match the number of headers");
            }
            for (int i = 0; i < firstRowValues.length; i++) {
                var colName = colNames.get(i);
                var colType = dataSource.inferType(firstRowValues[i]);
                columnNames.put(colName, colType);
            }
            dataSource.setColumns(columnNames);

            // Add the first row
            Map<String, String> firstRow = new HashMap<>();
            for (int i = 0; i < firstRowValues.length; i++) {
                firstRow.put(colNames.get(i), firstRowValues[i]);
            }
            dataSource.addRow(firstRow);

            // Add the remaining rows
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != headers.length) {
                    throw new IOException("CSV format error: number of values in a row does not match the number of headers");
                }
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < values.length; i++) {
                    row.put(colNames.get(i), values[i]);
                }
                dataSource.addRow(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
        }
    }




}
