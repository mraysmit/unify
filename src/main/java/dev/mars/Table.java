package dev.mars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Table {
    private List<Map<String, String>> rows;
    private Map<Integer, String> columnNames;
    private Map<String, String> columnTypes;

    public Table() {
        this.rows = new ArrayList<>();
        this.columnNames = new HashMap<>();
        this.columnTypes = new HashMap<>();
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
        }
        rows.add(row);
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

    public String getValueAt(int rowIndex, String columnName) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Invalid row index");
        }
        return rows.get(rowIndex).get(columnName);
    }

    public void setValueAt(int rowIndex, String columnName, String value) {
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IndexOutOfBoundsException("Invalid row index");
        }
        String type = columnTypes.get(columnName);
        if (!isValidType(value, type)) {
            throw new IllegalArgumentException("Invalid value type for column: " + columnName);
        }
        rows.get(rowIndex).put(columnName, value);
    }

    public int getRowCount() {
        return rows.size();
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
        for (Map<String, String> row : rows) {
            for (int j = 0; j < columnNames.size(); j++) {
                String columnName = columnNames.get(j);
                System.out.print(row.get(columnName) + "\t");
            }
            System.out.println();
        }
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
}