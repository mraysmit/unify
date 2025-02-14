package dev.mars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Table {
    private Map<String, Map<Integer, String>> table;
    private Map<Integer, String> columnNames;
    private Map<String, String> columnTypes;
    private int rowCount;

    public Table() {
        this.table = new HashMap<>();
        this.columnNames = new HashMap<>();
        this.columnTypes = new HashMap<>();
        this.rowCount = 0;
    }

    public void setColumnNames(Map<Integer, String> columnNames, Map<String, String> columnTypes) {
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
    }

    public void addRow(Map<String, String> row) {
        if (row.size() != columnNames.size()) {
            throw new IllegalArgumentException("Row size does not match column count");
        }
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String columnName = entry.getKey();
            String value = entry.getValue();
            String type = columnTypes.get(columnName);
            if (!isValidType(value, type)) {
                throw new IllegalArgumentException("Invalid value type for column: " + columnName);
            }
            if (!table.containsKey(columnName)) {
                table.put(columnName, new HashMap<>());
            }
            table.get(columnName).put(rowCount, value);
        }
        rowCount++;
    }

    private boolean isValidType(String value, String type) {
        try {
            switch (type.toLowerCase()) {
                case "int":
                    Integer.parseInt(value);
                    break;
                case "double":
                    Double.parseDouble(value);
                    break;
                case "boolean":
                    Boolean.parseBoolean(value);
                    break;
                case "string":
                    // No validation needed for string
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported column type: " + type);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private String inferType(String value) {
        if (value.matches("-?\\d+")) {
            return "int";
        } else if (value.matches("-?\\d*\\.\\d+")) {
            return "double";
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "boolean";
        } else {
            return "string";
        }
    }

    public String getValueAt(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Invalid row index");
        }
        if (!table.containsKey(columnName)) {
            throw new IllegalArgumentException("Invalid column name");
        }
        return table.get(columnName).get(rowIndex);
    }

    public void setValueAt(int rowIndex, String columnName, String value) {
        if (rowIndex < 0 || rowIndex >= rowCount) {
            throw new IndexOutOfBoundsException("Invalid row index");
        }
        if (!table.containsKey(columnName)) {
            throw new IllegalArgumentException("Invalid column name");
        }
        String type = columnTypes.get(columnName);
        if (!isValidType(value, type)) {
            throw new IllegalArgumentException("Invalid value type for column: " + columnName);
        }
        table.get(columnName).put(rowIndex, value);
    }

    public int getRowCount() {
        return rowCount;
    }

    public int getColumnCount() {
        return columnNames.size();
    }

    public void printTable() {
        for (int i = 0; i < columnNames.size(); i++) {
            System.out.print(columnNames.get(i) + "\t");
        }
        System.out.println();
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < columnNames.size(); j++) {
                String columnName = columnNames.get(j);
                System.out.print(table.get(columnName).get(i) + "\t");
            }
            System.out.println();
        }
    }

    public void writeToCSV(String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // Write the header
            for (int i = 0; i < columnNames.size(); i++) {
                writer.append(columnNames.get(i));
                if (i < columnNames.size() - 1) {
                    writer.append(",");
                }
            }
            writer.append("\n");

            // Write the data rows
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnNames.size(); j++) {
                    String columnName = columnNames.get(j);
                    writer.append(table.get(columnName).get(i));
                    if (j < columnNames.size() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readFromCSV(String fileName, boolean hasHeaderRow) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            String[] headers;
            Map<Integer, String> columnNames = new HashMap<>();
            Map<String, String> columnTypes = new HashMap<>();

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
                br.reset();
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
                columnTypes.put(columnNames.get(i), inferType(firstRowValues[i]));
            }
            setColumnNames(columnNames, columnTypes);

            // Add the first row
            Map<String, String> firstRow = new HashMap<>();
            for (int i = 0; i < firstRowValues.length; i++) {
                firstRow.put(columnNames.get(i), firstRowValues[i]);
            }
            addRow(firstRow);

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
                addRow(row);
            }
        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing CSV data: " + e.getMessage());
        }
    }
}