package dev.mars;

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

    public String inferType(String value) {
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

    public String getColumnName(int index) {
        return columnNames.get(index);
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
}