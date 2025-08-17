package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.model.ITable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for OptimizedTableCore.
 * This class verifies that OptimizedTableCore works correctly and demonstrates
 * its performance benefits compared to the original TableCore.
 */
public class OptimizedTableCoreTest {

    private OptimizedTableCore optimizedTable;
    private TableCore standardTable;

    @BeforeEach
    void setUp() {
        // Create tables with the same structure
        optimizedTable = new OptimizedTableCore();
        standardTable = new TableCore();

        // Define columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");

        // Set columns for both tables
        optimizedTable.setColumns(columns);
        standardTable.setColumns(columns);
    }

    /**
     * Test that OptimizedTableCore correctly adds and retrieves rows.
     */
    @Test
    void testAddAndRetrieveRows() {
        // Create a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Salary", "50000.50");
        row.put("IsActive", "true");

        // Add the row to the optimized table
        optimizedTable.addRow(row);

        // Verify the row was added correctly
        assertEquals(1, optimizedTable.getRowCount());
        assertEquals("Alice", optimizedTable.getValueAt(0, "Name"));
        assertEquals("30", optimizedTable.getValueAt(0, "Age"));
        assertEquals("50000.50", optimizedTable.getValueAt(0, "Salary"));
        assertEquals("true", optimizedTable.getValueAt(0, "IsActive"));
    }

    /**
     * Test that OptimizedTableCore correctly handles double values.
     */
    @Test
    void testDoubleHandling() {
        // Create a row with a double value
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Salary", "50000.50");
        row.put("IsActive", "true");

        // Add the row to the optimized table
        optimizedTable.addRow(row);

        // Verify the double value is preserved correctly
        assertEquals("50000.50", optimizedTable.getValueAt(0, "Salary"));

        // Update the double value
        optimizedTable.setValueAt(0, "Salary", "60000.75");

        // Verify the updated double value is preserved correctly
        assertEquals("60000.75", optimizedTable.getValueAt(0, "Salary"));
    }

    /**
     * Test that OptimizedTableCore correctly handles default values.
     */
    @Test
    void testDefaultValues() {
        // Create a row with missing values
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        // Salary and IsActive are missing

        // Add the row to the optimized table with createDefaultValue=true
        optimizedTable.setCreateDefaultValue(true);
        optimizedTable.addRow(row);

        // Verify default values were added
        assertEquals("Alice", optimizedTable.getValueAt(0, "Name"));
        assertEquals("30", optimizedTable.getValueAt(0, "Age"));
        assertEquals("0.0", optimizedTable.getValueAt(0, "Salary"));
        assertEquals("false", optimizedTable.getValueAt(0, "IsActive"));

        // Create a new table with createDefaultValue=false
        OptimizedTableCore tableWithoutDefaults = new OptimizedTableCore();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");
        tableWithoutDefaults.setColumns(columns);
        tableWithoutDefaults.setCreateDefaultValue(false);

        // Verify that adding a row with missing values throws an exception
        assertThrows(IllegalArgumentException.class, () -> tableWithoutDefaults.addRow(row));
    }

    /**
     * Test that OptimizedTableCore correctly infers types.
     */
    @Test
    void testInferType() {
        // Test inferring types
        assertEquals("int", optimizedTable.inferType("123"));
        assertEquals("double", optimizedTable.inferType("123.45"));
        assertEquals("boolean", optimizedTable.inferType("true"));
        assertEquals("string", optimizedTable.inferType("hello"));
    }

    /**
     * Test the performance of OptimizedTableCore compared to TableCore.
     * This test adds a large number of rows to both tables and measures the time taken.
     */
    @Test
    void testPerformance() {
        // Number of rows to add
        int numRows = 10000;

        // Create an optimized table with known capacity
        OptimizedTableCore optimizedTableWithCapacity = new OptimizedTableCore(numRows);
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");
        optimizedTableWithCapacity.setColumns(columns);

        // Measure time to add rows to standard table
        long startTimeStandard = System.currentTimeMillis();
        for (int i = 0; i < numRows; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("Name", "Person" + i);
            row.put("Age", String.valueOf(20 + (i % 50)));
            row.put("Salary", String.valueOf(50000.0 + (i * 100.0)));
            row.put("IsActive", String.valueOf(i % 2 == 0));
            standardTable.addRow(row);
        }
        long endTimeStandard = System.currentTimeMillis();
        long standardTime = endTimeStandard - startTimeStandard;

        // Measure time to add rows to optimized table with capacity
        long startTimeOptimized = System.currentTimeMillis();
        for (int i = 0; i < numRows; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("Name", "Person" + i);
            row.put("Age", String.valueOf(20 + (i % 50)));
            row.put("Salary", String.valueOf(50000.0 + (i * 100.0)));
            row.put("IsActive", String.valueOf(i % 2 == 0));
            optimizedTableWithCapacity.addRow(row);
        }
        long endTimeOptimized = System.currentTimeMillis();
        long optimizedTime = endTimeOptimized - startTimeOptimized;

        // Print performance results
        System.out.println("Standard TableCore time: " + standardTime + " ms");
        System.out.println("Optimized TableCore time: " + optimizedTime + " ms");
        System.out.println("Performance improvement: " + (standardTime - optimizedTime) + " ms (" + 
                           (standardTime > 0 ? (100.0 * (standardTime - optimizedTime) / standardTime) : 0) + "%)");

        // Verify both tables have the same content
        assertEquals(standardTable.getRowCount(), optimizedTableWithCapacity.getRowCount());
        for (int i = 0; i < numRows; i++) {
            assertEquals(standardTable.getValueAt(i, "Name"), optimizedTableWithCapacity.getValueAt(i, "Name"));
            assertEquals(standardTable.getValueAt(i, "Age"), optimizedTableWithCapacity.getValueAt(i, "Age"));
            assertEquals(standardTable.getValueAt(i, "Salary"), optimizedTableWithCapacity.getValueAt(i, "Salary"));
            assertEquals(standardTable.getValueAt(i, "IsActive"), optimizedTableWithCapacity.getValueAt(i, "IsActive"));
        }
    }

    /**
     * Test the performance impact of proper sizing strategies for concurrent collections.
     * This test compares the performance of adding rows with double values to a concurrent table
     * with and without proper sizing strategies.
     */
    @Test
    void testConcurrentCollectionSizing() {
        // Number of rows to add
        int numRows = 5000;

        // Create tables for testing
        OptimizedTableCore concurrentTableWithSizing = new OptimizedTableCore(true, numRows);
        OptimizedTableCore concurrentTableWithoutSizing = new OptimizedTableCore(true);

        // Set up columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Salary", "double"); // We'll focus on double values to test originalDoubleStrings map
        concurrentTableWithSizing.setColumns(columns);
        concurrentTableWithoutSizing.setColumns(columns);

        // Prepare test data - all rows have double values to ensure originalDoubleStrings is used
        List<Map<String, String>> testRows = new ArrayList<>(numRows);
        for (int i = 0; i < numRows; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("Name", "Person" + i);
            row.put("Salary", String.valueOf(50000.0 + (i * 0.01))); // Ensure it has a decimal point
            testRows.add(row);
        }

        // Measure time to add rows to table with sizing
        long startTimeWithSizing = System.currentTimeMillis();
        for (Map<String, String> row : testRows) {
            concurrentTableWithSizing.addRow(row);
        }
        long endTimeWithSizing = System.currentTimeMillis();
        long timeWithSizing = endTimeWithSizing - startTimeWithSizing;

        // Measure time to add rows to table without sizing
        long startTimeWithoutSizing = System.currentTimeMillis();
        for (Map<String, String> row : testRows) {
            concurrentTableWithoutSizing.addRow(row);
        }
        long endTimeWithoutSizing = System.currentTimeMillis();
        long timeWithoutSizing = endTimeWithoutSizing - startTimeWithoutSizing;

        // Print performance results
        System.out.println("Concurrent table with sizing: " + timeWithSizing + " ms");
        System.out.println("Concurrent table without sizing: " + timeWithoutSizing + " ms");
        System.out.println("Performance difference: " + (timeWithoutSizing - timeWithSizing) + " ms");

        // Verify both tables have the same content
        assertEquals(concurrentTableWithSizing.getRowCount(), concurrentTableWithoutSizing.getRowCount());
        for (int i = 0; i < numRows; i++) {
            assertEquals(
                concurrentTableWithSizing.getValueAt(i, "Name"), 
                concurrentTableWithoutSizing.getValueAt(i, "Name")
            );
            assertEquals(
                concurrentTableWithSizing.getValueAt(i, "Salary"), 
                concurrentTableWithoutSizing.getValueAt(i, "Salary")
            );
        }

        // We don't assert on performance because it can vary between runs,
        // but we expect the sized version to be at least as fast as the unsized version
        // The test output will show the actual performance difference
    }

    /**
     * Test the concurrent version of OptimizedTableCore.
     */
    @Test
    void testConcurrentTable() {
        // Create a concurrent optimized table
        OptimizedTableCore concurrentTable = new OptimizedTableCore(true);
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");
        concurrentTable.setColumns(columns);

        // Add a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Salary", "50000.50");
        row.put("IsActive", "true");
        concurrentTable.addRow(row);

        // Verify the row was added correctly
        assertEquals(1, concurrentTable.getRowCount());
        assertEquals("Alice", concurrentTable.getValueAt(0, "Name"));
        assertEquals("30", concurrentTable.getValueAt(0, "Age"));
        assertEquals("50000.50", concurrentTable.getValueAt(0, "Salary"));
        assertEquals("true", concurrentTable.getValueAt(0, "IsActive"));
    }
}
