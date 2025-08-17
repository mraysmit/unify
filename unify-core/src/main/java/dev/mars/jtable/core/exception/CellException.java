package dev.mars.jtable.core.exception;

/**
 * Exception thrown when there are issues with table cells.
 * This includes cell value validation, type conversion, and access errors.
 */
public class CellException extends TableException {
    
    private final String columnName;
    private final int rowIndex;
    
    /**
     * Creates a new CellException with the specified message.
     *
     * @param message the detail message
     */
    public CellException(String message) {
        super(message);
        this.columnName = null;
        this.rowIndex = -1;
    }
    
    /**
     * Creates a new CellException with the specified message, column name, and row index.
     *
     * @param message the detail message
     * @param columnName the name of the column
     * @param rowIndex the index of the row
     */
    public CellException(String message, String columnName, int rowIndex) {
        super(message + " (Cell: " + columnName + "[" + rowIndex + "])");
        this.columnName = columnName;
        this.rowIndex = rowIndex;
    }
    
    /**
     * Creates a new CellException with the specified message, column name, row index, and cause.
     *
     * @param message the detail message
     * @param columnName the name of the column
     * @param rowIndex the index of the row
     * @param cause the cause of this exception
     */
    public CellException(String message, String columnName, int rowIndex, Throwable cause) {
        super(message + " (Cell: " + columnName + "[" + rowIndex + "])", cause);
        this.columnName = columnName;
        this.rowIndex = rowIndex;
    }
    
    /**
     * Gets the name of the column that caused this exception.
     *
     * @return the column name, or null if not specified
     */
    public String getColumnName() {
        return columnName;
    }
    
    /**
     * Gets the index of the row that caused this exception.
     *
     * @return the row index, or -1 if not specified
     */
    public int getRowIndex() {
        return rowIndex;
    }
}
