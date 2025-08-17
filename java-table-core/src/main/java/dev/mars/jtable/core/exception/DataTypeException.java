package dev.mars.jtable.core.exception;

/**
 * Exception thrown when there are issues with data type conversion or validation.
 * This includes type inference, string-to-type conversion, and type compatibility errors.
 */
public class DataTypeException extends ColumnException {
    
    private final String sourceValue;
    private final Class<?> targetType;
    
    /**
     * Creates a new DataTypeException with the specified message.
     *
     * @param message the detail message
     */
    public DataTypeException(String message) {
        super(message);
        this.sourceValue = null;
        this.targetType = null;
    }
    
    /**
     * Creates a new DataTypeException with the specified message and source value.
     *
     * @param message the detail message
     * @param sourceValue the value that could not be converted
     */
    public DataTypeException(String message, String sourceValue) {
        super(message + " (Value: '" + sourceValue + "')");
        this.sourceValue = sourceValue;
        this.targetType = null;
    }
    
    /**
     * Creates a new DataTypeException with the specified message, source value, and target type.
     *
     * @param message the detail message
     * @param sourceValue the value that could not be converted
     * @param targetType the type that the value could not be converted to
     */
    public DataTypeException(String message, String sourceValue, Class<?> targetType) {
        super(message + " (Value: '" + sourceValue + "' -> " + 
              (targetType != null ? targetType.getSimpleName() : "unknown") + ")");
        this.sourceValue = sourceValue;
        this.targetType = targetType;
    }
    
    /**
     * Creates a new DataTypeException with the specified message, source value, target type, and cause.
     *
     * @param message the detail message
     * @param sourceValue the value that could not be converted
     * @param targetType the type that the value could not be converted to
     * @param cause the cause of this exception
     */
    public DataTypeException(String message, String sourceValue, Class<?> targetType, Throwable cause) {
        super(message + " (Value: '" + sourceValue + "' -> " +
              (targetType != null ? targetType.getSimpleName() : "unknown") + ")", null, cause);
        this.sourceValue = sourceValue;
        this.targetType = targetType;
    }
    
    /**
     * Gets the source value that could not be converted.
     *
     * @return the source value, or null if not specified
     */
    public String getSourceValue() {
        return sourceValue;
    }
    
    /**
     * Gets the target type that the value could not be converted to.
     *
     * @return the target type, or null if not specified
     */
    public Class<?> getTargetType() {
        return targetType;
    }
}
