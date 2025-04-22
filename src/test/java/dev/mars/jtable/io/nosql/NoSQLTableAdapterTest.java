package dev.mars.jtable.io.nosql;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.NoSQLTableAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for NoSQLTableAdapter.
 * These tests are disabled by default as they require an actual NoSQL database.
 * To run these tests, you need to set up a mock NoSQL database or use a real one.
 */
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class NoSQLTableAdapterTest {

    private Table table;
    private NoSQLTableAdapter adapter;
    private final String mockConnectionString = "mongodb://localhost:27017";
    private final String mockDatabase = "testdb";
    private final String mockCollection = "testcollection";

    @BeforeEach
    void setUp() {
        // Create a new Table instance for each test
        table = new Table();
        
        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("_id", "string");
        columns.put("name", "string");
        columns.put("age", "int");
        columns.put("email", "string");
        table.setColumns(columns);
        
        // Create the adapter with the table
        adapter = new NoSQLTableAdapter(table);
    }

    @Test
    void testConstructors() {
        // Test the constructors
        NoSQLTableAdapter adapter1 = new NoSQLTableAdapter(table);
        assertEquals(table, adapter1.getTable());
        
        NoSQLTableAdapter adapter2 = new NoSQLTableAdapter(mockConnectionString, mockDatabase, mockCollection);
        assertNotNull(adapter2.getTable());
        
        NoSQLTableAdapter adapter3 = new NoSQLTableAdapter(mockConnectionString, mockDatabase, mockCollection, "username", "password");
        assertNotNull(adapter3.getTable());
    }

    @Test
    void testWithMethods() {
        // Test the with methods
        NoSQLTableAdapter adapter1 = new NoSQLTableAdapter(mockConnectionString, mockDatabase, mockCollection)
                .withQuery("{ \"age\": { \"$gt\": 25 } }")
                .withLimit(10);
        
        assertNotNull(adapter1);
    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_DATABASE_TESTS", matches = "true")
    void testReadTable() {
        // Create a new adapter with the mock connection details
        NoSQLTableAdapter nosqlAdapter = new NoSQLTableAdapter(mockConnectionString, mockDatabase, mockCollection);
        
        // Read the table from the NoSQL database
        // Note: This will attempt to connect to a real MongoDB instance
        // If you're running this test, make sure the database is available
        try {
            nosqlAdapter.readTable();
            
            // Verify that data was read
            // This might fail if the database is empty or doesn't exist
            // So we're just checking that the method doesn't throw an exception
            assertNotNull(nosqlAdapter.getTable());
        } catch (Exception e) {
            // If the test fails due to database issues, just log it
            System.err.println("Test failed due to database issues: " + e.getMessage());
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