package dev.mars.xml;

import dev.mars.table.Table;
import dev.mars.adapter.XMLTableAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for XML reading and writing functionality.
 * This class tests the XMLReader and XMLWriter classes.
 */
class XMLTest {

    private Table table;
    private XMLTableAdapter adapter;
    private XMLReader reader;
    private XMLWriter writer;
    private final String testInputFileName = "src/test/resources/test_data.xml";
    private final String testOutputFileName = "test_output.xml";

    @BeforeEach
    void setUp() {
        // Create a new Table instance for each test
        table = new Table();

        // Create the adapter with the table
        adapter = new XMLTableAdapter(table);

        // Create reader and writer instances
        reader = new XMLReader();
        writer = new XMLWriter();
    }

    @AfterEach
    void tearDown() {
        // Delete the output file if it exists
        File outputFile = new File(testOutputFileName);
        if (outputFile.exists()) {
            outputFile.delete();
        }
    }

    @Test
    void testReadFromXML() {
        // Read data from the test XML file
        reader.readFromXML(adapter, testInputFileName, "data", "row");

        // Verify that the data was read correctly
        assertEquals(5, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify column names
        assertEquals("Name", table.getColumnName(0));
        assertEquals("Age", table.getColumnName(1));
        assertEquals("Occupation", table.getColumnName(2));
        assertEquals("Salary", table.getColumnName(3));
        assertEquals("IsEmployed", table.getColumnName(4));

        // Verify first row values
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Occupation"));
        assertEquals("75000.50", table.getValueAt(0, "Salary"));
        assertEquals("true", table.getValueAt(0, "IsEmployed"));

        // Verify last row values
        assertEquals("Eve", table.getValueAt(4, "Name"));
        assertEquals("22", table.getValueAt(4, "Age"));
        assertEquals("Intern", table.getValueAt(4, "Occupation"));
        assertEquals("45000.00", table.getValueAt(4, "Salary"));
        assertEquals("false", table.getValueAt(4, "IsEmployed"));
    }

    @Test
    void testWriteToXML() throws IOException {
        // Set up a table with data
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        columns.put("Salary", "double");
        columns.put("IsEmployed", "boolean");
        table.setColumns(columns);

        // Add rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", "Engineer");
        row1.put("Salary", "75000.50");
        row1.put("IsEmployed", "true");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        row2.put("Salary", "65000.75");
        row2.put("IsEmployed", "true");
        table.addRow(row2);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Verify that the file was created
        File outputFile = new File(testOutputFileName);
        assertTrue(outputFile.exists());

        // Read the file content to verify it contains valid XML
        String content = new String(Files.readAllBytes(outputFile.toPath()));
        System.out.println("[DEBUG_LOG] XML content: " + content);

        // Check for the presence of key elements
        assertTrue(content.contains("<data>"), "Root element not found");
        assertTrue(content.contains("<row>"), "Row element not found");
        assertTrue(content.contains("<Name>Alice</Name>"), "Name:Alice not found");
        assertTrue(content.contains("<Age>30</Age>"), "Age:30 not found");
        assertTrue(content.contains("<Occupation>Engineer</Occupation>"), "Occupation:Engineer not found");
        assertTrue(content.contains("<Salary>75000.50</Salary>"), "Salary:75000.5 not found");
        assertTrue(content.contains("<IsEmployed>true</IsEmployed>"), "IsEmployed:true not found");
    }

    @Test
    void testRoundTrip() {
        // Read data from the test XML file
        reader.readFromXML(adapter, testInputFileName, "data", "row");

        // Write the data to a new XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", false);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the new XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the data is the same
        assertEquals(table.getRowCount(), newTable.getRowCount());
        assertEquals(table.getColumnCount(), newTable.getColumnCount());

        // Verify all values
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
                String columnName = table.getColumnName(j);
                assertEquals(table.getValueAt(i, columnName), newTable.getValueAt(i, columnName));
            }
        }
    }

    @Test
    void testReadWithDifferentRootElement() throws IOException {
        // Create an XML file with a different root element
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<people>\n" +
                     "  <person>\n" +
                     "    <Name>Alice</Name>\n" +
                     "    <Age>30</Age>\n" +
                     "    <Occupation>Engineer</Occupation>\n" +
                     "  </person>\n" +
                     "</people>";
        Files.write(Paths.get(testOutputFileName), xml.getBytes());

        // Read data from the test XML file with different root and row elements
        reader.readFromXML(adapter, testOutputFileName, "people", "person");

        // Verify that the data was read correctly
        assertEquals(1, table.getRowCount());
        assertEquals(3, table.getColumnCount());

        // Verify first row values
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Occupation"));
    }

    @Test
    void testReadEmptyXML() throws IOException {
        // Create an XML file with an empty root element
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                     "<data>\n" +
                     "</data>";
        Files.write(Paths.get(testOutputFileName), xml.getBytes());

        // Read data from the empty XML file
        reader.readFromXML(adapter, testOutputFileName, "data", "row");

        // Verify that no data was read
        assertEquals(0, table.getRowCount());
    }

    @Test
    void testWriteEmptyTable() {
        // Write an empty table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", false);

        // Verify that the file was created
        File outputFile = new File(testOutputFileName);
        assertTrue(outputFile.exists());

        // Read the file content to verify it contains an empty root element
        try {
            String content = new String(Files.readAllBytes(outputFile.toPath()));
            assertTrue(content.contains("<data>"), "Root element not found");
            assertTrue(content.contains("</data>"), "Root closing element not found");
            assertFalse(content.contains("<row>"), "Row element found in empty table");
        } catch (IOException e) {
            fail("Failed to read output file: " + e.getMessage());
        }
    }

    @Test
    void testSpecialCharacters() {
        // Set up a table with special characters
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Description", "string");
        table.setColumns(columns);

        // Add a row with special characters
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Special \"Quoted\" Name");
        row.put("Description", "Contains special characters: < > & ' \"");
        table.addRow(row);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", false);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the special characters were preserved
        assertEquals("Special \"Quoted\" Name", newTable.getValueAt(0, "Name"));
        assertEquals("Contains special characters: < > & ' \"", newTable.getValueAt(0, "Description"));
    }

    @Test
    void testSingleRowTable() {
        // Set up a table with a single row
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        table.setColumns(columns);

        // Add a single row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Charlie");
        row.put("Age", "40");
        row.put("Occupation", "Artist");
        table.addRow(row);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the data was read correctly
        assertEquals(1, newTable.getRowCount());
        assertEquals(3, newTable.getColumnCount());
        assertEquals("Charlie", newTable.getValueAt(0, "Name"));
        assertEquals("40", newTable.getValueAt(0, "Age"));
        assertEquals("Artist", newTable.getValueAt(0, "Occupation"));
    }

    @Test
    void testSingleColumnTable() {
        // Set up a table with a single column
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        table.setColumns(columns);

        // Add rows
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        table.addRow(row2);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the data was read correctly
        assertEquals(2, newTable.getRowCount());
        assertEquals(1, newTable.getColumnCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("Bob", newTable.getValueAt(1, "Name"));
    }

    @Test
    void testEmptyValues() {
        // Set up a table with empty values
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        table.setColumns(columns);

        // Add rows with empty values
        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", ""); // Empty value
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        table.addRow(row2);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the empty values were preserved
        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("", newTable.getValueAt(0, "Occupation"));
        assertEquals("", newTable.getValueAt(1, "Name"));
        assertEquals("25", newTable.getValueAt(1, "Age"));
        assertEquals("Designer", newTable.getValueAt(1, "Occupation"));
    }

    @Test
    void testLargeDataSet() {
        // Set up a table for a large dataset
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "int");
        columns.put("Value", "string");
        table.setColumns(columns);

        // Add many rows
        for (int i = 0; i < 1000; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("ID", String.valueOf(i));
            row.put("Value", "Value" + i);
            table.addRow(row);
        }

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the data was read correctly
        assertEquals(1000, newTable.getRowCount());
        assertEquals(2, newTable.getColumnCount());

        // Check a sample of the data
        assertEquals("0", newTable.getValueAt(0, "ID"));
        assertEquals("Value0", newTable.getValueAt(0, "Value"));
        assertEquals("999", newTable.getValueAt(999, "ID"));
        assertEquals("Value999", newTable.getValueAt(999, "Value"));
    }

    @Test
    void testManyColumns() {
        // Set up a table with many columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (int i = 1; i <= 50; i++) {
            columns.put("Column" + i, "string");
        }
        table.setColumns(columns);

        // Add a row with values for all columns
        Map<String, String> row = new HashMap<>();
        for (int i = 1; i <= 50; i++) {
            row.put("Column" + i, "Value" + i);
        }
        table.addRow(row);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the data was read correctly
        assertEquals(1, newTable.getRowCount());
        assertEquals(50, newTable.getColumnCount());

        // Check a sample of the data
        for (int i = 1; i <= 50; i++) {
            assertEquals("Value" + i, newTable.getValueAt(0, "Column" + i));
        }
    }

    @Test
    void testNumericAndDecimalValues() {
        // Set up a table with numeric and decimal values
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("IntValue", "int");
        columns.put("DoubleValue", "double");
        columns.put("BooleanValue", "boolean");
        table.setColumns(columns);

        // Add rows with various numeric values
        Map<String, String> row1 = new HashMap<>();
        row1.put("IntValue", "123");
        row1.put("DoubleValue", "456.78");
        row1.put("BooleanValue", "true");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("IntValue", "-987");
        row2.put("DoubleValue", "-654.32");
        row2.put("BooleanValue", "false");
        table.addRow(row2);

        // Write the table to an XML file
        writer.writeToXML(adapter, testOutputFileName, "data", "row", true);

        // Create a new table and adapter
        Table newTable = new Table();
        XMLTableAdapter newAdapter = new XMLTableAdapter(newTable);

        // Read the data back from the XML file
        reader.readFromXML(newAdapter, testOutputFileName, "data", "row");

        // Verify that the numeric values were preserved
        assertEquals(2, newTable.getRowCount());
        assertEquals(3, newTable.getColumnCount());

        assertEquals("123", newTable.getValueAt(0, "IntValue"));
        assertEquals("456.78", newTable.getValueAt(0, "DoubleValue"));
        assertEquals("true", newTable.getValueAt(0, "BooleanValue"));

        assertEquals("-987", newTable.getValueAt(1, "IntValue"));
        assertEquals("-654.32", newTable.getValueAt(1, "DoubleValue"));
        assertEquals("false", newTable.getValueAt(1, "BooleanValue"));
    }
}