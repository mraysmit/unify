// src/main/java/dev/mars/model/TableBuilder.java
package dev.mars.model;

import dev.mars.model.ITable;
import dev.mars.model.IColumn;
import dev.mars.model.IRow;
import dev.mars.model.TableCore;
import dev.mars.model.ColumnFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableBuilder {
    private final ITable table;
    private final Map<String, IColumn<?>> columns = new LinkedHashMap<>();
    private final List<Map<String, String>> rows = new ArrayList<>();

    public TableBuilder() {
        this.table = new TableCore();
    }

    public TableBuilder addColumn(IColumn<?> column) {
        columns.put(column.getName(), column);
        return this;
    }

    public TableBuilder addStringColumn(String name) {
        return addColumn(ColumnFactory.createStringColumn(name));
    }

    public TableBuilder addIntColumn(String name) {
        return addColumn(ColumnFactory.createIntegerColumn(name));
    }

    public TableBuilder addDoubleColumn(String name) {
        return addColumn(ColumnFactory.createDoubleColumn(name));
    }

    public TableBuilder addBooleanColumn(String name) {
        return addColumn(ColumnFactory.createBooleanColumn(name));
    }

    /**
     * Adds a row to the table being built.
     *
     * @param row a map of column names to values
     * @return this builder for method chaining
     */
    public TableBuilder addRow(Map<String, String> row) {
        rows.add(new HashMap<>(row)); // Create a copy to avoid external modifications
        return this;
    }

    /**
     * Adds a row to the table with values for specific columns.
     *
     * @param columnValues pairs of column names and values (e.g., "Name", "Alice", "Age", "30")
     * @return this builder for method chaining
     * @throws IllegalArgumentException if the number of arguments is odd
     */
    public TableBuilder addRow(String... columnValues) {
        if (columnValues.length % 2 != 0) {
            throw new IllegalArgumentException("Must provide pairs of column names and values");
        }

        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < columnValues.length; i += 2) {
            row.put(columnValues[i], columnValues[i + 1]);
        }
        return addRow(row);
    }

    /**
     * Adds multiple rows to the table being built.
     *
     * @param rows a list of maps, where each map represents a row with column names as keys and values as values
     * @return this builder for method chaining
     */
    public TableBuilder addRows(List<Map<String, String>> rows) {
        for (Map<String, String> row : rows) {
            addRow(row);
        }
        return this;
    }

    /**
     * Sets whether to create default values for missing columns.
     *
     * @param createDefaultValue whether to create default values for missing columns
     * @return this builder for method chaining
     */
    public TableBuilder setCreateDefaultValue(boolean createDefaultValue) {
        ((TableCore) table).setCreateDefaultValue(createDefaultValue);
        return this;
    }

    /**
     * Builds the table with the configured columns and rows.
     *
     * @return the built table
     */
    public ITable build() {
        // Add columns to the table
        for (IColumn<?> column : columns.values()) {
            table.addColumn(column);
        }

        // Add rows to the table
        for (Map<String, String> rowData : rows) {
            IRow row = table.createRow();

            // Add values from the map
            for (Map.Entry<String, String> entry : rowData.entrySet()) {
                String columnName = entry.getKey();
                String value = entry.getValue();
                IColumn<?> column = table.getColumn(columnName);

                if (column == null) {
                    throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
                }

                // Convert the string value to the column's type and set it
                Object convertedValue = convertValue(value, column);
                row.setValue(columnName, convertedValue);
            }

            // Add the row to the table
            table.addRow(row);
        }

        return table;
    }

    @SuppressWarnings("unchecked")
    private Object convertValue(String value, IColumn<?> column) {
        return ((IColumn<Object>) column).convertFromString(value);
    }
}