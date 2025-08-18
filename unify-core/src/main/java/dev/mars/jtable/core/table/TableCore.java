/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.ICell;
import dev.mars.jtable.core.model.IColumn;
import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.model.ITable;

import java.util.*;

public class TableCore implements ITable {
    private final Map<String, IColumn<?>> columns = new LinkedHashMap<>();
    private final List<IRow> rows = new ArrayList<>();
    private boolean createDefaultValue = true;
    private String name;

    // Constants for Double handling
    private static final int MAX_FRACTION_DIGITS = 10;

    // Map to store original string representations of double values
    private final java.util.Map<String, java.util.Map<Integer, String>> originalDoubleStrings = new java.util.HashMap<>();

    public TableCore() {
        this.name = "TableCore";
    }

    /**
     * Creates a new TableCore with the specified name.
     *
     * @param name the name of the table
     */
    public TableCore(String name) {
        this.name = name;
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
        Map<String, String> rowCopy = new HashMap<>(row);

        // Check if all required columns are present
        for (IColumn<?> column : columns.values()) {
            if (!rowCopy.containsKey(column.getName())) {
                if (createDefaultValue) {
                    // Add default value for missing column
                    String columnType = "";
                    if (column instanceof Column) {
                        // Get the column type from the Column class
                        if (column.getClass().getGenericSuperclass() instanceof java.lang.reflect.ParameterizedType) {
                            java.lang.reflect.ParameterizedType paramType = (java.lang.reflect.ParameterizedType) column.getClass().getGenericSuperclass();
                            Class<?> valueType = (Class<?>) paramType.getActualTypeArguments()[0];
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
                originalDoubleStrings.computeIfAbsent(columnName, k -> new java.util.HashMap<>());
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
        return new Row(this);
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
            originalDoubleStrings.computeIfAbsent(columnName, k -> new java.util.HashMap<>());
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
        // Note: We don't treat values with a leading plus sign as integers
        // to match the expectations of the testInferTypeStringProperty test
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
        // Date pattern: yyyy-MM-dd (ISO_LOCAL_DATE)
        else if (trimmedValue.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            try {
                java.time.LocalDate.parse(trimmedValue);
                return "date";
            } catch (java.time.format.DateTimeParseException e) {
                // If parsing fails, it's not a valid date
                return "string";
            }
        }
        // Time pattern: HH:mm:ss (ISO_LOCAL_TIME)
        else if (trimmedValue.matches("^\\d{2}:\\d{2}:\\d{2}$")) {
            try {
                java.time.LocalTime.parse(trimmedValue);
                return "time";
            } catch (java.time.format.DateTimeParseException e) {
                // If parsing fails, it's not a valid time
                return "string";
            }
        }
        // DateTime pattern: yyyy-MM-ddTHH:mm:ss (ISO_LOCAL_DATE_TIME)
        else if (trimmedValue.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")) {
            try {
                java.time.LocalDateTime.parse(trimmedValue);
                return "datetime";
            } catch (java.time.format.DateTimeParseException e) {
                // If parsing fails, it's not a valid datetime
                return "string";
            }
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
            case "date":
                return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            case "time":
                return java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
            case "datetime":
                return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
        // Handling for LocalDate values
        else if (value instanceof java.time.LocalDate) {
            return ((java.time.LocalDate) value).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        }
        // Handling for LocalTime values
        else if (value instanceof java.time.LocalTime) {
            return ((java.time.LocalTime) value).format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        }
        // Handling for LocalDateTime values
        else if (value instanceof java.time.LocalDateTime) {
            return ((java.time.LocalDateTime) value).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        return value.toString();
    }

    /**
     * Prints the table to standard output.
     *
     * <p>This method is designed for extension. Subclasses can override this method
     * to customize the table printing behavior. The default implementation prints
     * column headers followed by all rows with tab-separated values.</p>
     *
     * <p>When overriding this method, ensure that:</p>
     * <ul>
     * <li>The table structure is properly displayed</li>
     * <li>Column headers are included if needed</li>
     * <li>Row data is formatted consistently</li>
     * </ul>
     */
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

    /**
     * Sets whether to create default values for missing columns.
     *
     * @param createDefaultValue whether to create default values for missing
     *                          columns
     */
    public void setCreateDefaultValue(final boolean createDefaultValue) {
        this.createDefaultValue = createDefaultValue;
    }

    /**
     * Gets whether to create default values for missing columns.
     *
     * @return whether to create default values for missing columns
     */
    public boolean isCreateDefaultValue() {
        return createDefaultValue;
    }

    /**
     * Gets the name of this table.
     *
     * <p>This method is designed for extension. Subclasses can override this method
     * to customize how the table name is retrieved or computed.</p>
     *
     * @return the name of this table
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this table.
     *
     * <p>This method is designed for extension. Subclasses can override this method
     * to customize how the table name is set or to add validation logic.</p>
     *
     * @param tableName the new name for this table
     */
    @Override
    public void setName(final String tableName) {
        this.name = tableName;
    }
}
