package dev.mars.jtable.io.common.exception;

/**
 * Exception thrown when there are issues with database operations.
 * This includes connection errors, SQL execution problems, and JDBC-related issues.
 */
public class DatabaseException extends DataSourceException {
    
    private final String sqlQuery;
    private final String tableName;
    
    /**
     * Creates a new DatabaseException with the specified message.
     *
     * @param message the detail message
     */
    public DatabaseException(String message) {
        super(message);
        this.sqlQuery = null;
        this.tableName = null;
    }
    
    /**
     * Creates a new DatabaseException with the specified message and connection string.
     *
     * @param message the detail message
     * @param connectionString the database connection string
     */
    public DatabaseException(String message, String connectionString) {
        super(message, connectionString);
        this.sqlQuery = null;
        this.tableName = null;
    }
    
    /**
     * Creates a new DatabaseException with the specified message, connection string, and table name.
     *
     * @param message the detail message
     * @param connectionString the database connection string
     * @param tableName the name of the table involved in the operation
     */
    public DatabaseException(String message, String connectionString, String tableName) {
        super(message + (tableName != null ? " (Table: " + tableName + ")" : ""), connectionString);
        this.sqlQuery = null;
        this.tableName = tableName;
    }
    
    /**
     * Creates a new DatabaseException with the specified message, connection string, SQL query, and cause.
     *
     * @param message the detail message
     * @param connectionString the database connection string
     * @param sqlQuery the SQL query that caused the error
     * @param cause the cause of this exception
     */
    public DatabaseException(String message, String connectionString, String sqlQuery, Throwable cause) {
        super(message + (sqlQuery != null ? " (Query: " + sqlQuery + ")" : ""), connectionString, cause);
        this.sqlQuery = sqlQuery;
        this.tableName = null;
    }
    
    /**
     * Gets the SQL query that caused this exception.
     *
     * @return the SQL query, or null if not specified
     */
    public String getSqlQuery() {
        return sqlQuery;
    }
    
    /**
     * Gets the table name involved in the operation that caused this exception.
     *
     * @return the table name, or null if not specified
     */
    public String getTableName() {
        return tableName;
    }
}
