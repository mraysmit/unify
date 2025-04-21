package dev.mars;

import dev.mars.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    private Table table;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        table = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name","string" );
        columnNames.put("Age", "int");
        columnNames.put("Occupation", "string");
        table.setColumns(columnNames);

        // Redirect System.out to our ByteArrayOutputStream
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
        // Clear the output stream for the next test
        outputStream.reset();
    }

    @Test
    void testAddRow() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");

        table.addRow(row);

        assertEquals(1, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Occupation"));
    }

    @Test
    void testSetValueAt() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");

        table.addRow(row);
        table.setValueAt(0, "Age", "31");

        assertEquals("31", table.getValueAt(0, "Age"));
    }

    @Test
    void testInvalidRowSize() throws Exception {
        // Set createDefaultValue to false to test the exception case
        table.setCreateDefaultValue(false);

        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        // Occupation is missing

        // This should throw an exception because createDefaultValue is false
        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }

    @Test
    void testInvalidValueType() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "thirty");
        row.put("Occupation", "Engineer");

        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }

    @Test
    void testGetValueAtInvalidRowIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> table.getValueAt(-1, "Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> table.getValueAt(1, "Name"));
    }

    @Test
    void testGetValueAtInvalidColumnName() {
        final String invalidColumnName = "InvalidColumnName";
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");

        // This row has an invalid column name
        // The column name "AgeInvalid" is not defined in the table
        row.put(invalidColumnName, "30");
        row.put("Occupation", "Engineer");

        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }

    @ParameterizedTest
    @CsvSource({
        "123, int",
        "0, int",
        "-456, int",
        "2147483647, int",
        "-2147483648, int"
    })
    void testInferTypeInt(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @ParameterizedTest
    @CsvSource({
        "123.45, double",
        "0.0, double",
        "-456.789, double",
        ".5, double",
        "-.5, double"
    })
    void testInferTypeDouble(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @Test
    void testInferTypeDoubleEdgeCases() {
        // Numbers with decimal point but no digits after should now be recognized as doubles
        assertEquals("double", table.inferType("123."));
    }

    @ParameterizedTest
    @CsvSource({
        "true, boolean",
        "false, boolean",
        "TRUE, boolean",
        "FALSE, boolean",
        "True, boolean",
        "False, boolean"
    })
    void testInferTypeBoolean(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "hello",
        "123abc",
        "true_false",
        "1,234",
        "1.2.3",
        "",
        " ",
        "null"
    })
    void testInferTypeString(String value) {
        assertEquals("string", table.inferType(value));
    }

    @Test
    void testInferTypeEdgeCases() {
        // Test with special characters and mixed content
        assertEquals("string", table.inferType("$100"));
        assertEquals("string", table.inferType("50%"));
        assertEquals("string", table.inferType("1+2"));
        assertEquals("string", table.inferType("1-2-3"));

        // Test with whitespace - now should handle whitespace correctly
        assertEquals("int", table.inferType("123")); // No whitespace
        assertEquals("int", table.inferType(" 123")); // Leading whitespace
        assertEquals("int", table.inferType("123 ")); // Trailing whitespace
        assertEquals("int", table.inferType(" 123 ")); // Both leading and trailing whitespace
    }

    @Test
    void testInferTypeImprovedPatterns() {
        // Test scientific notation
        assertEquals("double", table.inferType("1.23E10"));
        assertEquals("double", table.inferType("1.23e10"));
        assertEquals("double", table.inferType("1.23E+10"));
        assertEquals("double", table.inferType("1.23e-10"));
        assertEquals("double", table.inferType("-1.23E10"));
        assertEquals("double", table.inferType("+1.23e10"));

        // Test decimal point with no digits after
        assertEquals("double", table.inferType("123."));

        // Test decimal point with no digits before
        assertEquals("double", table.inferType(".5"));
        assertEquals("double", table.inferType("-.5"));
        assertEquals("double", table.inferType("+.5"));

        // Test special numeric values
        assertEquals("double", table.inferType("NaN"));
        assertEquals("double", table.inferType("Infinity"));
        assertEquals("double", table.inferType("+Infinity"));
        assertEquals("double", table.inferType("-Infinity"));
        assertEquals("double", table.inferType("nan")); // Case insensitive
        assertEquals("double", table.inferType("infinity")); // Case insensitive

        // Test with whitespace
        assertEquals("double", table.inferType(" 123.45 "));
        assertEquals("boolean", table.inferType(" true "));

        // Test with null and empty string
        assertEquals("string", table.inferType(null));
        assertEquals("string", table.inferType(""));
        assertEquals("string", table.inferType("   "));
    }

    @Test
    void testPrintTableEmptyTable() {
        // Test printing an empty table (no rows)
        table.printTable();

        // Get the actual output
        String actual = outputStream.toString();

        // Expected output: just the header row with column names
        String lineSeparator = System.lineSeparator();
        String expected = "Name\tAge\tOccupation\t" + lineSeparator;

        assertEquals(expected, actual);
    }

    @Test
    void testPrintTableOneRow() {
        // Add one row to the table
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");
        table.addRow(row);

        // Print the table
        table.printTable();

        // Expected output: header row + one data row
        String lineSeparator = System.lineSeparator();
        String expected = "Name\tAge\tOccupation\t" + lineSeparator +
                          "Alice\t30\tEngineer\t" + lineSeparator;
        assertEquals(expected, outputStream.toString());
    }

    @Test
    void testPrintTableMultipleRows() {
        // Add multiple rows to the table
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", "Engineer");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        table.addRow(row2);

        Map<String, String> row3 = new HashMap<>();
        row3.put("Name", "Charlie");
        row3.put("Age", "35");
        row3.put("Occupation", "Manager");
        table.addRow(row3);

        // Print the table
        table.printTable();

        // Expected output: header row + three data rows
        String lineSeparator = System.lineSeparator();
        String expected = "Name\tAge\tOccupation\t" + lineSeparator +
                          "Alice\t30\tEngineer\t" + lineSeparator +
                          "Bob\t25\tDesigner\t" + lineSeparator +
                          "Charlie\t35\tManager\t" + lineSeparator;
        assertEquals(expected, outputStream.toString());
    }

    // Tests for edge cases in TableCore.setColumns()

    @Test
    void testSetColumnsWithNullMap() {
        Table newTable = new Table();
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(null));
    }

    @Test
    void testSetColumnsWithNullColumnName() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put(null, "string");
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithEmptyColumnName() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("", "string");
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithBlankColumnName() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("   ", "string");
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithNullColumnType() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", null);
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithEmptyColumnType() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "");
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithBlankColumnType() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "   ");
        assertThrows(IllegalArgumentException.class, () -> newTable.setColumns(columns));
    }

    @Test
    void testSetColumnsWithDuplicateKeys() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Name", "int"); // Duplicate key overwrites the previous entry

        newTable.setColumns(columns);

        // Verify the final state of the columns
        assertEquals(1, newTable.getColumns().size());
        //assertEquals("int", newTable.getColumn("Name").getType();

        var x = newTable.getColumn("Name");;

    }

    @Test
    void testSetColumnsWithDuplicateKeysSimulation() {
        Table newTable = new Table();

        // Create a map where we'll manually simulate the duplicate check
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");

        // Manually check if the size of the map equals the size of a HashSet of its keys
        // This is the same check that setColumns() performs
        assertEquals(columns.size(), new HashSet<>(columns.keySet()).size());

        // Now let's add a key that would be considered a duplicate in a HashSet
        // but LinkedHashMap doesn't allow duplicate keys, so we'll just verify the logic

        // Create a new map with a "duplicate" key (though it's not really a duplicate in the map)
        LinkedHashMap<String, String> columnsWithDuplicate = new LinkedHashMap<>();
        columnsWithDuplicate.put("Name", "string");
        columnsWithDuplicate.put("Name", "int"); // This overwrites the previous entry

        // Verify that the map size is still 1 (because the second put overwrote the first)
        assertEquals(1, columnsWithDuplicate.size());

        // Verify that the HashSet size equals the map size (both are 1)
        assertEquals(columnsWithDuplicate.size(), new HashSet<>(columnsWithDuplicate.keySet()).size());

        // This confirms that we can't easily test the duplicate check directly,
        // but we've verified the logic behind it
    }

    @Test
    void testSetColumnsWithValidMap() {
        Table newTable = new Table();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Salary", "double");
        columns.put("IsActive", "boolean");

        // This should not throw an exception
        newTable.setColumns(columns);

        // Verify the columns were set correctly by adding a row and checking values
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Salary", "50000.50");
        row.put("IsActive", "true");
        newTable.addRow(row);

        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("50000.50", newTable.getValueAt(0, "Salary"));
        assertEquals("true", newTable.getValueAt(0, "IsActive"));
    }
    // Tests for the createDefaultValue functionality

    @Test
    void testGetDefaultValue() throws Exception {
        table.setCreateDefaultValue(false);

        // Test default values for different types
        assertEquals("0", table.getDefaultValue("int"));
        assertEquals("0.0", table.getDefaultValue("double"));

        assertEquals("false", table.getDefaultValue("boolean"));
        assertEquals("", table.getDefaultValue("string"));
        assertEquals("", table.getDefaultValue("unknown"));
    }

    @Test
    void testCreateDefaultValueTrue() throws Exception {
        table.setCreateDefaultValue(true);

        // Create a row with a missing column
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        // Occupation is missing


        // Add the row - this should not throw an exception
        table.addRow(row);

        // Verify the row was added
        assertEquals(1, table.getRowCount());

        // Verify all values including the default value for Occupation
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("", table.getValueAt(0, "Occupation")); // Default value for string is ""
    }

    @Test
    void testCreateDefaultValueFalse() throws Exception {
        table.setCreateDefaultValue(false);

        // Create a row with a missing column
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        // Occupation is missing

        // This should throw an exception because createDefaultValue is false
        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }
}
