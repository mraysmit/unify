// src/main/java/dev/mars/model/ColumnFactory.java
package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.IColumn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    /**
     * Creates a new date column with the given name.
     * The column will store LocalDate values.
     *
     * @param name the name of the column
     * @return a new date column
     */
    public static IColumn<LocalDate> createDateColumn(String name) {
        return new Column<>(name, LocalDate.class, LocalDate.now());
    }

    /**
     * Creates a new time column with the given name.
     * The column will store LocalTime values.
     *
     * @param name the name of the column
     * @return a new time column
     */
    public static IColumn<LocalTime> createTimeColumn(String name) {
        return new Column<>(name, LocalTime.class, LocalTime.now());
    }

    /**
     * Creates a new date-time column with the given name.
     * The column will store LocalDateTime values.
     *
     * @param name the name of the column
     * @return a new date-time column
     */
    public static IColumn<LocalDateTime> createDateTimeColumn(String name) {
        return new Column<>(name, LocalDateTime.class, LocalDateTime.now());
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
            case "date":
                return createDateColumn(name);
            case "time":
                return createTimeColumn(name);
            case "datetime":
                return createDateTimeColumn(name);
            default:
                throw new IllegalArgumentException("Unsupported column type: " + type);
        }
    }
}
