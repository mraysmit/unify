package dev.mars;

import java.util.*;

public class Table {
    private List<Map<String, String>> rows;
    private Map<String, String> columns;
    private boolean createDefaultValue = true;

    public Table() {
        this.rows = new ArrayList<>();
        this.columns = new LinkedHashMap<>();
    }

    public void setColumns(LinkedHashMap<String, String> columns) {
    if (columns == null) {
        throw new IllegalArgumentException("Columns map cannot be null");
    }
    for (Map.Entry<String, String> entry : columns.entrySet()) {
        String columnName = entry.getKey();
        String columnType = entry.getValue();
        if (columnName == null || columnName.trim().isEmpty()) {
            throw new IllegalArgumentException("Column names cannot be null or blank");
        }
        if (columnType == null || columnType.trim().isEmpty()) {
            throw new IllegalArgumentException("Column types cannot be null or blank");
        }
    }
    if (columns.size() != new HashSet<>(columns.keySet()).size()) {
        throw new IllegalArgumentException("Duplicate column names are not allowed");
    }
    this.columns = columns;
}

    public void addRow(Map<String, String> row) {
        if (row.size() != columns.size()) {

            if (createDefaultValue)  {
                if (row.size() != columns.size()) {
                    for (String column : columns.keySet()) {
                        if (!row.containsKey(column)) {
                            row.put(column, getDefaultValue(columns.get(column)));
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Row size does not match column count");
        }
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String columnName = entry.getKey();
            String value = entry.getValue();
            String type = columns.get(columnName);
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


    private String getDefaultValue(String type) {
        switch (type) {
            case "int":
                return "0";
            case "double":
                return "0.0";
            case "boolean":
                return "false";
            case "string":
            default:
                return "";
        }
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
        String type = columns.get(columnName);
        if (!isValidType(value, type)) {
            throw new IllegalArgumentException("Invalid value type for column: " + columnName);
        }
        rows.get(rowIndex).put(columnName, value);
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columns.size();
    }

    public String getColumnName(int index) {
        return (String) columns.keySet().toArray()[index];
    }

    public void printTable() {
        for (String columnName : columns.keySet()) {
            System.out.print(columnName + "\t");
        }
        System.out.println();
        for (Map<String, String> row : rows) {
            for (String columnName : columns.keySet()) {
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