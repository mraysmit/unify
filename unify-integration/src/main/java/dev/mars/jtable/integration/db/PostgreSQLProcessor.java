/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.integration.db;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.adapter.JDBCTableAdapter;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.files.jdbc.JDBCMappingReader;
import dev.mars.jtable.io.files.jdbc.JDBCMappingWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Processor for PostgreSQL database operations using the Unify framework.
 * This class provides methods to read from and write to PostgreSQL databases
 * using mapping configurations.
 */
public class PostgreSQLProcessor {
    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLProcessor.class);

    /**
     * Reads data from a PostgreSQL database into a table using mapping configuration.
     *
     * @param table the table to populate with data
     * @param connection the database connection
     * @param mappingConfiguration the mapping configuration
     * @throws SQLException if a database error occurs
     */
    public void readFromPostgreSQLDatabase(ITable table, 
                                         DbConnection connection, 
                                         MappingConfiguration mappingConfiguration) throws SQLException {
        logger.info("Reading data from PostgreSQL database: {}", connection.getConnectionString());
        
        if (!connection.isConnected()) {
            throw new SQLException("Database connection is not established");
        }

        JDBCTableAdapter adapter = new JDBCTableAdapter(table);
        JDBCMappingReader reader = new JDBCMappingReader();

        try {
            reader.readData(adapter, connection, mappingConfiguration.getOptions());
            logger.info("Successfully read {} rows from PostgreSQL database", table.getRowCount());
        } catch (Exception e) {
            logger.error("Error reading from PostgreSQL database: {}", e.getMessage(), e);
            throw new SQLException("Failed to read from PostgreSQL database", e);
        }
    }

    /**
     * Writes data from a table to a PostgreSQL database using mapping configuration.
     *
     * @param table the table containing data to write
     * @param connection the database connection
     * @param mappingConfiguration the mapping configuration
     * @throws SQLException if a database error occurs
     */
    public void writeToPostgreSQLDatabase(ITable table, 
                                        DbConnection connection, 
                                        MappingConfiguration mappingConfiguration) throws SQLException {
        logger.info("Writing data to PostgreSQL database: {}", connection.getConnectionString());
        
        if (!connection.isConnected()) {
            throw new SQLException("Database connection is not established");
        }

        // Create table if specified in options
        if (Boolean.TRUE.equals(mappingConfiguration.getOptions().get("createTable"))) {
            createTableIfNotExists(connection, mappingConfiguration);
        }

        JDBCTableAdapter adapter = new JDBCTableAdapter(table);
        JDBCMappingWriter writer = new JDBCMappingWriter();

        try {
            writer.writeData(adapter, connection, mappingConfiguration.getOptions());
            logger.info("Successfully wrote {} rows to PostgreSQL database", table.getRowCount());
        } catch (Exception e) {
            logger.error("Error writing to PostgreSQL database: {}", e.getMessage(), e);
            throw new SQLException("Failed to write to PostgreSQL database", e);
        }
    }

    /**
     * Creates a table in PostgreSQL if it doesn't exist, based on the mapping configuration.
     *
     * @param connection the database connection
     * @param mappingConfiguration the mapping configuration
     * @throws SQLException if a database error occurs
     */
    private void createTableIfNotExists(DbConnection connection, 
                                      MappingConfiguration mappingConfiguration) throws SQLException {
        String tableName = (String) mappingConfiguration.getOptions().get("tableName");
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new SQLException("Table name is required for table creation");
        }

        StringBuilder createTableSQL = new StringBuilder();
        createTableSQL.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");

        boolean first = true;
        for (var columnMapping : mappingConfiguration.getColumnMappings()) {
            if (!first) {
                createTableSQL.append(", ");
            }
            
            String columnName = columnMapping.getTargetColumn();
            String dataType = mapDataTypeToPostgreSQL(columnMapping.getDataType());
            
            createTableSQL.append(columnName).append(" ").append(dataType);
            
            // Add primary key constraint for id columns
            if ("id".equalsIgnoreCase(columnName)) {
                createTableSQL.append(" PRIMARY KEY");
            }
            
            first = false;
        }
        
        createTableSQL.append(")");

        logger.debug("Creating PostgreSQL table with SQL: {}", createTableSQL.toString());

        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(createTableSQL.toString());
            logger.info("Successfully created/verified PostgreSQL table: {}", tableName);
            
        } catch (SQLException e) {
            logger.error("Error creating PostgreSQL table {}: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Maps generic data types to PostgreSQL-specific data types.
     *
     * @param genericType the generic data type
     * @return the PostgreSQL-specific data type
     */
    private String mapDataTypeToPostgreSQL(String genericType) {
        if (genericType == null) {
            return "TEXT";
        }

        return switch (genericType.toLowerCase()) {
            case "int", "integer" -> "INTEGER";
            case "long", "bigint" -> "BIGINT";
            case "double", "float" -> "DOUBLE PRECISION";
            case "decimal", "numeric" -> "NUMERIC";
            case "boolean", "bool" -> "BOOLEAN";
            case "date" -> "DATE";
            case "time" -> "TIME";
            case "datetime", "timestamp" -> "TIMESTAMP";
            case "varchar", "string" -> "VARCHAR(255)";
            case "text" -> "TEXT";
            case "blob", "binary" -> "BYTEA";
            default -> "TEXT";
        };
    }

    /**
     * Executes a custom SQL query on the PostgreSQL database.
     *
     * @param connection the database connection
     * @param sql the SQL query to execute
     * @throws SQLException if a database error occurs
     */
    public void executeQuery(DbConnection connection, String sql) throws SQLException {
        logger.debug("Executing PostgreSQL query: {}", sql);
        
        if (!connection.isConnected()) {
            throw new SQLException("Database connection is not established");
        }

        try (Connection conn = connection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            logger.info("Successfully executed PostgreSQL query");
            
        } catch (SQLException e) {
            logger.error("Error executing PostgreSQL query: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Checks if a table exists in the PostgreSQL database.
     *
     * @param connection the database connection
     * @param tableName the name of the table to check
     * @return true if the table exists, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean tableExists(DbConnection connection, String tableName) throws SQLException {
        String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
        
        try (Connection conn = connection.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tableName.toLowerCase());
            var rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean(1);
            }
            
            return false;
            
        } catch (SQLException e) {
            logger.error("Error checking if PostgreSQL table exists: {}", e.getMessage(), e);
            throw e;
        }
    }
}
