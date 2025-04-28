package dev.mars.jtable.io.common.datasource;



/**
 * Factory for creating data source connections.
 * Separation of Concerns: Connection details are separated from data reading/writing logic
 * Unified Interface: Common interface for all connection types
 * Extensibility: Easy to add new connection types (e.g., NoSQL databases, web services)
 * Resource Management: Explicit connect/disconnect methods for proper resource handling
 * Connection Pooling: Could be implemented in the future for database connections
 * Testing: Easier to mock connections for testing
 */
public class DataSourceConnectionFactory {
    /**
     * Creates a database connection.
     *
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     * @return a JDBC connection
     */
    public static IDataSourceConnection createDatabaseConnection(String connectionString, String username, String password) {
        return new JDBCConnection(connectionString, username, password);
    }

    /**
     * Creates a file connection.
     *
     * @param location the file location (path or URL)
     * @param fileType the file type (e.g., "csv", "json", "xml")
     * @return a file connection
     */
    public static IDataSourceConnection createFileConnection(String location, String fileType) {
        return new FileConnection(location, fileType);
    }

    /**
     * Creates a REST API connection.
     *
     * @param endpoint the REST API endpoint URL
     * @return a REST connection
     */
    public static IDataSourceConnection createRESTConnection(String endpoint) {
        return new RESTConnection(endpoint);
    }

    /**
     * Creates a REST API connection with authentication.
     *
     * @param endpoint the REST API endpoint URL
     * @param authToken the authentication token
     * @return a REST connection
     */
    public static IDataSourceConnection createRESTConnection(String endpoint, String authToken) {
        return new RESTConnection(endpoint, authToken);
    }

    /**
     * Creates a NoSQL database connection.
     *
     * @param connectionString the connection string
     * @param database the database name
     * @param collection the collection name
     * @return a NoSQL connection
     */
    public static IDataSourceConnection createNoSQLConnection(String connectionString, String database, String collection) {
        return new NoSQLConnection(connectionString, database, collection);
    }

    /**
     * Creates a NoSQL database connection with authentication.
     *
     * @param connectionString the connection string
     * @param database the database name
     * @param collection the collection name
     * @param username the username
     * @param password the password
     * @return a NoSQL connection
     */
    public static IDataSourceConnection createNoSQLConnection(String connectionString, String database, String collection, String username, String password) {
        return new NoSQLConnection(connectionString, database, collection, username, password);
    }

    /**
     * Creates a connection based on the location format.
     * Automatically detects the connection type based on the location.
     *
     * @param location the data source location
     * @return an appropriate connection
     */
    public static IDataSourceConnection createConnection(String location) {
        if (location.startsWith("jdbc:")) {
            return new JDBCConnection(location, "", "");
        } else if (location.endsWith(".csv")) {
            return new FileConnection(location, "csv");
        } else if (location.endsWith(".json")) {
            return new FileConnection(location, "json");
        } else if (location.endsWith(".xml")) {
            return new FileConnection(location, "xml");
        } else if (location.startsWith("http://") || location.startsWith("https://")) {
            if (location.contains("/api/") || location.contains("/rest/")) {
                // Likely a REST API endpoint
                return new RESTConnection(location);
            } else {
                // Determine type from Content-Type header or extension
                String extension = location.substring(location.lastIndexOf('.') + 1);
                return new FileConnection(location, extension);
            }
        } else if (location.startsWith("mongodb://") || location.startsWith("mongodb+srv://")) {
            // MongoDB connection string
            // For simplicity, we'll use default database and collection names
            return new NoSQLConnection(location, "default", "default");
        } else {
            throw new IllegalArgumentException("Unsupported location format: " + location);
        }
    }
}
