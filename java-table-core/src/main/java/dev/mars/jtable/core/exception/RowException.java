package dev.mars.jtable.core.exception;

/**
 * Exception thrown when there are issues with table rows.
 * This includes row creation, validation, and access errors.
 */
public class RowException extends TableException {
    
    private final int rowIndex;
    
    /**
     * Creates a new RowException with the specified message.
     *
     * @param message the detail message
     */
    public RowException(String message) {
        super(message);
        this.rowIndex = -1;
    }
    
    /**
     * Creates a new RowException with the specified message and row index.
     *
     * @param message the detail message
     * @param rowIndex the index of the row that caused the exception
     */
    public RowException(String message, int rowIndex) {
        super(message + (rowIndex >= 0 ? " (Row: " + rowIndex + ")" : ""));
        this.rowIndex = rowIndex;
    }
    
    /**
     * Creates a new RowException with the specified message, row index, and cause.
     *
     * @param message the detail message
     * @param rowIndex the index of the row that caused the exception
     * @param cause the cause of this exception
     */
    public RowException(String message, int rowIndex, Throwable cause) {
        super(message + (rowIndex >= 0 ? " (Row: " + rowIndex + ")" : ""), cause);
        this.rowIndex = rowIndex;
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
