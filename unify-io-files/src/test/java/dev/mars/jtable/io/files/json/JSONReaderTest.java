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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IJSONDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JSONReaderTest {

    private JSONReader jsonReader;
    private MockJSONDataSource dataSource;
    private MockFileConnection fileConnection;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jsonReader = new JSONReader();
        dataSource = new MockJSONDataSource();
        fileConnection = new MockFileConnection();
    }

    @Test
    void testReadFromJSON() throws Exception {
        // Create a test JSON file
        File testFile = tempDir.resolve("test.json").toFile();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootArray = mapper.createArrayNode();

        // Add a test object to the array
        ObjectNode testObject = mapper.createObjectNode();
        testObject.put("id", 1);
        testObject.put("name", "Test Name");
        testObject.put("active", true);
        testObject.put("score", 95.5);
        rootArray.add(testObject);

        // Write the array to the file
        mapper.writeValue(testFile, rootArray);

        // Test reading from the file
        jsonReader.readFromJSON(dataSource, testFile.getAbsolutePath(), null);

        // Verify the data was read correctly
        assertEquals(1, dataSource.getRowCount());
        assertEquals(4, dataSource.getColumnCount());

        // Verify column types
        Map<String, String> columns = dataSource.getColumns();
        assertEquals("int", columns.get("id"));
        assertEquals("string", columns.get("name"));
        assertEquals("boolean", columns.get("active"));
        assertEquals("double", columns.get("score"));

        // Verify row values
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test Name", dataSource.getValueAt(0, "name"));
        assertEquals("true", dataSource.getValueAt(0, "active"));
        assertEquals("95.5", dataSource.getValueAt(0, "score"));
    }

    @Test
    void testReadData() throws Exception {
        // Create a test JSON file
        File testFile = tempDir.resolve("test_data.json").toFile();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rootArray = mapper.createArrayNode();

        // Add a test object to the array
        ObjectNode testObject = mapper.createObjectNode();
        testObject.put("id", 1);
        testObject.put("name", "Test Name");
        rootArray.add(testObject);

        // Write the array to the file
        mapper.writeValue(testFile, rootArray);

        // Set up the file connection
        fileConnection.setRawConnection(testFile.toPath());
        fileConnection.setConnected(true);

        // Test reading from the file using readData
        Map<String, Object> options = new HashMap<>();
        jsonReader.readData(dataSource, fileConnection, options);

        // Verify the data was read correctly
        assertEquals(1, dataSource.getRowCount());
        assertEquals(2, dataSource.getColumnCount());

        // Verify row values
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test Name", dataSource.getValueAt(0, "name"));
    }

    @Test
    void testReadDataWithRootElement() throws Exception {
        // Create a test JSON file with a root element
        File testFile = tempDir.resolve("test_root.json").toFile();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode dataArray = mapper.createArrayNode();

        // Add a test object to the array
        ObjectNode testObject = mapper.createObjectNode();
        testObject.put("id", 1);
        testObject.put("name", "Test Name");
        dataArray.add(testObject);

        // Add the array to the root element
        rootNode.set("data", dataArray);

        // Write the object to the file
        mapper.writeValue(testFile, rootNode);

        // Set up the file connection
        fileConnection.setRawConnection(testFile.toPath());
        fileConnection.setConnected(true);

        // Test reading from the file using readData with root element
        Map<String, Object> options = new HashMap<>();
        options.put("rootElement", "data");
        jsonReader.readData(dataSource, fileConnection, options);

        // Verify the data was read correctly
        assertEquals(1, dataSource.getRowCount());
        assertEquals(2, dataSource.getColumnCount());

        // Verify row values
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test Name", dataSource.getValueAt(0, "name"));
    }

    // Mock classes for testing
    private static class MockJSONDataSource implements IJSONDataSource {
        private LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        private Map<Integer, Map<String, String>> rows = new HashMap<>();
        private int rowCount = 0;

        @Override
        public void setColumns(LinkedHashMap<String, String> columns) {
            this.columns = columns;
        }

        public LinkedHashMap<String, String> getColumns() {
            return columns;
        }

        @Override
        public void addRow(Map<String, String> row) {
            rows.put(rowCount++, row);
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return (String) columns.keySet().toArray()[columnIndex];
        }

        @Override
        public String getValueAt(int rowIndex, String columnName) {
            return rows.get(rowIndex).get(columnName);
        }

        @Override
        public String inferType(String value) {
            if (value == null || value.isEmpty()) {
                return "string";
            }

            if (value.matches("-?\\d+")) {
                return "int";
            } else if (value.matches("-?\\d*\\.\\d+")) {
                return "double";
            } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return "boolean";
            } else {
                return "string";
            }
        }
    }

    private static class MockFileConnection extends FileConnection {
        private boolean connected = false;
        private Path rawConnection;
        private Map<String, Object> properties = new HashMap<>();

        public MockFileConnection() {
            super("", "json");
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void setRawConnection(Path rawConnection) {
            this.rawConnection = rawConnection;
        }

        @Override
        public boolean connect() {
            connected = true;
            return true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public String getConnectionType() {
            return "json";
        }

        @Override
        public Object getRawConnection() {
            return rawConnection;
        }

        @Override
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getLocation() {
            return rawConnection != null ? rawConnection.toString() : "";
        }
    }
}
