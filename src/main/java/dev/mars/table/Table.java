package dev.mars.table;

import dev.mars.model.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Table implements ITable {
    private final ITable table;
    private boolean createDefaultValue = true;

    // Map to store original string representations of double values
    private final java.util.Map<String, java.util.Map<Integer, String>> originalDoubleStrings = new java.util.HashMap<>();

    public Table() {
        this.table = new TableCore();
        ((TableCore) table).setCreateDefaultValue(createDefaultValue);
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> columns) {
        table.setColumns(columns);
    }



    @SuppressWarnings("unchecked")
    @Override
    public Object convertValue(String value, IColumn<?> column) {
        return table.convertValue(value, column);
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        Object value = table.getValue(rowIndex, columnName);
        if (value instanceof Double) {
            // Store the original string representation of the double value
            String originalString = String.valueOf(value);
            originalDoubleStrings.computeIfAbsent(columnName, k -> new java.util.HashMap<>()).put(rowIndex, originalString);
        }
        return table.getValueAt(rowIndex, columnName);
    }

    public void setValueAt(int rowIndex, String columnName, String value) {
        table.setValue(rowIndex, columnName, convertValue(value, table.getColumn(columnName)));
    }

    @Override
    public String inferType(String value) {
        return table.inferType(value);
    }

    // Add this method to support the createDefaultValue functionality
    public void setCreateDefaultValue(boolean createDefaultValue) {
        this.createDefaultValue = createDefaultValue;
        ((TableCore) table).setCreateDefaultValue(createDefaultValue);
    }

    // Add this method to support the tests that use reflection to access it
    @Override
    public String getDefaultValue(String type) {
        return table.getDefaultValue(type);
    }

    @Override
    public IColumn<?> getColumn(String name) {
        return table.getColumn(name);
    }

    @Override
    public IColumn<?> getColumn(int index) {
        return table.getColumn(index);
    }

    @Override
    public List<IColumn<?>> getColumns() {
        return table.getColumns();
    }

    @Override
    public void addColumn(IColumn<?> column) {
        table.addColumn(column);
    }

    @Override
    public IRow getRow(int index) {
        return table.getRow(index);
    }

    @Override
    public List<IRow> getRows() {
        return table.getRows();
    }

    @Override
    public void addRow(IRow row) {
        table.addRow(row);
    }

    @Override
    public void addRow(Map<String, String> row) {
        // Create a copy of the row map to avoid modifying the original
        Map<String, String> rowCopy = new HashMap<>(row);

        // Check if all required columns are present
        for (IColumn<?> column : table.getColumns()) {
            if (!rowCopy.containsKey(column.getName())) {
                if (createDefaultValue) {
                    // Add default value for missing column
                    String columnType = "";
                    if (column instanceof Column) {
                        // Get the column type from the Column class
                        if (column.getClass().getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType) {
                            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) column.getClass().getGenericSuperclass();
                            Class<?> valueType = (Class<?>) paramType.getActualTypeArguments()[0];
                            if (valueType == String.class) {
                                columnType = "string";
                            } else if (valueType == Integer.class) {
                                columnType = "int";
                            } else if (valueType == Double.class) {
                                columnType = "double";
                            } else if (valueType == Boolean.class) {
                                columnType = "boolean";
                            }
                        }
                    }
                    rowCopy.put(column.getName(), table.getDefaultValue(columnType));
                } else {
                    throw new IllegalArgumentException("Row is missing column: " + column.getName());
                }
            }
        }

        table.addRow(rowCopy);
    }

    @Override
    public IRow createRow() {
        return table.createRow();
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
    public Object getValue(int rowIndex, String columnName) {
        return table.getValue(rowIndex, columnName);
    }

    @Override
    public void setValue(int rowIndex, String columnName, Object value) {
        table.setValue(rowIndex, columnName, value);
    }

    @Override
    public void printTable() {
        table.printTable();
    }

    @Override
    public String getColumnName(int index) {
        return table.getColumnName(index);
    }
}
