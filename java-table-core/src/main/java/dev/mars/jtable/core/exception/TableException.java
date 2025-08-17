package dev.mars.jtable.core.exception;

/**
 * Base exception class for all table-related exceptions.
 * This is the root of the table exception hierarchy.
 */
public class TableException extends RuntimeException {
    
    /**
     * Creates a new TableException with the specified message.
     *
     * @param message the detail message
     */
    public TableException(String message) {
        super(message);
    }
    
    /**
     * Creates a new TableException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public TableException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Creates a new TableException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public TableException(Throwable cause) {
        super(cause);
    }
}
