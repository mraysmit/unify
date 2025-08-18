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
package dev.mars.jtable.io.files.jdbc;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader for database tables using a mapping configuration.
 * This class reads data from a database table according to a mapping configuration.
 */
public class JDBCMappingReader {
    private static final Logger logger = LoggerFactory.getLogger(JDBCMappingReader.class);
    /**
     * Reads data from a database into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     * @throws IllegalArgumentException if table or config is null, or if config has invalid settings
     * @throws SQLException if there is an error reading from the database
     */
    public void readFromDatabase(ITable table, MappingConfiguration config) throws SQLException {
        // Validate input parameters
        if (table == null) {
            String errorMsg = "Table cannot be null";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (config == null) {
            String errorMsg = "Mapping configuration cannot be null";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        String connectionString = config.getSourceLocation();
        if (connectionString == null || connectionString.trim().isEmpty()) {
            String errorMsg = "Source location (connection string) in mapping configuration cannot be null or empty";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate column mappings
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            String errorMsg = "Column mappings cannot be null or empty";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Extract database options
        String tableName = (String) config.getOption("tableName", null);
        String query = (String) config.getOption("query", null);
        String username = (String) config.getOption("username", "");
        String password = (String) config.getOption("password", "");

        if (tableName == null && query == null) {
            String errorMsg = "Either 'tableName' or 'query' must be specified in options";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Set up the table columns based on the mapping configuration
        table.setColumns(config.createColumnDefinitions());

        try (Connection connection = DriverManager.getConnection(connectionString, username, password)) {
            // Prepare the query
            String sql;
            if (query != null) {
                sql = query;
            } else {
                sql = "SELECT * FROM " + tableName;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Create a map of database column names to indices
                Map<String, Integer> dbColumnIndices = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    dbColumnIndices.put(metaData.getColumnName(i).toUpperCase(), i);
                }

                // Process all rows in the result set
                while (resultSet.next()) {
                    Map<String, String> rowValues = new HashMap<>();

                    // For each column mapping, get the value from the result set
                    for (ColumnMapping mapping : columnMappings) {
                        String targetColumnName = mapping.getTargetColumnName();
                        String value = null;

                        if (mapping.usesSourceColumnName()) {
                            String sourceColumnName = mapping.getSourceColumnName();
                            if (sourceColumnName != null && !sourceColumnName.isEmpty()) {
                                // Look up the column index in the result set
                                Integer columnIndex = dbColumnIndices.get(sourceColumnName.toUpperCase());
                                if (columnIndex != null) {
                                    value = resultSet.getString(columnIndex);
                                } else {
                                    logger.warn("Source column '{}' not found in result set", sourceColumnName);
                                }
                            }
                        } else if (mapping.usesSourceColumnIndex()) {
                            int sourceColumnIndex = mapping.getSourceColumnIndex();
                            if (sourceColumnIndex >= 0 && sourceColumnIndex < columnCount) {
                                value = resultSet.getString(sourceColumnIndex + 1);
                            } else {
                                logger.warn("Source column index {} is out of bounds (0-{})", sourceColumnIndex, 
                                        (columnCount - 1));
                            }
                        }

                        // Use default value if the value is null or empty
                        if (value == null || value.isEmpty()) {
                            value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                        }

                        rowValues.put(targetColumnName, value);
                    }

                    // Add the row to the table
                    table.addRow(rowValues);
                }
            }
        } catch (SQLException e) {
            logger.error("Error reading from database: {}", e.getMessage());
            throw e;
        }
    }
}
