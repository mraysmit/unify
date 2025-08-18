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
package dev.mars.jtable.io.files.nosql;

import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.NoSQLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSQLReaderTest {

    private NoSQLReader noSQLReader;
    private MockNoSQLDataSource dataSource;
    private MockNoSQLConnection connection;

    @BeforeEach
    void setUp() {
        noSQLReader = new NoSQLReader();
        dataSource = new MockNoSQLDataSource();
        connection = new MockNoSQLConnection("mongodb://localhost:27017", "testdb", "testcollection");
    }

    @Test
    void testReadData() {
        // Set up the connection
        connection.setConnected(true);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("query", "{ \"status\": \"active\" }");
        options.put("limit", 10);

        // Read data
        noSQLReader.readData(dataSource, connection, options);

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
        assertEquals(3, dataSource.getColumnCount());

        // Verify column names
        assertEquals("id", dataSource.getColumnName(0));
        assertEquals("name", dataSource.getColumnName(1));
        assertEquals("value", dataSource.getColumnName(2));

        // Verify row data
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Sample", dataSource.getValueAt(0, "name"));
        assertEquals("This is a sample row from NoSQL database", dataSource.getValueAt(0, "value"));
    }

    @Test
    void testReadDataWithNullOptions() {
        // Set up the connection
        connection.setConnected(true);

        // Read data with null options
        noSQLReader.readData(dataSource, connection, null);

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
        assertEquals(3, dataSource.getColumnCount());
    }

    @Test
    void testReadDataWithInvalidConnection() {
        // Create a non-NoSQLConnection
        MockInvalidConnection invalidConnection = new MockInvalidConnection();

        // Attempt to read data with invalid connection
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            noSQLReader.readData(dataSource, invalidConnection, null);
        });

        // Verify exception message
        assertEquals("Connection must be a NoSQLConnection", exception.getMessage());
    }

    @Test
    void testReadDataWithDisconnectedConnection() {
        // Set up the connection as disconnected
        connection.setConnected(false);

        // Read data
        noSQLReader.readData(dataSource, connection, null);

        // Verify that connection was established
        assertTrue(connection.isConnected());

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
    }

    // Mock classes for testing
    private static class MockNoSQLDataSource implements IDataSource {
        private LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        private Map<Integer, Map<String, String>> rows = new HashMap<>();
        private int rowCount = 0;

        @Override
        public void setColumns(LinkedHashMap<String, String> columns) {
            this.columns = columns;
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

    private static class MockNoSQLConnection extends NoSQLConnection {
        private boolean connected = false;

        public MockNoSQLConnection(String connectionString, String database, String collection) {
            super(connectionString, database, collection);
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
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
    }

    private static class MockInvalidConnection implements dev.mars.jtable.io.common.datasource.IDataSourceConnection {
        @Override
        public boolean connect() {
            return true;
        }

        @Override
        public void disconnect() {
            // Do nothing
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public String getConnectionType() {
            return "invalid";
        }

        @Override
        public Object getRawConnection() {
            return null;
        }

        @Override
        public Map<String, Object> getProperties() {
            return new HashMap<>();
        }
    }
}
