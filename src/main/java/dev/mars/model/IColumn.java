// src/main/java/dev/mars/model/IColumn.java
package dev.mars.model;

/**
 * Interface representing a column in a table.
 * A column has a name and a type.
 */
public interface IColumn<T> {
    /**
     * Gets the name of the column.
     *
     * @return the name of the column
     */
    String getName();

    /**
     * Gets the type of the column.
     *
     * @return the type of the column
     */
    Class<T> getType();

    /**
     * Creates a default value for this column.
     *
     * @return the default value for this column
     */
    T createDefaultValue();

    /**
     * Validates if the given value is valid for this column.
     *
     * @param value the value to validate
     * @return true if the value is valid, false otherwise
     */
    boolean isValidValue(Object value);

    /**
     * Converts a string value to the type of this column.
     *
     * @param value the string value to convert
     * @return the converted value
     */
    T convertFromString(String value);

    /**
     * Creates a new cell for this column with the given value.
     *
     * @param value the value for the cell
     * @return the new cell
     */
    ICell<T> createCell(T value);
}