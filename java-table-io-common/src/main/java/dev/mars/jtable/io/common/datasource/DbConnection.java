package dev.mars.jtable.io.common.datasource;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Implementation of IDataSourceConnection for JDBC database connections.
 * Supports loading connection properties from external properties files.
 */
public class DbConnection implements IDataSourceConnection {
    private static final Logger logger = LoggerFactory.getLogger(DbConnection.class);
    private String connectionString;
    private String username;
    private String password;
    private Connection connection;
    private Map<String, Object> properties;
    private String dbType;

    /**
     * Creates a new database connection with the specified connection parameters.
     *
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     */
    public DbConnection(String connectionString, String username, String password) {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        this.properties = new HashMap<>();
    }

    /**
     * Creates a new database connection by loading properties from a properties file.
     *
     * @param propertiesFilePath the path to the properties file
     * @param dbType the database type (e.g., "sqlite", "h2") to load properties for
     * @throws IOException if the properties file cannot be loaded
     */
    public DbConnection(String propertiesFilePath, String dbType) throws IOException {
        this.dbType = dbType;
        this.properties = new HashMap<>();
        loadPropertiesFromFile(propertiesFilePath);
    }

    /**
     * Loads database connection properties from a properties file.
     *
     * @param propertiesFilePath the path to the properties file
     * @throws IOException if the properties file cannot be loaded
     */
    private void loadPropertiesFromFile(String propertiesFilePath) throws IOException {
        Properties fileProperties = new Properties();
        try (InputStream input = getPropertiesInputStream(propertiesFilePath)) {
            if (input == null) {
                logger.error("Unable to find properties file: {}", propertiesFilePath);
                throw new IOException("Unable to find properties file: " + propertiesFilePath);
            }

            fileProperties.load(input);
            logger.debug("Successfully loaded properties from {}", propertiesFilePath);

            // Set connection properties based on database type
            this.connectionString = fileProperties.getProperty(dbType + ".connectionString");
            this.username = fileProperties.getProperty(dbType + ".username");
            this.password = fileProperties.getProperty(dbType + ".password");

            if (this.connectionString == null) {
                logger.error("Connection string not found for database type: {}", dbType);
                throw new IOException("Connection string not found for database type: " + dbType);
            }

            logger.debug("Loaded connection properties for database type: {}", dbType);
        }
    }

    /**
     * Gets an input stream for the properties file.
     * First tries to load from the file system, then from the classpath.
     *
     * @param propertiesFilePath the path to the properties file
     * @return an input stream for the properties file
     * @throws IOException if the properties file cannot be loaded
     */
    private InputStream getPropertiesInputStream(String propertiesFilePath) throws IOException {
        // First try to load from file system
        try {
            return new FileInputStream(propertiesFilePath);
        } catch (IOException e) {
            logger.debug("Could not load properties file from file system: {}", e.getMessage());

            // Then try to load from classpath
            InputStream input = DbConnection.class.getClassLoader().getResourceAsStream(propertiesFilePath);
            if (input != null) {
                return input;
            }

            // If still not found, throw exception
            logger.error("Properties file not found: {}", propertiesFilePath);
            throw new IOException("Properties file not found: " + propertiesFilePath);
        }
    }

    @Override
    public boolean connect() {
        try {
            connection = DriverManager.getConnection(connectionString, username, password);
            return true;
        } catch (SQLException e) {
            logger.error("Error connecting to database: {}", e.getMessage());
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
                logger.error("Error disconnecting from database: {}", e.getMessage());
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

    /**
     * Gets the password for this connection.
     * 
     * @return the database password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the database type for this connection.
     * 
     * @return the database type (e.g., "sqlite", "h2")
     */
    public String getDbType() {
        return dbType;
    }

    /**
     * Factory method to create a SQLite database connection from a properties file.
     * 
     * @param propertiesFilePath the path to the properties file
     * @return a new DbConnection configured for SQLite
     * @throws IOException if the properties file cannot be loaded
     */
    public static DbConnection createSqliteConnection(String propertiesFilePath) throws IOException {
        return new DbConnection(propertiesFilePath, "sqlite");
    }

    /**
     * Factory method to create an H2 database connection from a properties file.
     * 
     * @param propertiesFilePath the path to the properties file
     * @return a new DbConnection configured for H2
     * @throws IOException if the properties file cannot be loaded
     */
    public static DbConnection createH2Connection(String propertiesFilePath) throws IOException {
        return new DbConnection(propertiesFilePath, "h2");
    }

    /**
     * Factory method to create a database connection from a properties file.
     * 
     * @param propertiesFilePath the path to the properties file
     * @param dbType the database type (e.g., "sqlite", "h2")
     * @return a new DbConnection configured for the specified database type
     * @throws IOException if the properties file cannot be loaded
     */
    public static DbConnection createConnection(String propertiesFilePath, String dbType) throws IOException {
        return new DbConnection(propertiesFilePath, dbType);
    }
}
