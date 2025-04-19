// src/main/java/dev/mars/TableAdapter.java
package dev.mars;

import dev.mars.model.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter for the old Table class to use the new data model.
 * This class maintains backward compatibility with existing code.
 */
public class TableAdapter extends Table {
    private final ITable table;

    public TableAdapter() {
        this.table = new dev.mars.model.Table();
    }

    @Override
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

        // Clear existing columns
        for (IColumn<?> column : table.getColumns()) {
            // We can't actually remove columns, so we'll just create a new table
            // This is a limitation of the adapter approach
        }

        // Add new columns
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            IColumn<?> column = ColumnFactory.createColumn(columnName, columnType);
            table.addColumn(column);
        }
    }

    @Override
    public void addRow(Map<String, String> row) {
        IRow newRow = table.createRow();

        // Add values from the map
        for (Map.Entry<String, String> entry : row.entrySet()) {
            String columnName = entry.getKey();
            String value = entry.getValue();
            IColumn<?> column = table.getColumn(columnName);

            if (column == null) {
                throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
            }

            // Convert the string value to the column's type
            Object convertedValue = convertValue(value, column);
            newRow.setValue(columnName, convertedValue);
        }

        // Add the row to the table
        table.addRow(newRow);
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(String value, IColumn<?> column) {
        return ((IColumn<Object>) column).convertFromString(value);
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        Object value = table.getValue(rowIndex, columnName);
        return value == null ? null : value.toString();
    }

    @Override
    public void setValueAt(int rowIndex, String columnName, String value) {
        IColumn<?> column = table.getColumn(columnName);
        if (column == null) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
        }

        Object convertedValue = convertValue(value, column);
        table.setValue(rowIndex, columnName, convertedValue);
    }

    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return table.getColumnCount();
    }

    @Override
    public String getColumnName(int index) {
        IColumn<?> column = table.getColumn(index);
        return column.getName();
    }

    @Override
    public void printTable() {
        table.printTable();
    }

    @Override
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