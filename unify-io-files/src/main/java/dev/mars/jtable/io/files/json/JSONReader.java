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
package dev.mars.jtable.io.files.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IJSONDataSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of the IJSONReader interface for reading data from JSON files.
 *
 * This class provides two methods:
 * - readData: The main interface method that adapts generic IDataReader calls to JSON-specific operations
 * - readFromJSON: The JSON-specific implementation method that handles the actual JSON parsing
 */
public class JSONReader implements IJSONReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a JSON-specific dataSource
        IJSONDataSource jsonDataSource;
        if (dataSource instanceof IJSONDataSource) {
            jsonDataSource = (IJSONDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IJSONDataSource");
        }

        // Ensure we have a file connection
        if (!(connection instanceof FileConnection)) {
            throw new IllegalArgumentException("Connection must be a FileConnection");
        }
        FileConnection fileConnection = (FileConnection) connection;

        // Extract options
        String rootElement = options != null && options.containsKey("rootElement") ? (String) options.get("rootElement") : null;

        // Connect if not already connected
        if (!fileConnection.isConnected()) {
            fileConnection.connect();
        }

        // Get the file path from the connection
        String source;
        if (fileConnection.isRemote()) {
            source = fileConnection.getLocation();
        } else {
            Object rawConnection = fileConnection.getRawConnection();
            if (rawConnection instanceof Path) {
                source = ((Path) rawConnection).toString();
            } else {
                throw new IllegalArgumentException("Cannot determine file path from connection");
            }
        }

        // Call the JSON-specific method
        readFromJSON(jsonDataSource, source, rootElement);
    }

    /**
     * Reads data from a JSON file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param rootElement the name of the root element in the JSON file (optional)
     */
    @Override
    public void readFromJSON(IJSONDataSource dataSource, String fileName, String rootElement) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(new File(fileName));

            // If rootElement is specified, navigate to that element
            if (rootElement != null && !rootElement.isEmpty()) {
                rootNode = rootNode.path(rootElement);
                if (rootNode.isMissingNode()) {
                    throw new IOException("Root element '" + rootElement + "' not found in JSON file");
                }
            }

            // Check if the root node is an array
            if (rootNode.isArray()) {
                processArrayNode((ArrayNode) rootNode, dataSource);
            } else {
                throw new IOException("JSON root must be an array of objects");
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing JSON data: " + e.getMessage());
        }
    }

    /**
     * Processes an array node from the JSON file.
     *
     * @param arrayNode the array node to process
     * @param dataSource the data source to read into
     * @throws IOException if there is an error processing the array node
     */
    private void processArrayNode(ArrayNode arrayNode, IJSONDataSource dataSource) throws IOException {
        if (arrayNode.size() == 0) {
            return; // Empty array, nothing to do
        }

        // Get the first object to infer column names and types
        JsonNode firstObject = arrayNode.get(0);
        if (!firstObject.isObject()) {
            throw new IOException("JSON array must contain objects");
        }

        // Create columns based on the first object
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = firstObject.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            String fieldType = inferTypeFromJsonNode(fieldValue);
            columns.put(fieldName, fieldType);
        }
        dataSource.setColumns(columns);

        // Process all objects in the array
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode obj = arrayNode.get(i);
            if (!obj.isObject()) {
                continue; // Skip non-objects
            }

            Map<String, String> row = new HashMap<>();
            fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode fieldValue = field.getValue();
                String stringValue = getStringValueFromJsonNode(fieldValue);
                row.put(fieldName, stringValue);
            }
            dataSource.addRow(row);
        }
    }

    /**
     * Infers the data type from a JSON node.
     *
     * @param node the JSON node to infer the type from
     * @return the inferred data type
     */
    private String inferTypeFromJsonNode(JsonNode node) {
        if (node.isInt() || node.isLong()) {
            return "int";
        } else if (node.isDouble() || node.isFloat()) {
            return "double";
        } else if (node.isBoolean()) {
            return "boolean";
        } else {
            return "string";
        }
    }

    /**
     * Gets a string value from a JSON node.
     *
     * @param node the JSON node to get the value from
     * @return the string value
     */
    private String getStringValueFromJsonNode(JsonNode node) {
        if (node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else {
            return node.toString();
        }
    }
}
