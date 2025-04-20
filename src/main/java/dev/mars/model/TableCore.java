// src/main/java/dev/mars/model/TableCore.java
package dev.mars.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableCore implements ITable {
    private final Map<String, IColumn<?>> columns = new LinkedHashMap<>();
    private final List<IRow> rows = new ArrayList<>();
    private boolean createDefaultValue = true;

    public TableCore() {
    }

    @Override
    public IColumn<?> getColumn(String name) {
        return columns.get(name);
    }

    @Override
    public IColumn<?> getColumn(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("Invalid column index: " + index);
        }
        return new ArrayList<>(columns.values()).get(index);
    }

    @Override
    public List<IColumn<?>> getColumns() {
        return new ArrayList<>(columns.values());
    }

    @Override
    public void addColumn(IColumn<?> column) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null");
        }
        if (columns.containsKey(column.getName())) {
            throw new IllegalArgumentException("Column already exists: " + column.getName());
        }
        columns.put(column.getName(), column);
    }

    @Override
    public IRow getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IndexOutOfBoundsException("Invalid row index: " + index);
        }
        return rows.get(index);
    }

    @Override
    public List<IRow> getRows() {
        return new ArrayList<>(rows);
    }

    @Override
    public void addRow(IRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }

        // Check if all required columns are present
        for (IColumn<?> column : columns.values()) {
            if (row.getCell(column.getName()) == null) {
                if (createDefaultValue) {
                    // Add default value for missing column
                    addDefaultValue(row, column);
                } else {
                    throw new IllegalArgumentException("Row is missing column: " + column.getName());
                }
            }
        }

        // Validate all cells
        for (ICell<?> cell : row.getCells()) {
            IColumn<?> column = getColumn(cell.getColumn().getName());
            if (column == null) {
                throw new IllegalArgumentException("Column does not exist: " + cell.getColumn().getName());
            }
            if (!column.isValidValue(cell.getValue())) {
                throw new IllegalArgumentException("Invalid value for column: " + column.getName());
            }
        }

        rows.add(row);
    }

    @SuppressWarnings("unchecked")
    private <T> void addDefaultValue(IRow row, IColumn<?> column) {
        IColumn<T> typedColumn = (IColumn<T>) column;
        T defaultValue = typedColumn.createDefaultValue();
        row.setValue(typedColumn, defaultValue);
    }

    @Override
    public IRow createRow() {
        return new Row(this);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Object getValue(int rowIndex, String columnName) {
        IRow row = getRow(rowIndex);
        ICell<?> cell = row.getCell(columnName);
        if (cell == null) {
            return null;
        }
        return cell.getValue();
    }

    @Override
    public void setValue(int rowIndex, String columnName, Object value) {
        IRow row = getRow(rowIndex);
        row.setValue(columnName, value);
    }

    @Override
    public void printTable() {
        // Print column names
        for (IColumn<?> column : columns.values()) {
            System.out.print(column.getName() + "\t");
        }
        System.out.println();

        // Print rows
        for (IRow row : rows) {
            for (IColumn<?> column : columns.values()) {
                ICell<?> cell = row.getCell(column.getName());
                System.out.print(cell.getValueAsString() + "\t");
            }
            System.out.println();
        }
    }

    /**
     * Sets whether to create default values for missing columns.
     *
     * @param createDefaultValue whether to create default values for missing columns
     */
    public void setCreateDefaultValue(boolean createDefaultValue) {
        this.createDefaultValue = createDefaultValue;
    }

    /**
     * Gets whether to create default values for missing columns.
     *
     * @return whether to create default values for missing columns
     */
    public boolean isCreateDefaultValue() {
        return createDefaultValue;
    }
}