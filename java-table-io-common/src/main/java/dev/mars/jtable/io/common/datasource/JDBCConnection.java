package dev.mars.jtable.io.common.datasource;


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