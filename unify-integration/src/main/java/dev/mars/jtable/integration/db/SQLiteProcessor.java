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
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.files.jdbc.JDBCMappingWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Processor class for SQLite database operations.
 * This class separates the SQLite processing concerns from the main application logic.
 */
public class SQLiteProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteProcessor.class);

    /**
     * Writes data from a table to a SQLite database using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using JDBCConnection for database access
     * - Saving and loading mapping configurations using JSON serialization
     *
     * @param table the table to write from
     * @param connection the database connection to use
     * @param sqliteConfig the mapping configuration to use
     * @return the number of rows written to the database
     * @throws SQLException if there is an error writing to the database
     * @throws IOException if there is an error with the mapping configuration
     */
    public int writeToSQLiteDatabase(ITable table, DbConnection connection, MappingConfiguration sqliteConfig) throws SQLException, IOException {
        if (table == null) {
            throw new IllegalArgumentException("Table cannot be null");
        }
        if (connection == null) {
            throw new IllegalArgumentException("Database connection cannot be null");
        }
        if (sqliteConfig == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }

        try {
            // Ensure the connection is established
            if (!connection.isConnected() && !connection.connect()) {
                throw new SQLException("Failed to connect to SQLite database: " + connection.getConnectionString());
            }
            logger.debug("Successfully connected to SQLite database");

            // Write to SQLite database
            JDBCMappingWriter sqliteWriter = new JDBCMappingWriter();
            sqliteWriter.writeToDatabase(table, sqliteConfig);

            int rowsWritten = table.getRowCount();
            logger.info("Successfully wrote {} rows to SQLite database table: {}", rowsWritten, sqliteConfig.getOption("tableName", "person_data"));

            return rowsWritten;
        } finally {
            // We don't disconnect here because the connection was passed in from outside
            // The caller is responsible for managing the connection lifecycle
        }
    }
}
