// src/main/java/dev/mars/model/ITable.java
package dev.mars.model;

import java.util.List;

/**
 * Interface representing a table.
 * A table contains columns and rows.
 */
public interface ITable {
    /**
     * Gets the column with the given name.
     *
     * @param name the name of the column
     * @return the column with the given name
     */
    IColumn<?> getColumn(String name);

    /**
     * Gets the column at the given index.
     *
     * @param index the index of the column
     * @return the column at the given index
     */
    IColumn<?> getColumn(int index);

    /**
     * Gets all columns in this table.
     *
     * @return all columns in this table
     */
    List<IColumn<?>> getColumns();

    /**
     * Adds a column to this table.
     *
     * @param column the column to add
     */
    void addColumn(IColumn<?> column);

    /**
     * Gets the row at the given index.
     *
     * @param index the index of the row
     * @return the row at the given index
     */
    IRow getRow(int index);

    /**
     * Gets all rows in this table.
     *
     * @return all rows in this table
     */
    List<IRow> getRows();

    /**
     * Adds a row to this table.
     *
     * @param row the row to add
     */
    void addRow(IRow row);

    /**
     * Creates a new row for this table.
     *
     * @return a new row for this table
     */
    IRow createRow();

    /**
     * Gets the number of rows in this table.
     *
     * @return the number of rows in this table
     */
    int getRowCount();

    /**
     * Gets the number of columns in this table.
     *
     * @return the number of columns in this table
     */
    int getColumnCount();

    /**
     * Gets the value at the given row and column.
     *
     * @param rowIndex the index of the row
     * @param columnName the name of the column
     * @return the value at the given row and column
     */
    Object getValue(int rowIndex, String columnName);

    /**
     * Sets the value at the given row and column.
     *
     * @param rowIndex the index of the row
     * @param columnName the name of the column
     * @param value the value to set
     */
    void setValue(int rowIndex, String columnName, Object value);

    /**
     * Prints the table to the console.
     */
    void printTable();
}