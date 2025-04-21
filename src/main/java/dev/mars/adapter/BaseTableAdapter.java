package dev.mars.adapter;

import dev.mars.csv.ICSVDataSource;
import dev.mars.datasource.IDataSource;
import dev.mars.model.ITable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects CSV data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the ICSVDataSource interface.
 */
public abstract class BaseTableAdapter implements ITableAdapter, IDataSource {
    protected final ITable table;

    public BaseTableAdapter(ITable table) {
        this.table = table;
    }

    @Override
    public ITable getTable() {
        return table;
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
        return table.getColumnName(index);
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        return table.getValueAt(rowIndex, columnName);
    }

    @Override
    public String inferType(String value) {
        return table.inferType(value);
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> columns) {
        table.setColumns(columns);
    }

    @Override
    public void addRow(Map<String, String> row) {
        table.addRow(row);
    }
}
