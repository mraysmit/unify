package dev.mars.jtable.core.model;

/**
 * Interface representing a cell in a table.
 * A cell contains a value of a specific type.
 */
public interface ICell<T> {
    /**
     * Gets the value of the cell.
     *
     * @return the value of the cell
     */
    T getValue();

    /**
     * Sets the value of the cell.
     *
     * @param value the value to set
     */
    void setValue(T value);

    /**
     * Gets the string representation of the cell value.
     *
     * @return the string representation of the cell value
     */
    String getValueAsString();

    /**
     * Gets the column this cell belongs to.
     *
     * @return the column this cell belongs to
     */
    IColumn<T> getColumn();
}
