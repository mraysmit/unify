// src/main/java/dev/mars/model/IRow.java
package dev.mars.jtable.core.model;

import java.util.List;

/**
 * Interface representing a row in a table.
 * A row contains cells.
 */
public interface IRow {
    /**
     * Gets the cell at the given column.
     *
     * @param column the column
     * @return the cell at the given column
     */
    <T> ICell<T> getCell(IColumn<T> column);

    /**
     * Gets the cell at the given column name.
     *
     * @param columnName the name of the column
     * @return the cell at the given column name
     */
    ICell<?> getCell(String columnName);

    /**
     * Sets the value at the given column.
     *
     * @param column the column
     * @param value the value to set
     */
    <T> void setValue(IColumn<T> column, T value);

    /**
     * Sets the value at the given column name.
     *
     * @param columnName the name of the column
     * @param value the value to set
     */
    void setValue(String columnName, Object value);

    /**
     * Gets all cells in this row.
     *
     * @return all cells in this row
     */
    List<ICell<?>> getCells();

    /**
     * Gets the table this row belongs to.
     *
     * @return the table this row belongs to
     */
    ITable getTable();
}
