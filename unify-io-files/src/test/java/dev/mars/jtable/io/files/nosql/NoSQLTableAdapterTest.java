package dev.mars.jtable.io.files.nosql;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.NoSQLConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NoSQLTableAdapterTest {

    private NoSQLTableAdapter adapter;
    private MockNoSQLReader mockReader;

    @BeforeEach
    void setUp() {
        // Create a mock NoSQLReader
        mockReader = new MockNoSQLReader();

        // Create the adapter with test parameters
        adapter = new NoSQLTableAdapter("mongodb://localhost:27017", "testdb", "testcollection");
    }

    @Test
    void testConstructorWithTable() {
        // Create a mock table
        ITable mockTable = new Table();

        // Create adapter with the mock table
        NoSQLTableAdapter tableAdapter = new NoSQLTableAdapter(mockTable);

        // Verify the table was set correctly
        assertNotNull(tableAdapter.getTable());
        assertEquals(mockTable, tableAdapter.getTable());
    }

    @Test
    void testConstructorWithConnectionParameters() {
        // Verify the adapter was created correctly
        assertNotNull(adapter);
        assertNotNull(adapter.getTable());
    }

    @Test
    void testConstructorWithAuthentication() {
        // Create adapter with authentication
        NoSQLTableAdapter authAdapter = new NoSQLTableAdapter(
                "mongodb://localhost:27017", 
                "testdb", 
                "testcollection", 
                "username", 
                "password");

        // Verify the adapter was created correctly
        assertNotNull(authAdapter);
        assertNotNull(authAdapter.getTable());
    }

    @Test
    void testWithQuery() {
        // Set a query
        NoSQLTableAdapter queryAdapter = adapter.withQuery("{ \"status\": \"active\" }");

        // Verify the adapter was returned (for method chaining)
        assertSame(adapter, queryAdapter);
    }

    @Test
    void testWithLimit() {
        // Set a limit
        NoSQLTableAdapter limitAdapter = adapter.withLimit(10);

        // Verify the adapter was returned (for method chaining)
        assertSame(adapter, limitAdapter);
    }

    @Test
    void testReadTable() {
        // Create a new adapter for this test
        NoSQLTableAdapter testAdapter = new NoSQLTableAdapter(
                "mongodb://localhost:27017", 
                "testdb", 
                "testcollection");

        // Use reflection to replace the NoSQLReader with our mock
        try {
            java.lang.reflect.Field readerField = NoSQLTableAdapter.class.getDeclaredField("reader");
            readerField.setAccessible(true);
            readerField.set(testAdapter, mockReader);
        } catch (Exception e) {
            // If reflection fails, we'll just use the original reader
            System.out.println("Reflection failed, using original reader: " + e.getMessage());
        }

        // Read the table
        ITable result = testAdapter.readTable();

        // Verify the result
        assertNotNull(result);
    }

    @Test
    void testReadTableWithAuthentication() {
        // Create a new adapter with authentication for this test
        NoSQLTableAdapter authAdapter = new NoSQLTableAdapter(
                "mongodb://localhost:27017", 
                "testdb", 
                "testcollection", 
                "username", 
                "password");

        // Use reflection to replace the NoSQLReader with our mock
        try {
            java.lang.reflect.Field readerField = NoSQLTableAdapter.class.getDeclaredField("reader");
            readerField.setAccessible(true);
            readerField.set(authAdapter, mockReader);
        } catch (Exception e) {
            // If reflection fails, we'll just use the original reader
            System.out.println("Reflection failed, using original reader: " + e.getMessage());
        }

        // Read the table
        ITable result = authAdapter.readTable();

        // Verify the result
        assertNotNull(result);
    }

    // Mock classes for testing
    private static class MockNoSQLReader extends NoSQLReader {
        @Override
        public void readData(dev.mars.jtable.io.common.datasource.IDataSource dataSource, 
                            IDataSourceConnection connection, 
                            Map<String, Object> options) {
            // Just add some sample data to the dataSource
            java.util.LinkedHashMap<String, String> columns = new java.util.LinkedHashMap<>();
            columns.put("id", "string");
            columns.put("name", "string");
            columns.put("value", "string");
            dataSource.setColumns(columns);

            Map<String, String> row = new HashMap<>();
            row.put("id", "1");
            row.put("name", "Sample");
            row.put("value", "This is a sample row from NoSQL database");
            dataSource.addRow(row);
        }
    }
}
