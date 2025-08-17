package dev.mars.jtable.core.exception;

/**
 * Exception thrown when there are issues with table columns.
 * This includes column creation, validation, type conversion, and access errors.
 */
public class ColumnException extends TableException {
    
    private final String columnName;
    
    /**
     * Creates a new ColumnException with the specified message.
     *
     * @param message the detail message
     */
    public ColumnException(String message) {
        super(message);
        this.columnName = null;
    }
    
    /**
     * Creates a new ColumnException with the specified message and column name.
     *
     * @param message the detail message
     * @param columnName the name of the column that caused the exception
     */
    public ColumnException(String message, String columnName) {
        super(message + (columnName != null ? " (Column: " + columnName + ")" : ""));
        this.columnName = columnName;
    }
    
    /**
     * Creates a new ColumnException with the specified message, column name, and cause.
     *
     * @param message the detail message
     * @param columnName the name of the column that caused the exception
     * @param cause the cause of this exception
     */
    public ColumnException(String message, String columnName, Throwable cause) {
        super(message + (columnName != null ? " (Column: " + columnName + ")" : ""), cause);
        this.columnName = columnName;
    }
    
    /**
     * Gets the name of the column that caused this exception.
     *
     * @return the column name, or null if not specified
     */
    public String getColumnName() {
        return columnName;
    }
}
