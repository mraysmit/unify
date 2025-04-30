package dev.mars.jtable.io.rest;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.RESTTableAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import dev.mars.jtable.core.model.ITable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for RESTTableAdapter.
 * These tests are disabled by default as they require an actual REST API endpoint.
 * To run these tests, you need to set up a mock REST API server or use a public API.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class RESTTableAdapterTest {
    private ITable table;
    private RESTTableAdapter adapter;
    private final String mockEndpoint = "https://jsonplaceholder.typicode.com/posts";

    @BeforeEach
    void setUp() {
        // Create a new Table instance for each test
        table = new Table();
        
        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("title", "string");
        columns.put("body", "string");
        columns.put("userId", "int");
        table.setColumns(columns);
        
        // Create the adapter with the table
        adapter = new RESTTableAdapter(table);
    }

    @Test
    void testConstructors() {
        // Test the constructors
        RESTTableAdapter adapter1 = new RESTTableAdapter(table);
        assertEquals(table, adapter1.getTable());
        
        RESTTableAdapter adapter2 = new RESTTableAdapter(mockEndpoint);
        assertNotNull(adapter2.getTable());
        
        RESTTableAdapter adapter3 = new RESTTableAdapter(mockEndpoint, "token123");
        assertNotNull(adapter3.getTable());
    }

    @Test
    void testWithMethods() {
        // Test the with methods
        RESTTableAdapter adapter1 = new RESTTableAdapter(mockEndpoint)
                .withMethod("GET")
                .withResponseFormat("json");
        
        assertNotNull(adapter1);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_NETWORK_TESTS", matches = "true")
    void testReadTable() {
        // Create a new adapter with the mock endpoint
        RESTTableAdapter restAdapter = new RESTTableAdapter(mockEndpoint);
        
        // Read the table from the REST API
        // Note: This will make an actual HTTP request to the mock endpoint
        // If you're running this test, make sure the endpoint is available
        try {
            restAdapter.readTable();
            
            // Verify that data was read
            assertTrue(restAdapter.getTable().getRowCount() > 0);
            assertTrue(restAdapter.getTable().getColumnCount() > 0);
        } catch (Exception e) {
            // If the test fails due to network issues, just log it
            System.err.println("Test failed due to network issues: " + e.getMessage());
            // Don't fail the test in CI environments
            if (System.getenv("CI") == null) {
                fail("Test failed: " + e.getMessage());
            }
        }
    }

    @Test
    void testImplementsInterfaces() {
        // Test that the adapter implements the correct interfaces
        assertTrue(adapter instanceof dev.mars.jtable.io.adapter.ITableAdapter);
    }

    @Test
    void testGetTable() {
        // Test that getTable returns the correct table instance
        assertSame(table, adapter.getTable());
    }
}