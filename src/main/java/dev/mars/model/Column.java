// src/main/java/dev/mars/model/Column.java
package dev.mars.model;

public class Column<T> implements IColumn<T> {
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    /**
     * Creates a new column with the given name, type, and default value.
     *
     * @param name the name of the column
     * @param type the type of the column
     * @param defaultValue the default value for the column
     */
    public Column(String name, Class<T> type, T defaultValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public T createDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isValidValue(Object value) {
        if (value == null) {
            return true; // Allow null values
        }
        return type.isInstance(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T convertFromString(String value) {
        if (value == null || value.isEmpty()) {
            if (type != String.class) {
                return null; // Return null for non-string types
            }
        }

        if (type == String.class) {
            return (T) value;
        } else if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (type == Double.class) {
            return (T) Double.valueOf(value);
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    @Override
    public ICell<T> createCell(T value) {
        return new Cell<>(this, value);
    }
}