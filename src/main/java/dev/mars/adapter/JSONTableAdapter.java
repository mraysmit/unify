package dev.mars.adapter;

import dev.mars.model.ITable;
import dev.mars.json.IJSONDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects JSON data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IJSONDataSource interface.
 */
public class JSONTableAdapter implements ITableAdapter, IJSONDataSource {
    private final ITable table;

    /**
     * Creates a new JSONTableAdapter for the specified Table instance.
     *
     * @param table the Table instance to adapt
     */
    public JSONTableAdapter(ITable table) {
        this.table = table;
    }

    /**
     * Gets the Table instance that this adapter is connected to.
     *
     * @return the Table instance
     */
    @Override
    public ITable getTable() {
        return table;
    }

    /**
     * Gets the number of rows in the data source.
     *
     * @return the number of rows
     */
    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    /**
     * Gets the number of columns in the data source.
     *
     * @return the number of columns
     */
    @Override
    public int getColumnCount() {
        return table.getColumnCount();
    }

    /**
     * Gets the name of a column at a specific index.
     *
     * @param index the index of the column
     * @return the name of the column
     */
    @Override
    public String getColumnName(int index) {
        return table.getColumnName(index);
    }

    /**
     * Gets the value at a specific row and column.
     *
     * @param rowIndex the index of the row
     * @param columnName the name of the column
     * @return the value at the specified row and column
     */
    @Override
    public String getValueAt(int rowIndex, String columnName) {
        return table.getValueAt(rowIndex, columnName);
    }

    /**
     * Infers the data type of a value.
     *
     * @param value the value to infer the type of
     * @return the inferred data type
     */
    @Override
    public String inferType(String value) {
        return table.inferType(value);
    }

    /**
     * Sets the columns of the data source.
     *
     * @param columns a map of column names to column types
     */
    @Override
    public void setColumns(LinkedHashMap<String, String> columns) {
        table.setColumns(columns);
    }

    /**
     * Adds a row to the data source.
     *
     * @param row a map of column names to values
     */
    @Override
    public void addRow(Map<String, String> row) {
        table.addRow(row);
    }
}
