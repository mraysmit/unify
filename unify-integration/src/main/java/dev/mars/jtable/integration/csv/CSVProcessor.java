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
package dev.mars.jtable.integration.csv;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.files.csv.CSVMappingReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Processor class for CSV file operations.
 * This class separates the CSV processing concerns from the main application logic.
 */
public class CSVProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CSVProcessor.class);

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     * This method demonstrates advanced usage of MappingConfiguration:
     * - Using FileConnection from DataSourceConnectionFactory
     * - Using dependency injection for MappingConfiguration
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @param csvConfig the mapping configuration to use
     * @return the number of rows read from the CSV file
     * @throws IOException if there is an error reading the file
     */
    public int readFromCSV(ITable table, String csvFilePath, MappingConfiguration csvConfig) throws IOException {
        logger.info("Creating FileConnection for CSV file: {}", csvFilePath);

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = null;
        try {
            connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new IOException("Failed to connect to CSV file: " + csvFilePath);
            }
            logger.debug("Successfully connected to CSV file");

            // Update the source location to the current file
            csvConfig.setSourceLocation(connection.getLocation());

            // Read from CSV
            CSVMappingReader csvReader = new CSVMappingReader();
            csvReader.readFromCSV(table, csvConfig);

            int rowsRead = table.getRowCount();
            logger.info("Successfully read data from CSV file with {} rows and {} columns",
                    rowsRead, table.getColumnCount());

            return rowsRead;
        } finally {
            // Ensure connection is closed even if an exception occurs
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from CSV file");
            }
        }
    }

    /**
     * Gets the location of a CSV file.
     * This method connects to the file and gets its location, then disconnects.
     *
     * @param csvFilePath the path to the CSV file
     * @return the location of the CSV file
     * @throws IOException if there is an error connecting to the file
     */
    public String getCSVFileLocation(String csvFilePath) throws IOException {
        FileConnection connection = null;
        try {
            connection = (FileConnection) DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new IOException("Failed to connect to CSV file: " + csvFilePath);
            }

            return connection.getLocation();
        } finally {
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
            }
        }
    }
}
