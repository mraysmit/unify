// src/main/java/dev/mars/model/TableBuilder.java
package dev.mars.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class TableBuilder {
    private final ITable table;
    private final Map<String, IColumn<?>> columns = new LinkedHashMap<>();

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
        return addColumn(ColumnFactory.createIntColumn(name));
    }

    public TableBuilder addDoubleColumn(String name) {
        return addColumn(ColumnFactory.createDoubleColumn(name));
    }

    public TableBuilder addBooleanColumn(String name) {
        return addColumn(ColumnFactory.createBooleanColumn(name));
    }

    public TableBuilder setCreateDefaultValue(boolean createDefaultValue) {
        ((TableCore) table).setCreateDefaultValue(createDefaultValue);
        return this;
    }

    public ITable build() {
        for (IColumn<?> column : columns.values()) {
            table.addColumn(column);
        }
        return table;
    }
}