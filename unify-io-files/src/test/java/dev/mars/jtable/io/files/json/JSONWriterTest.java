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

class JSONWriterTest {

    private JSONWriter jsonWriter;
    private MockJSONDataSource dataSource;
    private MockFileConnection fileConnection;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        jsonWriter = new JSONWriter();
        dataSource = new MockJSONDataSource();
        fileConnection = new MockFileConnection();
        
        // Set up test data
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("name", "string");
        columns.put("active", "boolean");
        columns.put("score", "double");
        dataSource.setColumns(columns);
        
        Map<String, String> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "Test Name");
        row1.put("active", "true");
        row1.put("score", "95.5");
        dataSource.addRow(row1);
        
        Map<String, String> row2 = new HashMap<>();
        row2.put("id", "2");
        row2.put("name", "Another Name");
        row2.put("active", "false");
        row2.put("score", "82.3");
        dataSource.addRow(row2);
    }

    @Test
    void testWriteToJSON() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_write.json").toFile();
        
        // Write to the file
        jsonWriter.writeToJSON(dataSource, testFile.getAbsolutePath(), false);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(testFile);
        
        // Verify it's an array
        assertTrue(rootNode.isArray());
        assertEquals(2, rootNode.size());
        
        // Verify first row
        JsonNode row1 = rootNode.get(0);
        assertEquals(1, row1.get("id").asInt());
        assertEquals("Test Name", row1.get("name").asText());
        assertTrue(row1.get("active").asBoolean());
        assertEquals(95.5, row1.get("score").asDouble(), 0.001);
        
        // Verify second row
        JsonNode row2 = rootNode.get(1);
        assertEquals(2, row2.get("id").asInt());
        assertEquals("Another Name", row2.get("name").asText());
        assertFalse(row2.get("active").asBoolean());
        assertEquals(82.3, row2.get("score").asDouble(), 0.001);
    }

    @Test
    void testWriteToJSONWithPrettyPrint() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_pretty.json").toFile();
        
        // Write to the file with pretty print
        jsonWriter.writeToJSON(dataSource, testFile.getAbsolutePath(), true);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(testFile);
        
        // Verify it's an array
        assertTrue(rootNode.isArray());
        assertEquals(2, rootNode.size());
        
        // Verify first row
        JsonNode row1 = rootNode.get(0);
        assertEquals(1, row1.get("id").asInt());
        assertEquals("Test Name", row1.get("name").asText());
        assertTrue(row1.get("active").asBoolean());
        assertEquals(95.5, row1.get("score").asDouble(), 0.001);
    }

    @Test
    void testWriteData() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_write_data.json").toFile();
        
        // Set up the file connection
        fileConnection.setRawConnection(testFile.toPath());
        fileConnection.setConnected(true);
        
        // Write to the file using writeData
        Map<String, Object> options = new HashMap<>();
        options.put("prettyPrint", true);
        jsonWriter.writeData(dataSource, fileConnection, options);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(testFile);
        
        // Verify it's an array
        assertTrue(rootNode.isArray());
        assertEquals(2, rootNode.size());
        
        // Verify first row
        JsonNode row1 = rootNode.get(0);
        assertEquals(1, row1.get("id").asInt());
        assertEquals("Test Name", row1.get("name").asText());
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