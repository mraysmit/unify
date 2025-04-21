// src/main/java/dev/mars/model/ITable.java
package dev.mars.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface representing a table.
 * A table contains columns and rows.
 */
public interface ITable {

    String getDefaultValue(String type);

    IColumn<?> getColumn(String name);

    IColumn<?> getColumn(int index);

    List<IColumn<?>> getColumns();

    void addColumn(IColumn<?> column);

    String getColumnName(int index);

    IRow getRow(int index);

    List<IRow> getRows();

    void addRow(IRow row);

    void addRow(Map<String, String> row);

    IRow createRow();

    int getRowCount();

    int getColumnCount();

    Object getValue(int rowIndex, String columnName);

    void setValue(int rowIndex, String columnName, Object value);

    @SuppressWarnings("unchecked")
    Object convertValue(String value, IColumn<?> column);

    void setValueAt(int rowIndex, String columnName, String value);

    void setColumns(LinkedHashMap<String, String> columns);

    String inferType(String value);

    String getValueAt(int rowIndex, String columnName);

    void printTable();
}
