package dev.mars.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TableBuilder.
 * This class tests the functionality of the TableBuilder class.
 */
class TableBuilderTest {

    private TableBuilder builder;

    @BeforeEach
    void setUp() {
        // Create a new TableBuilder instance for each test
        builder = new TableBuilder();
    }

    @Test
    void testAddStringColumn() {
        // Add a string column
        builder.addStringColumn("Name");

        // Build the table
        ITable table = builder.build();

        // Verify the column was added correctly
        assertEquals(1, table.getColumnCount());
        assertEquals("Name", table.getColumnName(0));
        assertEquals(String.class, table.getColumn("Name").getType());
    }

    @Test
    void testAddIntColumn() {
        // Add an int column
        builder.addIntColumn("Age");

        // Build the table
        ITable table = builder.build();

        // Verify the column was added correctly
        assertEquals(1, table.getColumnCount());
        assertEquals("Age", table.getColumnName(0));
        assertEquals(Integer.class, table.getColumn("Age").getType());
    }

    @Test
    void testAddDoubleColumn() {
        // Add a double column
        builder.addDoubleColumn("Salary");

        // Build the table
        ITable table = builder.build();

        // Verify the column was added correctly
        assertEquals(1, table.getColumnCount());
        assertEquals("Salary", table.getColumnName(0));
        assertEquals(Double.class, table.getColumn("Salary").getType());
    }

    @Test
    void testAddBooleanColumn() {
        // Add a boolean column
        builder.addBooleanColumn("IsActive");

        // Build the table
        ITable table = builder.build();

        // Verify the column was added correctly
        assertEquals(1, table.getColumnCount());
        assertEquals("IsActive", table.getColumnName(0));
        assertEquals(Boolean.class, table.getColumn("IsActive").getType());
    }

    @Test
    void testAddMultipleColumns() {
        // Add multiple columns of different types
        builder.addStringColumn("Name")
               .addIntColumn("Age")
               .addDoubleColumn("Salary")
               .addBooleanColumn("IsActive");

        // Build the table
        ITable table = builder.build();

        // Verify the columns were added correctly
        assertEquals(4, table.getColumnCount());
        assertEquals("Name", table.getColumnName(0));
        assertEquals("Age", table.getColumnName(1));
        assertEquals("Salary", table.getColumnName(2));
        assertEquals("IsActive", table.getColumnName(3));

        assertEquals(String.class, table.getColumn("Name").getType());
        assertEquals(Integer.class, table.getColumn("Age").getType());
        assertEquals(Double.class, table.getColumn("Salary").getType());
        assertEquals(Boolean.class, table.getColumn("IsActive").getType());
    }

    @Test
    void testAddRowWithMap() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age");

        // Create a row as a map
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");

        // Add the row
        builder.addRow(row);

        // Build the table
        ITable table = builder.build();

        // Verify the row was added correctly
        assertEquals(1, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
    }

    @Test
    void testAddRowWithVarargs() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age");

        // Add a row using varargs
        builder.addRow("Name", "Bob", "Age", "25");

        // Build the table
        ITable table = builder.build();

        // Verify the row was added correctly
        assertEquals(1, table.getRowCount());
        assertEquals("Bob", table.getValueAt(0, "Name"));
        assertEquals("25", table.getValueAt(0, "Age"));
    }

    @Test
    void testAddMultipleRows() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age");

        // Add multiple rows
        builder.addRow("Name", "Alice", "Age", "30")
               .addRow("Name", "Bob", "Age", "25")
               .addRow("Name", "Charlie", "Age", "35");

        // Build the table
        ITable table = builder.build();

        // Verify the rows were added correctly
        assertEquals(3, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Bob", table.getValueAt(1, "Name"));
        assertEquals("25", table.getValueAt(1, "Age"));
        assertEquals("Charlie", table.getValueAt(2, "Name"));
        assertEquals("35", table.getValueAt(2, "Age"));
    }

    @Test
    void testAddRowsWithList() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age");

        // Create rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");

        // Add rows as a list
        builder.addRows(List.of(row1, row2));

        // Build the table
        ITable table = builder.build();

        // Verify the rows were added correctly
        assertEquals(2, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Bob", table.getValueAt(1, "Name"));
        assertEquals("25", table.getValueAt(1, "Age"));
    }

    @Test
    void testAddRowWithNonExistentColumn() {
        // Add a column
        builder.addStringColumn("Name");

        // Create a row with a non-existent column
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30"); // Age column doesn't exist

        // Add the row
        builder.addRow(row);

        // Build the table - should throw an exception
        assertThrows(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    void testAddRowWithOddNumberOfVarargs() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age");

        // Add a row with an odd number of varargs - should throw an exception
        assertThrows(IllegalArgumentException.class, () -> builder.addRow("Name", "Alice", "Age"));
    }

    @Test
    void testSetCreateDefaultValue() {
        // Add columns
        builder.addStringColumn("Name")
               .addIntColumn("Age")
               .setCreateDefaultValue(true);

        // Add a row with a missing column
        builder.addRow("Name", "Alice"); // Age is missing

        // Build the table
        ITable table = builder.build();

        // Verify the row was added with a default value for the missing column
        assertEquals(1, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("0", table.getValueAt(0, "Age")); // Default value for int is 0
    }

    @Test
    void testValueConversion() {
        // Add columns of different types
        builder.addStringColumn("Name")
               .addIntColumn("Age")
               .addDoubleColumn("Salary")
               .addBooleanColumn("IsActive");

        // Add a row with string values that need to be converted
        builder.addRow(
            "Name", "Alice",
            "Age", "30",
            "Salary", "75000.50",
            "IsActive", "true"
        );

        // Build the table
        ITable table = builder.build();

        // Verify the values were converted correctly
        assertEquals("Alice", table.getValue(0, "Name"));
        assertEquals(30, table.getValue(0, "Age"));
        assertEquals(75000.50, table.getValue(0, "Salary"));
        assertEquals(true, table.getValue(0, "IsActive"));
    }
}
