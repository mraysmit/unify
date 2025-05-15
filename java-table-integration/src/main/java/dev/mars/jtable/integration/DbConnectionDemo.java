package dev.mars.jtable.integration;

import dev.mars.jtable.io.common.datasource.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Demonstration of how to use DbConnection to load database connection properties
 * directly from a properties file.
 */
public class DbConnectionDemo {
    private static final Logger logger = LoggerFactory.getLogger(DbConnectionDemo.class);
    
    // Default properties file path
    private static final String DEFAULT_PROPERTIES_FILE = "db.properties";
    
    /**
     * Main method to run the demonstration.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting DbConnection demonstration");
            
            // Demonstrate loading SQLite connection properties from a file
            demonstrateSqliteConnection();
            
            // Demonstrate loading H2 connection properties from a file
            demonstrateH2Connection();
            
            // Demonstrate loading connection properties for a custom database type
            demonstrateCustomConnection("mysql");
            
            logger.info("DbConnection demonstration completed successfully");
        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Demonstrates how to create and use a SQLite database connection
     * with properties loaded from a file.
     *
     * @throws IOException if there is an error loading the properties
     * @throws SQLException if there is an error connecting to the database
     */
    private static void demonstrateSqliteConnection() throws IOException, SQLException {
        logger.info("Demonstrating SQLite connection with properties from file");
        
        // Create a SQLite connection using the factory method
        DbConnection sqliteConnection = DbConnection.createSqliteConnection(DEFAULT_PROPERTIES_FILE);
        
        // Log the connection details
        logger.info("SQLite Connection String: {}", sqliteConnection.getConnectionString());
        logger.info("SQLite Username: {}", sqliteConnection.getUsername());
        logger.info("SQLite Password: {}", 
                sqliteConnection.getPassword() != null && !sqliteConnection.getPassword().isEmpty() 
                ? "********" : "(empty)");
        
        // Try to connect to the database
        boolean connected = sqliteConnection.connect();
        logger.info("SQLite Connection successful: {}", connected);
        
        // Disconnect if connected
        if (connected) {
            sqliteConnection.disconnect();
            logger.info("Disconnected from SQLite database");
        }
    }
    
    /**
     * Demonstrates how to create and use an H2 database connection
     * with properties loaded from a file.
     *
     * @throws IOException if there is an error loading the properties
     * @throws SQLException if there is an error connecting to the database
     */
    private static void demonstrateH2Connection() throws IOException, SQLException {
        logger.info("Demonstrating H2 connection with properties from file");
        
        // Create an H2 connection using the factory method
        DbConnection h2Connection = DbConnection.createH2Connection(DEFAULT_PROPERTIES_FILE);
        
        // Log the connection details
        logger.info("H2 Connection String: {}", h2Connection.getConnectionString());
        logger.info("H2 Username: {}", h2Connection.getUsername());
        logger.info("H2 Password: {}", 
                h2Connection.getPassword() != null && !h2Connection.getPassword().isEmpty() 
                ? "********" : "(empty)");
        
        // Try to connect to the database
        boolean connected = h2Connection.connect();
        logger.info("H2 Connection successful: {}", connected);
        
        // Disconnect if connected
        if (connected) {
            h2Connection.disconnect();
            logger.info("Disconnected from H2 database");
        }
    }
    
    /**
     * Demonstrates how to create and use a database connection for a custom database type
     * with properties loaded from a file.
     *
     * @param dbType the database type to use
     * @throws IOException if there is an error loading the properties
     */
    private static void demonstrateCustomConnection(String dbType) throws IOException {
        logger.info("Demonstrating {} connection with properties from file", dbType);
        
        try {
            // Create a connection for the custom database type
            DbConnection customConnection = DbConnection.createConnection(DEFAULT_PROPERTIES_FILE, dbType);
            
            // Log the connection details
            logger.info("{} Connection String: {}", dbType, customConnection.getConnectionString());
            logger.info("{} Username: {}", dbType, customConnection.getUsername());
            logger.info("{} Password: {}", dbType, 
                    customConnection.getPassword() != null && !customConnection.getPassword().isEmpty() 
                    ? "********" : "(empty)");
            
            // Note: We don't try to connect here since the properties file might not have
            // entries for this custom database type
        } catch (IOException e) {
            logger.warn("Could not create connection for database type {}: {}", dbType, e.getMessage());
            logger.info("This is expected if the properties file doesn't contain entries for this database type");
        }
    }
}