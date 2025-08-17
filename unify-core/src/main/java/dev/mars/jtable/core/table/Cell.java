// src/main/java/dev/mars/model/Cell.java
package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.ICell;
import dev.mars.jtable.core.model.IColumn;

public class Cell<T> implements ICell<T> {
    private T value;
    private final IColumn<T> column;

    public Cell(IColumn<T> column, T value) {
        this.column = column;
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T value) {
        if (!column.isValidValue(value)) {
            throw new IllegalArgumentException("Invalid value for column: " + column.getName());
        }
        this.value = value;
    }

    @Override
    public String getValueAsString() {
        return value == null ? "" : value.toString();
    }

    @Override
    public IColumn<T> getColumn() {
        return column;
    }
}
