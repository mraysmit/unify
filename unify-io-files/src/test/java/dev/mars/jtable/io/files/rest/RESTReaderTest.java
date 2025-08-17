package dev.mars.jtable.io.files.rest;

import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.RESTConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RESTReaderTest {

    private RESTReader restReader;
    private MockRESTDataSource dataSource;
    private MockRESTConnection connection;

    @BeforeEach
    void setUp() {
        restReader = new RESTReader();
        dataSource = new MockRESTDataSource();
        connection = new MockRESTConnection("https://api.example.com/data");
    }

    @Test
    void testReadDataWithJsonResponse() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("[{\"id\":\"1\",\"name\":\"Test\",\"value\":\"Sample\"}]", 200);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("responseFormat", "json");

        // Read data
        restReader.readData(dataSource, connection, options);

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
        assertEquals(3, dataSource.getColumnCount());

        // Verify column names
        assertEquals("id", dataSource.getColumnName(0));
        assertEquals("name", dataSource.getColumnName(1));
        assertEquals("value", dataSource.getColumnName(2));

        // Verify row data
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test", dataSource.getValueAt(0, "name"));
        assertEquals("Sample", dataSource.getValueAt(0, "value"));
    }

    @Test
    void testReadDataWithXmlResponse() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("<data><row><id>1</id><name>Test</name><value>Sample</value></row></data>", 200);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("responseFormat", "xml");

        // Read data
        restReader.readData(dataSource, connection, options);

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
        assertEquals(1, dataSource.getColumnCount());

        // Verify column names
        assertEquals("response", dataSource.getColumnName(0));
    }

    @Test
    void testReadDataWithCsvResponse() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("id,name,value\n1,Test,Sample", 200);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("responseFormat", "csv");

        // Read data
        restReader.readData(dataSource, connection, options);

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
        assertEquals(1, dataSource.getColumnCount());

        // Verify column names
        assertEquals("response", dataSource.getColumnName(0));
    }

    @Test
    void testReadDataWithInvalidResponseFormat() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("Some data", 200);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("responseFormat", "invalid");

        // Read data should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            restReader.readData(dataSource, connection, options);
        });

        // Verify exception message
        assertEquals("Unsupported response format: invalid", exception.getMessage());
    }

    @Test
    void testReadDataWithHttpError() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("Not Found", 404);

        // Create options
        Map<String, Object> options = new HashMap<>();
        options.put("method", "GET");
        options.put("responseFormat", "json");

        // Read data
        restReader.readData(dataSource, connection, options);

        // Verify that no data was read due to HTTP error
        assertEquals(0, dataSource.getRowCount());
    }

    @Test
    void testReadDataWithNullOptions() {
        // Set up the connection
        connection.setConnected(true);
        connection.setMockResponse("[{\"id\":\"1\",\"name\":\"Test\",\"value\":\"Sample\"}]", 200);

        // Read data with null options
        restReader.readData(dataSource, connection, null);

        // Verify that data was read with default options
        assertEquals(1, dataSource.getRowCount());
    }

    @Test
    void testReadDataWithInvalidConnection() {
        // Create a non-RESTConnection
        MockInvalidConnection invalidConnection = new MockInvalidConnection();

        // Attempt to read data with invalid connection
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            restReader.readData(dataSource, invalidConnection, null);
        });

        // Verify exception message
        assertEquals("Connection must be a RESTConnection", exception.getMessage());
    }

    @Test
    void testReadDataWithDisconnectedConnection() {
        // Set up the connection as disconnected
        connection.setConnected(false);
        connection.setMockResponse("[{\"id\":\"1\",\"name\":\"Test\",\"value\":\"Sample\"}]", 200);

        // Read data
        restReader.readData(dataSource, connection, null);

        // Verify that connection was established
        assertTrue(connection.isConnected());

        // Verify that data was read
        assertEquals(1, dataSource.getRowCount());
    }

    // Mock classes for testing
    private static class MockRESTDataSource implements IDataSource {
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

    private static class MockRESTConnection extends RESTConnection {
        private boolean connected = false;
        private String mockResponse;
        private int mockResponseCode;
        private MockHttpURLConnection mockHttpConnection;

        public MockRESTConnection(String endpoint) {
            super(endpoint);
            mockHttpConnection = new MockHttpURLConnection();
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void setMockResponse(String response, int responseCode) {
            this.mockResponse = response;
            this.mockResponseCode = responseCode;
            mockHttpConnection.setResponseCode(responseCode);
            mockHttpConnection.setResponseBody(response);
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
        public HttpURLConnection createConnection(String method) {
            mockHttpConnection.setRequestMethod(method);
            return mockHttpConnection;
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

    private static class MockHttpURLConnection extends HttpURLConnection {
        private int responseCode = 200;
        private String responseBody = "{}";
        private String requestMethod = "GET";

        public MockHttpURLConnection() {
            super(null);
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public void setRequestMethod(String method) {
            this.requestMethod = method;
        }

        @Override
        public String getRequestMethod() {
            return requestMethod;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(responseBody.getBytes());
        }

        @Override
        public void disconnect() {
            // Do nothing
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() throws IOException {
            // Do nothing
        }
    }
}
