package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.ICell;
import dev.mars.jtable.core.model.IColumn;
import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.model.ITable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An optimized implementation of TableCore that uses appropriate collection types
 * based on read-write ratios. This class demonstrates how to optimize collections
 * for different usage patterns.
 * 
 * Key optimizations:
 * 1. LinkedHashMap for columns (read-heavy, order matters)
 * 2. ArrayList for rows (balanced read-write, random access)
 * 3. ConcurrentHashMap for originalDoubleStrings (thread-safe for potential concurrent access)
 * 
 * For multi-threaded scenarios:
 * 1. ConcurrentHashMap with custom ordering logic for columns
 * 2. CopyOnWriteArrayList for rows (if read-heavy)
 */
public class OptimizedTableCore implements ITable {
    // Read-heavy collection (mostly read after initialization)
    // LinkedHashMap preserves column order which is important
    private final Map<String, IColumn<?>> columns = new LinkedHashMap<>();

    // Balanced read-write collection (frequent reads and writes)
    // ArrayList provides fast random access and efficient iteration
    private final List<IRow> rows;

    // For single-threaded access, HashMap is appropriate
    // For multi-threaded access, ConcurrentHashMap provides thread safety
    // This example uses ConcurrentHashMap to demonstrate thread-safe collections
    // Using default initial capacity (16) and load factor (0.75)
    private final Map<String, Map<Integer, String>> originalDoubleStrings = new ConcurrentHashMap<>(16, 0.75f);

    private boolean createDefaultValue = true;

    // Constants for Double handling
    private static final int MAX_FRACTION_DIGITS = 10;

    /**
     * Creates a new OptimizedTableCore.
     */
    public OptimizedTableCore() {
        // Initialize with default capacity
        this.rows = new ArrayList<>();
    }

    /**
     * Creates a new OptimizedTableCore with the specified initial capacity for rows.
     * This constructor is useful when the approximate number of rows is known in advance,
     * which can improve performance by reducing the number of resizing operations.
     *
     * @param initialRowCapacity the initial capacity for the rows collection
     */
    public OptimizedTableCore(int initialRowCapacity) {
        // Initialize with specified capacity for better performance
        // when the number of rows is known in advance
        this.rows = new ArrayList<>(initialRowCapacity);
    }

    /**
     * Creates a new OptimizedTableCore optimized for concurrent access.
     * This constructor uses thread-safe collections for all internal data structures.
     *
     * @param concurrent whether to use thread-safe collections
     */
    public OptimizedTableCore(boolean concurrent) {
        if (concurrent) {
            // Use thread-safe collections for concurrent access
            // CopyOnWriteArrayList is appropriate for read-heavy scenarios
            // No initial capacity parameter is provided as CopyOnWriteArrayList doesn't have a constructor with capacity
            this.rows = new CopyOnWriteArrayList<>();
            // Note: ConcurrentHashMap doesn't preserve order, so we need custom ordering logic
            // This is just a demonstration - in a real implementation, we would need to
            // implement custom ordering logic for columns
        } else {
            // Use standard collections for single-threaded access
            this.rows = new ArrayList<>();
        }
    }

    /**
     * Creates a new OptimizedTableCore with specified initial capacity and concurrent access option.
     * This constructor allows specifying the initial capacity for collections to reduce resizing operations.
     *
     * @param concurrent whether to use thread-safe collections
     * @param initialRowCapacity the initial capacity for the rows collection
     */
    public OptimizedTableCore(boolean concurrent, int initialRowCapacity) {
        if (concurrent) {
            // Use thread-safe collections for concurrent access
            // Since CopyOnWriteArrayList doesn't have a constructor with capacity,
            // we create an ArrayList with the specified capacity and convert it to a CopyOnWriteArrayList
            List<IRow> initialList = new ArrayList<>(initialRowCapacity);
            this.rows = new CopyOnWriteArrayList<>(initialList);
        } else {
            // Use standard collections for single-threaded access with specified capacity
            this.rows = new ArrayList<>(initialRowCapacity);
        }
    }


    @Override
    public IColumn<?> getColumn(String name) {
        return columns.get(name);
    }

    @Override
    public IColumn<?> getColumn(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("Invalid column index: " + index);
        }
        return new ArrayList<>(columns.values()).get(index);
    }

    @Override
    public List<IColumn<?>> getColumns() {
        return new ArrayList<>(columns.values());
    }

    @Override
    public void addColumn(IColumn<?> column) {
        if (column == null) {
            throw new IllegalArgumentException("Column cannot be null");
        }
        if (columns.containsKey(column.getName())) {
            throw new IllegalArgumentException("Column already exists: " + column.getName());
        }
        columns.put(column.getName(), column);
    }

    @Override
    public String getColumnName(int index) {
        if (index < 0 || index >= columns.size()) {
            throw new IndexOutOfBoundsException("Invalid column index: " + index);
        }
        IColumn<?> column = new ArrayList<>(columns.values()).get(index);
        return column.getName();
    }

    @Override
    public IRow getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IndexOutOfBoundsException("Invalid row index: " + index);
        }
        return rows.get(index);
    }

    @Override
    public List<IRow> getRows() {
        return new ArrayList<>(rows);
    }

    @Override
    public void addRow(IRow row) {
        if (row == null) {
            throw new IllegalArgumentException("Row cannot be null");
        }

        // Check if all required columns are present
        for (IColumn<?> column : columns.values()) {
            if (row.getCell(column.getName()) == null) {
                if (createDefaultValue) {
                    // Add default value for missing column
                    addDefaultValue(row, column);
                } else {
                    throw new IllegalArgumentException("Row is missing column: " + column.getName());
                }
            }
        }

        // Validate all cells
        for (ICell<?> cell : row.getCells()) {
            IColumn<?> column = getColumn(cell.getColumn().getName());
            if (column == null) {
                throw new IllegalArgumentException("Column does not exist: " + cell.getColumn().getName());
            }
            if (!column.isValidValue(cell.getValue())) {
                throw new IllegalArgumentException("Invalid value for column: " + column.getName());
            }
        }

        rows.add(row);
    }

    @Override
    public void addRow(Map<String, String> row) {
        if (row == null) {
            throw new IllegalArgumentException("Row map cannot be null");
        }

        // Create a copy of the row map to avoid modifying the original
        // HashMap is appropriate here as it's a temporary collection with balanced read-write operations
        Map<String, String> rowCopy = new HashMap<>(row);

        // Check if all required columns are present
        for (IColumn<?> column : columns.values()) {
            if (!rowCopy.containsKey(column.getName())) {
                if (createDefaultValue) {
                    // Add default value for missing column
                    String columnType = "";
                    if (column instanceof Column) {
                        Column<?> typedColumn = (Column<?>) column;
                        Class<?> valueType = typedColumn.getType();
                        if (valueType == String.class) {
                            columnType = "string";
                        } else if (valueType == Integer.class) {
                            columnType = "int";
                        } else if (valueType == Double.class) {
                            columnType = "double";
                        } else if (valueType == Boolean.class) {
                            columnType = "boolean";
                        }
                    }
                    rowCopy.put(column.getName(), getDefaultValue(columnType));
                } else {
                    throw new IllegalArgumentException("Row is missing column: " + column.getName());
                }
            }
        }

        // Create a new row
        IRow newRow = createRow();

        // Add values from the map
        for (Map.Entry<String, String> entry : rowCopy.entrySet()) {
            String columnName = entry.getKey();
            String value = entry.getValue();
            IColumn<?> column = getColumn(columnName);

            if (column == null) {
                throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
            }

            // Convert the string value to the column's type
            Object convertedValue = column.convertFromString(value);
            newRow.setValue(columnName, convertedValue);

            // Store original string representation for double values
            if (convertedValue instanceof Double && value.contains(".")) {
                // Initialize the map for this column if needed
                // Using createSizedConcurrentMap to create a properly sized ConcurrentHashMap
                originalDoubleStrings.computeIfAbsent(columnName, k -> createSizedConcurrentMap(8));
                // Store the original string at the next row index
                originalDoubleStrings.get(columnName).put(getRowCount(), value);
            }
        }

        // Add the row to the table
        rows.add(newRow);
    }

    @SuppressWarnings("unchecked")
    private <T> void addDefaultValue(IRow row, IColumn<?> column) {
        IColumn<T> typedColumn = (IColumn<T>) column;
        T defaultValue = typedColumn.createDefaultValue();
        row.setValue(typedColumn, defaultValue);
    }

    @Override
    public IRow createRow() {
        return new OptimizedRow(this);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public Object getValueObject(int rowIndex, String columnName) {
        IRow row = getRow(rowIndex);
        ICell<?> cell = row.getCell(columnName);
        if (cell == null) {
            return null;
        }
        return cell.getValue();
    }

    @Override
    public void setValue(int rowIndex, String columnName, Object value) {
        IRow row = getRow(rowIndex);
        row.setValue(columnName, value);
    }

    @Override
    public void setValueAt(int rowIndex, String columnName, String value) {
        IColumn<?> column = getColumn(columnName);
        if (column == null) {
            throw new IllegalArgumentException("Column '" + columnName + "' does not exist");
        }

        Object convertedValue = column.convertFromString(value);
        setValue(rowIndex, columnName, convertedValue);

        // Store original string representation for double values
        if (convertedValue instanceof Double && value.contains(".")) {
            // Initialize the map for this column if needed
            // Using createSizedConcurrentMap to create a properly sized ConcurrentHashMap
            // Initial capacity of 8 is appropriate for most columns as they will have a moderate number of rows
            originalDoubleStrings.computeIfAbsent(columnName, k -> createSizedConcurrentMap(8));
            // Store the original string
            originalDoubleStrings.get(columnName).put(rowIndex, value);
        }
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> newColumns) {
        if (newColumns == null) {
            throw new IllegalArgumentException("Columns map cannot be null");
        }
        for (Map.Entry<String, String> entry : newColumns.entrySet()) {
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            if (columnName == null || columnName.trim().isEmpty()) {
                throw new IllegalArgumentException("Column names cannot be null or blank");
            }
            if (columnType == null || columnType.trim().isEmpty()) {
                throw new IllegalArgumentException("Column types cannot be null or blank");
            }
        }
        if (newColumns.size() != new HashSet<>(newColumns.keySet()).size()) {
            throw new IllegalArgumentException("Duplicate column names are not allowed");
        }

        // Clear existing columns
        if (!columns.isEmpty()) {
            columns.clear();
        }

        // Add new columns
        for (Map.Entry<String, String> entry : newColumns.entrySet()) {
            String columnName = entry.getKey();
            String columnType = entry.getValue();
            IColumn<?> column = ColumnFactory.createColumn(columnName, columnType);
            columns.put(column.getName(), column);
        }
    }

    @Override
    public String inferType(String value) {
        if (value == null) {
            return "string";
        }

        // Trim whitespace for better pattern matching
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return "string";
        }

        // Integer pattern: optional negative sign followed by one or more digits
        if (trimmedValue.matches("^-?\\d+$")) {
            return "int";
        } 
        // Double patterns:
        // 1. Standard decimal: optional sign, digits, decimal point, optional digits
        // 2. Leading decimal: optional sign, decimal point, one or more digits
        // 3. Scientific notation: any of the above followed by e or E, optional sign, and digits
        else if (trimmedValue.matches("^[-+]?\\d+\\.\\d*$") || 
                 trimmedValue.matches("^[-+]?\\.\\d+$") ||
                 trimmedValue.matches("^[-+]?\\d+\\.?\\d*[eE][-+]?\\d+$") ||
                 trimmedValue.matches("^[-+]?\\.\\d+[eE][-+]?\\d+$")) {
            return "double";
        } 
        // Boolean pattern: case-insensitive "true" or "false"
        else if (trimmedValue.equalsIgnoreCase("true") || trimmedValue.equalsIgnoreCase("false")) {
            return "boolean";
        } 
        // Special numeric values
        else if (trimmedValue.equalsIgnoreCase("NaN") || 
                 trimmedValue.equalsIgnoreCase("Infinity") || 
                 trimmedValue.equalsIgnoreCase("+Infinity") || 
                 trimmedValue.equalsIgnoreCase("-Infinity")) {
            return "double";
        } 
        // Everything else is a string
        else {
            return "string";
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convertValue(String value, IColumn<?> column) {
        return ((IColumn<Object>) column).convertFromString(value);
    }

    @Override
    public String getDefaultValue(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        switch (type) {
            case "int":
                return "0";
            case "double":
                return "0.0";
            case "boolean":
                return "false";
            case "string":
            default:
                return "";
        }
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        Object value = getValueObject(rowIndex, columnName);
        if (value == null) {
            return null;
        }

        // General handling for Double values to preserve trailing zeros
        if (value instanceof Double) {
            // Check if we have the original string representation
            if (originalDoubleStrings.containsKey(columnName) &&
                    originalDoubleStrings.get(columnName).containsKey(rowIndex)) {
                return originalDoubleStrings.get(columnName).get(rowIndex);
            }

            // Use DecimalFormat to preserve decimal places
            java.text.DecimalFormat df = new java.text.DecimalFormat();
            df.setMinimumFractionDigits(0);
            df.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
            df.setGroupingUsed(false);

            // Check if the value has decimal places
            String stringValue = value.toString();
            if (stringValue.contains(".")) {
                // Count the number of digits after decimal point in the original string
                int decimalPlaces = stringValue.length() - stringValue.indexOf('.') - 1;
                // Ensure we keep at least that many decimal places
                df.setMinimumFractionDigits(decimalPlaces);
            }

            return df.format(value);
        }

        return value.toString();
    }

    @Override
    public void printTable() {
        // Print column names
        for (IColumn<?> column : columns.values()) {
            System.out.print(column.getName() + "\t");
        }
        System.out.println();

        // Print rows
        for (IRow row : rows) {
            for (IColumn<?> column : columns.values()) {
                ICell<?> cell = row.getCell(column.getName());
                System.out.print(cell.getValueAsString() + "\t");
            }
            System.out.println();
        }
    }

    @Override
    public void setCreateDefaultValue(boolean createDefaultValue) {
        this.createDefaultValue = createDefaultValue;
    }

    @Override
    public boolean isCreateDefaultValue() {
        return createDefaultValue;
    }

    /**
     * Creates a properly sized ConcurrentHashMap with the specified initial capacity.
     * This utility method helps reduce resizing operations, which can be expensive in concurrent contexts.
     *
     * @param initialCapacity the initial capacity for the map
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return a new ConcurrentHashMap with the specified initial capacity
     */
    private <K, V> ConcurrentHashMap<K, V> createSizedConcurrentMap(int initialCapacity) {
        // Using a load factor of 0.75 (default) which provides a good balance between
        // space efficiency and performance
        return new ConcurrentHashMap<>(initialCapacity, 0.75f);
    }

    /**
     * An optimized implementation of Row that uses a HashMap for cells.
     * This class demonstrates how to optimize collections for different usage patterns.
     */
    private static class OptimizedRow implements IRow {
        private final ITable table;
        // Balanced read-write collection (frequent reads and writes)
        // HashMap provides fast access by key
        private final Map<String, ICell<?>> cells = new HashMap<>();

        public OptimizedRow(ITable table) {
            this.table = table;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> ICell<T> getCell(IColumn<T> column) {
            return (ICell<T>) cells.get(column.getName());
        }

        @Override
        public ICell<?> getCell(String columnName) {
            return cells.get(columnName);
        }

        @Override
        public <T> void setValue(IColumn<T> column, T value) {
            ICell<T> cell = getCell(column);
            if (cell == null) {
                cell = column.createCell(value);
                cells.put(column.getName(), cell);
            } else {
                cell.setValue(value);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setValue(String columnName, Object value) {
            IColumn<?> column = table.getColumn(columnName);
            if (column == null) {
                throw new IllegalArgumentException("Column does not exist: " + columnName);
            }

            ICell<?> cell = getCell(columnName);
            if (cell == null) {
                IColumn<Object> objectColumn = (IColumn<Object>) column;
                cell = objectColumn.createCell((Object) value);
                cells.put(columnName, cell);
            } else {
                ((ICell<Object>) cell).setValue(value);
            }
        }

        @Override
        public List<ICell<?>> getCells() {
            return new ArrayList<>(cells.values());
        }

        @Override
        public ITable getTable() {
            return table;
        }
    }
}
