
# Data Source Connection Abstraction Design

After analyzing the current structure of the datasource package, I propose adding a new abstraction that represents a connection to a data source. This abstraction will work for various database types, file formats (CSV, JSON, XML), and locations (local, network drive, web URL).

## Current Structure Analysis

The current system has:
- `IDataSource` interface defining common methods for all data sources
- Specific interfaces for different data source types (CSV, JSON, XML, JDBC) that extend `IDataSource`
- `IDataReader` and `IDataWriter` interfaces for reading from and writing to data sources
- Connection details (connection strings, file paths, etc.) passed as parameters to reader/writer methods

## Proposed Solution: IDataSourceConnection Interface

I propose creating a new interface called `IDataSourceConnection` that abstracts the connection details:

```java
package dev.mars.jtable.io.datasource;

import java.util.Map;

/**
 * Interface representing a connection to a data source.
 * This abstraction handles the details of connecting to various types of data sources
 * such as databases, files (CSV, JSON, XML), web services, etc.
 */
public interface IDataSourceConnection {
    /**
     * Establishes a connection to the data source.
     * 
     * @return true if connection was successful, false otherwise
     */
    boolean connect();
    
    /**
     * Closes the connection to the data source.
     */
    void disconnect();
    
    /**
     * Checks if the connection is currently active.
     * 
     * @return true if connected, false otherwise
     */
    boolean isConnected();
    
    /**
     * Gets the connection type (e.g., "jdbc", "csv", "json", "xml", "http", etc.)
     * 
     * @return the connection type as a string
     */
    String getConnectionType();
    
    /**
     * Gets the raw connection object if applicable (e.g., JDBC Connection object)
     * 
     * @return the raw connection object, or null if not applicable
     */
    Object getRawConnection();
    
    /**
     * Gets connection-specific properties.
     * 
     * @return a map of connection properties
     */
    Map<String, Object> getProperties();
}
```

## Specific Implementations

### JDBCConnection

```java
package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.io.datasource.IDataSourceConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of IDataSourceConnection for JDBC database connections.
 */
public class JDBCConnection implements IDataSourceConnection {
    private String connectionString;
    private String username;
    private String password;
    private Connection connection;
    private Map<String, Object> properties;
    
    public JDBCConnection(String connectionString, String username, String password) {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        this.properties = new HashMap<>();
    }
    
    @Override
    public boolean connect() {
        try {
            connection = DriverManager.getConnection(connectionString, username, password);
            return true;
        } catch (SQLException e) {
            System.err.println("Error connecting to database: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (SQLException e) {
                System.err.println("Error disconnecting from database: " + e.getMessage());
            }
        }
    }
    
    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public String getConnectionType() {
        return "jdbc";
    }
    
    @Override
    public Object getRawConnection() {
        return connection;
    }
    
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    // Additional JDBC-specific methods
    public String getConnectionString() {
        return connectionString;
    }
    
    public String getUsername() {
        return username;
    }
}
```

### FileConnection (for CSV, JSON, XML)

```java
package dev.mars.jtable.io.file;

import dev.mars.jtable.io.datasource.IDataSourceConnection;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of IDataSourceConnection for file-based data sources.
 * Supports local files, network shares, and HTTP/HTTPS URLs.
 */
public class FileConnection implements IDataSourceConnection {
    private String location;
    private String fileType;
    private Path filePath;
    private URL fileUrl;
    private boolean isConnected;
    private Map<String, Object> properties;
    
    /**
     * Creates a new file connection.
     * 
     * @param location the file location (path or URL)
     * @param fileType the file type (e.g., "csv", "json", "xml")
     */
    public FileConnection(String location, String fileType) {
        this.location = location;
        this.fileType = fileType.toLowerCase();
        this.properties = new HashMap<>();
    }
    
    @Override
    public boolean connect() {
        try {
            // Check if it's a URL
            if (location.startsWith("http://") || location.startsWith("https://")) {
                fileUrl = new URL(location);
                // Test connection by opening and closing a stream
                fileUrl.openStream().close();
                isConnected = true;
            } else {
                // Treat as a file path
                filePath = Paths.get(location);
                isConnected = Files.exists(filePath);
            }
            return isConnected;
        } catch (IOException e) {
            System.err.println("Error connecting to file: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }
    
    @Override
    public void disconnect() {
        isConnected = false;
    }
    
    @Override
    public boolean isConnected() {
        return isConnected;
    }
    
    @Override
    public String getConnectionType() {
        return fileType;
    }
    
    @Override
    public Object getRawConnection() {
        return fileUrl != null ? fileUrl : filePath;
    }
    
    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    // Additional file-specific methods
    public String getLocation() {
        return location;
    }
    
    public boolean isRemote() {
        return fileUrl != null;
    }
}
```

## Updated Reader/Writer Interfaces

The `IDataReader` and `IDataWriter` interfaces would be updated to use the new connection abstraction:

```java
public interface IDataReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options);
}

public interface IDataWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options);
}
```

## Connection Factory

To simplify connection creation, a factory pattern could be implemented:

```java
package dev.mars.jtable.io.datasource;

/**
 * Factory for creating data source connections.
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
            // Determine type from Content-Type header or extension
            String extension = location.substring(location.lastIndexOf('.') + 1);
            return new FileConnection(location, extension);
        } else {
            throw new IllegalArgumentException("Unsupported location format: " + location);
        }
    }
}
```

## Benefits of This Design

1. **Separation of Concerns**: Connection details are separated from data reading/writing logic
2. **Unified Interface**: Common interface for all connection types
3. **Extensibility**: Easy to add new connection types (e.g., NoSQL databases, web services)
4. **Resource Management**: Explicit connect/disconnect methods for proper resource handling
5. **Connection Pooling**: Could be implemented in the future for database connections
6. **Testing**: Easier to mock connections for testing

This design provides a clean abstraction for data source connections while maintaining compatibility with the existing system.