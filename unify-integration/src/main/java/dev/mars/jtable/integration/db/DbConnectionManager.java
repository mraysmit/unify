package dev.mars.jtable.integration.db;

import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Manager class for creating and managing database connections.
 * This class separates the database connection concerns from the processing logic.
 */
public class DbConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(DbConnectionManager.class);

    /**
     * Creates a SQLite database connection using the default properties.
     * 
     * @return a new DbConnection configured for SQLite
     * @throws SQLException if there is an error connecting to the database
     * @deprecated Use {@link #createSQLiteConnection(DatabaseProperties)} instead to follow dependency injection principles
     */
    @Deprecated
    public DbConnection createSQLiteConnection() throws SQLException {
        return createSQLiteConnection(new DatabaseProperties());
    }

    /**
     * Creates a SQLite database connection using the provided database properties.
     *
     * @param dbProperties the database properties to use
     * @return a new DbConnection configured for SQLite
     * @throws SQLException if there is an error connecting to the database
     */
    public DbConnection createSQLiteConnection(DatabaseProperties dbProperties) throws SQLException {
        String connectionString = dbProperties.getSqliteConnectionString();
        String username = dbProperties.getSqliteUsername();
        String password = dbProperties.getSqlitePassword();
        logger.info("Creating SQLite connection: {}", connectionString);

        return createConnection(connectionString, username, password);
    }

    /**
     * Creates an H2 database connection using the default properties.
     *
     * @return a new DbConnection configured for H2
     * @throws SQLException if there is an error connecting to the database
     * @deprecated Use {@link #createH2Connection(DatabaseProperties)} instead to follow dependency injection principles
     */
    @Deprecated
    public DbConnection createH2Connection() throws SQLException {
        return createH2Connection(new DatabaseProperties());
    }

    /**
     * Creates an H2 database connection using the provided database properties.
     *
     * @param dbProperties the database properties to use
     * @return a new DbConnection configured for H2
     * @throws SQLException if there is an error connecting to the database
     */
    public DbConnection createH2Connection(DatabaseProperties dbProperties) throws SQLException {
        String connectionString = dbProperties.getH2ConnectionString();
        String username = dbProperties.getH2Username();
        String password = dbProperties.getH2Password();
        logger.info("Creating H2 connection: {}", connectionString);

        return createConnection(connectionString, username, password);
    }

    /**
     * Creates a database connection with the specified connection parameters.
     *
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     * @return a new DbConnection
     * @throws SQLException if there is an error connecting to the database
     */
    public DbConnection createConnection(String connectionString, String username, String password) throws SQLException {
        logger.debug("Creating database connection: {}", connectionString);
        DbConnection connection = new DbConnection(connectionString, username, password);
        return connection;
    }

    /**
     * Creates a database connection from a properties file.
     *
     * @param propertiesFilePath the path to the properties file
     * @param dbType the database type (e.g., "sqlite", "h2")
     * @return a new DbConnection
     * @throws IOException if there is an error loading the properties
     * @throws SQLException if there is an error connecting to the database
     */
    public DbConnection createConnectionFromProperties(String propertiesFilePath, String dbType) throws IOException, SQLException {
        logger.debug("Creating database connection from properties file: {} for type: {}", propertiesFilePath, dbType);
        DbConnection connection = DbConnection.createConnection(propertiesFilePath, dbType);
        return connection;
    }

    /**
     * Connects to a database using the provided connection.
     *
     * @param connection the database connection to use
     * @return true if the connection was successful, false otherwise
     * @throws SQLException if there is an error connecting to the database
     */
    public boolean connect(DbConnection connection) throws SQLException {
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        if (connection.isConnected()) {
            logger.debug("Connection is already established");
            return true;
        }

        boolean connected = connection.connect();
        if (connected) {
            logger.debug("Successfully connected to database: {}", connection.getConnectionString());
        } else {
            logger.error("Failed to connect to database: {}", connection.getConnectionString());
            throw new SQLException("Failed to connect to database: " + connection.getConnectionString());
        }

        return connected;
    }

    /**
     * Disconnects from a database using the provided connection.
     *
     * @param connection the database connection to use
     */
    public void disconnect(DbConnection connection) {
        if (connection == null) {
            logger.warn("Cannot disconnect null connection");
            return;
        }

        if (!connection.isConnected()) {
            logger.debug("Connection is already disconnected");
            return;
        }

        connection.disconnect();
        logger.debug("Disconnected from database: {}", connection.getConnectionString());
    }

    /**
     * Ensures that a connection is properly closed, even if an exception occurs.
     * This method should be called in a finally block.
     *
     * @param connection the database connection to close
     */
    public void ensureConnectionClosed(DbConnection connection) {
        if (connection != null && connection.isConnected()) {
            disconnect(connection);
        }
    }
}
