// src/main/java/dev/mars/model/IColumn.java
package dev.mars.model;

/**
 * Interface representing a column in a table.
 * A column has a name and a type.
 */
public interface IColumn<T> {

    String getName();

    Class<T> getType();

    T createDefaultValue();

    boolean isValidValue(Object value);

    T convertFromString(String value);

    ICell<T> createCell(T value);

}
