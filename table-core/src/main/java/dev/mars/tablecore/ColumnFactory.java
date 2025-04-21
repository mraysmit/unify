// src/main/java/dev/mars/model/ColumnFactory.java
package dev.mars.tablecore;

/**
 * Factory for creating columns of different types.
 */
public class ColumnFactory {

    public static IColumn<String> createStringColumn(String name) {
        return new Column<>(name, String.class, "");
    }

    public static IColumn<Integer> createIntegerColumn(String name) {
        return new Column<>(name, Integer.class, 0);
    }

    public static IColumn<Double> createDoubleColumn(String name) {
        return new Column<>(name, Double.class, 0.0);
    }

    public static IColumn<Boolean> createBooleanColumn(String name) {
        return new Column<>(name, Boolean.class, false);
    }

    public static IColumn<?> createColumn(String name, String type) {
        switch (type.toLowerCase()) {
            case "string":
                return createStringColumn(name);
            case "int":
                return createIntegerColumn(name);
            case "double":
                return createDoubleColumn(name);
            case "boolean":
                return createBooleanColumn(name);
            default:
                throw new IllegalArgumentException("Unsupported column type: " + type);
        }
    }
}