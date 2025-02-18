package dev.mars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CSVUtils {

    public static void writeToCSV(Table table, String fileName, boolean withHeaderRow) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the header if withHeaderRow is true
            if (withHeaderRow) {
                for (int i = 0; i < table.getColumnCount(); i++) {
                    String columnName = table.getColumnName(i);
                    if (columnName == null || columnName.isEmpty()) {
                        columnName = "Column" + (i + 1);
                    }
                    writer.append(columnName);
                    if (i < table.getColumnCount() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }

            // Write the data rows
            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < table.getColumnCount(); j++) {
                    String columnName = table.getColumnName(j);
                    String value = table.getValueAt(i, columnName);
                    if (value == null || value.isEmpty()) {
                        writer.append("");
                    } else {
                        writer.append(value);
                    }
                    if (j < table.getColumnCount() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void readFromCSV(Table table, String fileName, boolean hasHeaderRow) {
        String line;
        String[] headers = new String[0];
        Map<String, String> columnNames = new HashMap<>();
        var colNames = new ArrayList<String>();
        var colTypes = new ArrayList<String>();

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
            String[] firstRowValues = line.split(",");
            if (firstRowValues.length != headers.length) {
                throw new IOException("CSV format error: number of values in the first row does not match the number of headers");
            }
            for (int i = 0; i < firstRowValues.length; i++) {
                var colName = colNames.get(i);
                var colType = table.inferType(firstRowValues[i]);
                columnNames.put(colName, colType);
            }
            table.setColumns(columnNames);

            // Add the first row
            Map<String, String> firstRow = new HashMap<>();
            for (int i = 0; i < firstRowValues.length; i++) {
                firstRow.put(colNames.get(i), firstRowValues[i]);
            }
            table.addRow(firstRow);

            // Add the remaining rows
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != headers.length) {
                    throw new IOException("CSV format error: number of values in a row does not match the number of headers");
                }
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < values.length; i++) {
                    row.put(columnNames.get(i), values[i]);
                }
                table.addRow(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
        }
    }
}