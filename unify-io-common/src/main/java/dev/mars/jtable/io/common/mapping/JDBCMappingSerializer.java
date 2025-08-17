package dev.mars.jtable.io.common.mapping;

import dev.mars.jtable.io.common.datasource.DbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * JDBC implementation of the mapping serializer.
 * This class serializes and deserializes mapping configurations to and from a JDBC data source.
 * It stores mapping configurations in a database table.
 */
public class JDBCMappingSerializer implements IMappingSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(JDBCMappingSerializer.class);
    
    private static final String DEFAULT_TABLE_NAME = "mapping_configurations";
    private static final String CREATE_TABLE_SQL = 
            "CREATE TABLE IF NOT EXISTS %s (" +
            "id VARCHAR(255) PRIMARY KEY, " +
            "name VARCHAR(255), " +
            "source_location VARCHAR(255), " +
            "configuration TEXT, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
    private static final String INSERT_SQL = 
            "INSERT INTO %s (id, name, source_location, configuration) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_SQL = 
            "UPDATE %s SET name = ?, source_location = ?, configuration = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    private static final String SELECT_SQL = 
            "SELECT id, name, source_location, configuration FROM %s WHERE id = ?";
    private static final String SELECT_ALL_SQL = 
            "SELECT id, name, source_location, configuration FROM %s";

    private final DbConnection dbConnection;
    private final String tableName;
    private final JSONMappingSerializer jsonSerializer;


    public JDBCMappingSerializer(DbConnection dbConnection) {
        if (dbConnection == null) {
            throw new IllegalArgumentException("DbConnection cannot be null");
        }

        this.dbConnection = dbConnection;
        this.tableName = DEFAULT_TABLE_NAME;
        this.jsonSerializer = new JSONMappingSerializer();
    }


    /**
     * Creates a new JDBCMappingSerializer with the specified connection parameters.
     * 
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     * @param tableName the name of the table to store mapping configurations in
     */
    public JDBCMappingSerializer(String connectionString, String username, String password, String tableName) {
        this.dbConnection = new DbConnection(connectionString, username, password);
        this.tableName = tableName != null ? tableName : DEFAULT_TABLE_NAME;
        this.jsonSerializer = new JSONMappingSerializer();
        
        try {
            initializeDatabase();
        } catch (SQLException e) {
            logger.error("Error initializing database: {}", e.getMessage());
        }
    }

    public JDBCMappingSerializer(String connectionString, String username, String password) {
        this.dbConnection = new DbConnection(connectionString, username, password);
        this.tableName = DEFAULT_TABLE_NAME;
        this.jsonSerializer = new JSONMappingSerializer();

        try {
            initializeDatabase();
        } catch (SQLException e) {
            logger.error("Error initializing database: {}", e.getMessage());
        }
    }


    /**
     * Initializes the database by creating the mapping configurations table if it doesn't exist.
     * 
     * @throws SQLException if there is an error initializing the database
     */
    private void initializeDatabase() throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(String.format(CREATE_TABLE_SQL, tableName));
        }
    }
    
    /**
     * Gets a database connection.
     * 
     * @return a database connection
     * @throws SQLException if there is an error getting a connection
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbConnection.getConnectionString());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormat() {
        return "jdbc";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(MappingConfiguration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }
        
        // Use the JSON serializer to serialize the configuration to a string
        return jsonSerializer.serialize(config);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration deserialize(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        // Use the JSON serializer to deserialize the configuration from a string
        return jsonSerializer.deserialize(content);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(MappingConfiguration config, OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException("Writing to an output stream is not supported for JDBC serialization");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration read(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException("Reading from an input stream is not supported for JDBC serialization");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToFile(MappingConfiguration config, String filePath) throws IOException {
        throw new UnsupportedOperationException("Writing to a file is not supported for JDBC serialization");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration readFromFile(String filePath) throws IOException {
        throw new UnsupportedOperationException("Reading from a file is not supported for JDBC serialization");
    }
    
    /**
     * Writes a mapping configuration to the database.
     * 
     * @param id the ID of the mapping configuration
     * @param name the name of the mapping configuration
     * @param config the mapping configuration to write
     * @throws IOException if there is an error writing to the database
     */
    public void writeToDatabase(String id, String name, MappingConfiguration config) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }
        
        String serialized = serialize(config);
        
        try (Connection connection = getConnection()) {
            // Check if the configuration already exists
            boolean exists = false;
            try (PreparedStatement statement = connection.prepareStatement(String.format(SELECT_SQL, tableName))) {
                statement.setString(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    exists = resultSet.next();
                }
            }
            
            // Insert or update the configuration
            String sql = exists ? String.format(UPDATE_SQL, tableName) : String.format(INSERT_SQL, tableName);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                if (exists) {
                    statement.setString(1, name);
                    statement.setString(2, config.getSourceLocation());
                    statement.setString(3, serialized);
                    statement.setString(4, id);
                } else {
                    statement.setString(1, id);
                    statement.setString(2, name);
                    statement.setString(3, config.getSourceLocation());
                    statement.setString(4, serialized);
                }
                statement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.error("Error writing to database: {}", e.getMessage());
            throw new IOException("Error writing to database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reads a mapping configuration from the database.
     * 
     * @param id the ID of the mapping configuration to read
     * @return the mapping configuration, or null if not found
     * @throws IOException if there is an error reading from the database
     */
    public MappingConfiguration readFromDatabase(String id) throws IOException {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }
        
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(String.format(SELECT_SQL, tableName))) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String serialized = resultSet.getString("configuration");
                    return deserialize(serialized);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading from database: {}", e.getMessage());
            throw new IOException("Error reading from database: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reads all mapping configurations from the database.
     * 
     * @return a map of mapping configuration IDs to mapping configurations
     * @throws IOException if there is an error reading from the database
     */
    public Map<String, MappingConfiguration> readAllFromDatabase() throws IOException {
        Map<String, MappingConfiguration> configurations = new HashMap<>();
        
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(String.format(SELECT_ALL_SQL, tableName))) {
            while (resultSet.next()) {
                String id = resultSet.getString("id");
                String serialized = resultSet.getString("configuration");
                MappingConfiguration config = deserialize(serialized);
                configurations.put(id, config);
            }
            return configurations;
        } catch (SQLException e) {
            logger.error("Error reading from database: {}", e.getMessage());
            throw new IOException("Error reading from database: " + e.getMessage(), e);
        }
    }
}