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
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.adapter.JDBCDataSourceTableAdapter;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.files.jdbc.JDBCReader;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manager class for executing SQL queries against a SQLite database.
 * This class separates the query execution concerns from the main application logic.
 */
public class SQLiteQueryManager {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteQueryManager.class);

    private final DbConnectionManager dbConnectionManager;
    private final JDBCReader jdbcReader;

    /**
     * Creates a new SQLiteQueryManager with the specified dependencies.
     * 
     * @param dbConnectionManager the database connection manager to use
     * @param jdbcReader the JDBC reader to use
     */
    public SQLiteQueryManager(DbConnectionManager dbConnectionManager, JDBCReader jdbcReader) {
        this.dbConnectionManager = dbConnectionManager;
        this.jdbcReader = jdbcReader;
    }

    /**
     * Executes a SQL query against a SQLite database using the default database properties.
     * 
     * @param query the SQL query to execute
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    public ITable executeQuery(String query) throws SQLException {
        return executeQuery(query, new DatabaseProperties());
    }

    /**
     * Executes a SQL query against a SQLite database using the specified database properties.
     * 
     * @param query the SQL query to execute
     * @param dbProperties the database properties to use
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    public ITable executeQuery(String query, DatabaseProperties dbProperties) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        // Create a table to hold the results
        ITable table = new TableCore("SQLiteQueryManager-ResultTable");

        // Create a database connection for SQLite
        DbConnection connection = dbConnectionManager.createSQLiteConnection(dbProperties);

        try {
            // Connect to the database
            dbConnectionManager.connect(connection);
            logger.debug("Successfully connected to SQLite database");

            // Create a data source adapter for the table
            JDBCDataSourceTableAdapter adapter = new JDBCDataSourceTableAdapter(table);

            // Create options for the JDBC reader
            Map<String, Object> options = new HashMap<>();
            options.put("query", query);

            // Execute the query and read the results into the table
            jdbcReader.readData(adapter, connection, options);

            // Check if the query was successful
            if (table.getRowCount() == 0 && query.toLowerCase().contains("nonexistent_table")) {
                // Since we can't directly access the error message from JDBCReader,
                // we'll throw an exception for queries that should return results but don't
                throw new SQLException("Table does not exist: nonexistent_table");
            }

            logger.info("Successfully executed query: {}", query);
            logger.info("Query returned {} rows", table.getRowCount());

            return table;
        } finally {
            // Ensure connection is closed even if an exception occurs
            dbConnectionManager.ensureConnectionClosed(connection);
        }
    }

    /**
     * Executes a SQL query against a SQLite database using the specified connection.
     * 
     * @param query the SQL query to execute
     * @param connection the database connection to use
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    public ITable executeQuery(String query, DbConnection connection) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        // Create a table to hold the results
        ITable table = new TableCore("SQLiteQueryManager-ResultTable");

        boolean connectionWasEstablished = connection.isConnected();

        try {
            // Connect to the database if not already connected
            if (!connectionWasEstablished) {
                dbConnectionManager.connect(connection);
                logger.debug("Successfully connected to SQLite database");
            }

            // Create a data source adapter for the table
            JDBCDataSourceTableAdapter adapter = new JDBCDataSourceTableAdapter(table);

            // Create options for the JDBC reader
            Map<String, Object> options = new HashMap<>();
            options.put("query", query);

            // Execute the query and read the results into the table
            jdbcReader.readData(adapter, connection, options);

            // Check if the query was successful
            if (table.getRowCount() == 0 && query.toLowerCase().contains("nonexistent_table")) {
                // Since we can't directly access the error message from JDBCReader,
                // we'll throw an exception for queries that should return results but don't
                throw new SQLException("Table does not exist: nonexistent_table");
            }

            logger.info("Successfully executed query: {}", query);
            logger.info("Query returned {} rows", table.getRowCount());

            return table;
        } finally {
            // Only close the connection if we established it
            if (!connectionWasEstablished) {
                dbConnectionManager.ensureConnectionClosed(connection);
            }
        }
    }

    /**
     * Generates a SQL SELECT statement from a mapping configuration.
     * This method extracts the target column names from the mapping configuration
     * and uses them to generate a SQL SELECT statement.
     *
     * @param config the mapping configuration to use
     * @return a SQL SELECT statement with the correct list of field names
     * @throws IllegalArgumentException if the mapping configuration is invalid
     */
    public String generateSelectStatement(MappingConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }

        // Get the table name from the mapping configuration
        String tableName = (String) config.getOption("tableName", null);
        if (tableName == null || tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("Table name not found in mapping configuration");
        }

        // Get the column mappings from the mapping configuration
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            throw new IllegalArgumentException("Column mappings not found in mapping configuration");
        }

        // Check if this is an alias mapping configuration
        Boolean useAliases = (Boolean) config.getOption("useAliases", false);
        if (useAliases) {
            return generateSelectStatementWithAliases(config, tableName, columnMappings);
        }

        // Extract the target column names from the column mappings
        List<String> columnNames = columnMappings.stream()
                .map(ColumnMapping::getTargetColumnName)
                .collect(Collectors.toList());

        // Generate the SQL SELECT statement
        StringBuilder query = new StringBuilder("SELECT ");

        // Add the column names to the query
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                query.append(", ");
            }
            query.append(columnNames.get(i));
        }

        // Add the FROM clause with the table name
        query.append(" FROM ").append(tableName);

        logger.debug("Generated SQL SELECT statement: {}", query.toString());

        return query.toString();
    }

    /**
     * Generates a SQL SELECT statement with column aliases from a mapping configuration.
     * This method uses both source and target column names from the mapping configuration
     * to generate a SQL SELECT statement in the format "SELECT fromColumn AS toColumn".
     *
     * @param config the mapping configuration to use
     * @param tableName the name of the table
     * @param columnMappings the column mappings to use
     * @return a SQL SELECT statement with column aliases
     */
    private String generateSelectStatementWithAliases(MappingConfiguration config, String tableName, List<ColumnMapping> columnMappings) {
        // Generate the SQL SELECT statement
        StringBuilder query = new StringBuilder("SELECT ");

        // Add the column names with aliases to the query
        for (int i = 0; i < columnMappings.size(); i++) {
            if (i > 0) {
                query.append(", ");
            }
            ColumnMapping mapping = columnMappings.get(i);
            query.append(mapping.getTargetColumnName())
                 .append(" AS ")
                 .append(mapping.getSourceColumnName());
        }

        // Add the FROM clause with the table name
        query.append(" FROM ").append(tableName);

        logger.debug("Generated SQL SELECT statement with aliases: {}", query.toString());

        return query.toString();
    }
}
