package dev.mars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CSVUtils {

    public static void writeToCSV(Table table, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the header
            for (int i = 0; i < table.getColumnCount(); i++) {
                writer.append(table.getColumnName(i));
                if (i < table.getColumnCount() - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");

            // Write the data rows
            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < table.getColumnCount(); j++) {
                    String columnName = table.getColumnName(j);
                    writer.append(table.getValueAt(i, columnName));
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
        String[] headers;
        Map<Integer, String> columnNames = new HashMap<>();
        Map<String, String> columnTypes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            if (hasHeaderRow) {
                headers = br.readLine().split(",");
                for (int i = 0; i < headers.length; i++) {
                    columnNames.put(i, headers[i]);
                }
            } else {
                line = br.readLine();
                headers = line.split(",");
                for (int i = 0; i < headers.length; i++) {
                    columnNames.put(i, "Column" + (i + 1));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            // Skip the header row if it exists
            if (hasHeaderRow) {
                br.readLine();
            }

            // Read the first data row to infer types
            line = br.readLine();
            if (line == null) {
                throw new IOException("CSV file is empty or missing data rows");
            }
            String[] firstRowValues = line.split(",");
            if (firstRowValues.length != headers.length) {
                throw new IOException("CSV format error: number of values in the first row does not match the number of headers");
            }
            for (int i = 0; i < firstRowValues.length; i++) {
                columnTypes.put(columnNames.get(i), table.inferType(firstRowValues[i]));
            }
            table.setColumnNames(columnNames, columnTypes);

            // Add the first row
            Map<String, String> firstRow = new HashMap<>();
            for (int i = 0; i < firstRowValues.length; i++) {
                firstRow.put(columnNames.get(i), firstRowValues[i]);
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