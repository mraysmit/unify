package dev.mars.jtable.io.csv;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// new
class CSVProcessorTest {

    private ITable table;
    private final String testFileName = "test.csv";

    @BeforeEach
    void setUp() {
        table = getTableWithColumns();

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", "Engineer");

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        table.addRow(row1);
        table.addRow(row2);
    }

    @AfterEach
    void tearDown() {
        File file = new File(testFileName);
        if (file.exists()) {
            try {
                Files.delete(file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    void testWriteToCSVWithHeaderRow() {
        CSVProcessor.writeToCSV(table, testFileName, true);
        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify the header row
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine();
            assertEquals("Name,Age,Occupation", header);

            String row1 = reader.readLine();
            assertEquals("Alice,30,Engineer", row1);

            String row2 = reader.readLine();
            assertEquals("Bob,25,Designer", row2);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testWriteToCSV() {
        CSVProcessor.writeToCSV(table, testFileName, false);
        File file = new File(testFileName);
        assertTrue(file.exists());
    }

    @Test
    void testReadFromCSVWithoutHeader() {
        CSVProcessor.writeToCSV(table, testFileName, false);

        final Table newTable = getTableWithoutColumns();
        CSVProcessor.readFromCSV(newTable, testFileName, false, false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Column1"));
        assertEquals("30", newTable.getValueAt(0, "Column2"));
        assertEquals("Engineer", newTable.getValueAt(0, "Column3"));
        assertEquals("Bob", newTable.getValueAt(1, "Column1"));
        assertEquals("25", newTable.getValueAt(1, "Column2"));
        assertEquals("Designer", newTable.getValueAt(1, "Column3"));
    }

    @Test
    void testReadFromCSVWithHeader() throws IOException {
        String csvContent = "Name,Age,Occupation\nAlice,30,Engineer\nBob,25,Designer\n";
        Files.write(new File(testFileName).toPath(), csvContent.getBytes());

        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true,false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("Engineer", newTable.getValueAt(0, "Occupation"));
        assertEquals("Bob", newTable.getValueAt(1, "Name"));
        assertEquals("25", newTable.getValueAt(1, "Age"));
        assertEquals("Designer", newTable.getValueAt(1, "Occupation"));
    }

    @Test
    void testEmptyTable() {
        Table emptyTable = new Table();
        CSVProcessor.writeToCSV(emptyTable, testFileName, false);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, false, false);
        assertEquals(0, newTable.getRowCount());
    }

    @Test
    void testSingleRowTable() {
        final var withHeaders = true;
        Table singleRowTable = getTableWithColumns();
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Charlie");
        row.put("Age", "40");
        row.put("Occupation", "Artist");
        singleRowTable.addRow(row);

        CSVProcessor.writeToCSV(singleRowTable, testFileName, withHeaders);
        // get a blank table as we are reading with headers
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, withHeaders, false);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Charlie", newTable.getValueAt(0, "Name"));
        assertEquals("40", newTable.getValueAt(0, "Age"));
        assertEquals("Artist", newTable.getValueAt(0, "Occupation"));
    }

    @Test
    void testSingleColumnTableWithHeader() {
        final var withHeaders = true;
        Table singleColumnTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        singleColumnTable.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        singleColumnTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        singleColumnTable.addRow(row2);

        CSVProcessor.writeToCSV(singleColumnTable, testFileName, withHeaders);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, withHeaders, false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("Bob", newTable.getValueAt(1, "Name"));
    }

    @Test
    void testSingleColumnWithoutHeader() {
        Table singleColumnTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Column1", "string");
        singleColumnTable.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Column1", "Alice");
        singleColumnTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Column1", "Bob");
        singleColumnTable.addRow(row2);

        CSVProcessor.writeToCSV(singleColumnTable, testFileName, false);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, false, false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Column1"));
        assertEquals("Bob", newTable.getValueAt(1, "Column1"));
    }

    @Test
    void testSpecialCharacters() {
        final var withHeaders = true;
        Table specialCharTable = getTableWithColumns();
        Map<String, String> row = new HashMap<>();
        row.put("Name", "D@vid");
        row.put("Age", "35");
        row.put("Occupation", "D&veloper");
        specialCharTable.addRow(row);

        CSVProcessor.writeToCSV(specialCharTable, testFileName, withHeaders);
        // get a blank table as we are reading with headers
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, withHeaders, false);

        assertEquals(1, newTable.getRowCount());
        assertEquals("D@vid", newTable.getValueAt(0, "Name"));
        assertEquals("35", newTable.getValueAt(0, "Age"));
        assertEquals("D&veloper", newTable.getValueAt(0, "Occupation"));
    }

    @Test
    void testWriteToCSVWithMixedDataTypes() {
        Table mixedDataTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("Age", "int");
        columnNames.put("IsEmployed", "boolean");
        mixedDataTable.setColumns(columnNames);

        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("IsEmployed", "true");
        mixedDataTable.addRow(row);

        CSVProcessor.writeToCSV(mixedDataTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("true", newTable.getValueAt(0, "IsEmployed"));
    }

    @Test
    void testWriteToCSVWithEmptyHeaderRow() {
        final var testFileName = "output.csv";
        final var csvContent = "Alice,30,Engineer\n";
        final var hasHeaderRow = false;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFileName))) {
            writer.write(csvContent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, hasHeaderRow, false);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Column1"));
        assertEquals("30", newTable.getValueAt(0, "Column2"));
        assertEquals("Engineer", newTable.getValueAt(0, "Column3"));
    }

    @Test
    void testWriteToCSVWithSpecialCharactersInHeader() {
        Table specialHeaderTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("N@me", "string");
        columnNames.put("A#e", "int");
        columnNames.put("Occu*pation", "string");

        specialHeaderTable.setColumns(columnNames);

        Map<String, String> row = new HashMap<>();
        row.put("N@me", "Alice");
        row.put("A#e", "30");
        row.put("Occu*pation", "Engineer");
        specialHeaderTable.addRow(row);

        CSVProcessor.writeToCSV(specialHeaderTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "N@me"));
        assertEquals("30", newTable.getValueAt(0, "A#e"));
        assertEquals("Engineer", newTable.getValueAt(0, "Occu*pation"));
    }

    @Test
    void testWriteToCSVWithLargeDataSet() {
        Table largeDataTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("ID", "int");
        columnNames.put("Value", "string");
        largeDataTable.setColumns(columnNames);

        for (int i = 0; i < 1000; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("ID", String.valueOf(i));
            row.put("Value", "Value" + i);
            largeDataTable.addRow(row);
        }

        CSVProcessor.writeToCSV(largeDataTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(1000, newTable.getRowCount());
        for (int i = 0; i < 1000; i++) {
            assertEquals(String.valueOf(i), newTable.getValueAt(i, "ID"));
            assertEquals("Value" + i, newTable.getValueAt(i, "Value"));
        }
    }

    @Test
    void testDuplicateColumnNames() {
        Table duplicateColumnTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("Name", "string"); // Duplicate column name
        duplicateColumnTable.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        duplicateColumnTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        duplicateColumnTable.addRow(row2);

        CSVProcessor.writeToCSV(duplicateColumnTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("Bob", newTable.getValueAt(1, "Name"));
    }

    @Test
    void testNumericAndDecimalValues() {
        Table numericTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("ID", "int");
        columnNames.put("Value", "double");
        numericTable.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("ID", "1");
        row1.put("Value", "123.45");
        numericTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("ID", "2");
        row2.put("Value", "678.90");
        numericTable.addRow(row2);

        CSVProcessor.writeToCSV(numericTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(2, newTable.getRowCount());
        assertEquals("1", newTable.getValueAt(0, "ID"));
        assertEquals("123.45", newTable.getValueAt(0, "Value"));
        assertEquals("2", newTable.getValueAt(1, "ID"));
        assertEquals("678.90", newTable.getValueAt(1, "Value"));
    }

    @Test
    void testManyColumns() {
        Table manyColumnsTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        for (int i = 1; i <= 100; i++) {
            columnNames.put("Column" + i, "string");
        }
        manyColumnsTable.setColumns(columnNames);

        Map<String, String> row = new HashMap<>();
        for (int i = 1; i <= 100; i++) {
            row.put("Column" + i, "Value" + i);
        }
        manyColumnsTable.addRow(row);

        CSVProcessor.writeToCSV(manyColumnsTable, testFileName, true);
        Table newTable = new Table();
        CSVProcessor.readFromCSV(newTable, testFileName, true, false);

        assertEquals(1, newTable.getRowCount());
        for (int i = 1; i <= 100; i++) {
            assertEquals("Value" + i, newTable.getValueAt(0, "Column" + i));
        }
    }

    @Test
    void testEmptyValues() {
        Table emptyValuesTable = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("Age", "int");
        columnNames.put("Occupation", "string");
        emptyValuesTable.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", ""); // Empty value
        emptyValuesTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        emptyValuesTable.addRow(row2);

        CSVProcessor.writeToCSV(emptyValuesTable, testFileName, true);
        Table newTable = new Table();
        // Read the file content to verify the empty values and allowEmptyValues
        CSVProcessor.readFromCSV(newTable, testFileName, true, true);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("", newTable.getValueAt(0, "Occupation"));
        assertEquals("", newTable.getValueAt(1, "Name"));
        assertEquals("25", newTable.getValueAt(1, "Age"));
        assertEquals("Designer", newTable.getValueAt(1, "Occupation"));
    }

    private static Table getTableWithColumns() {
        Table newTable = new Table();
        var columns = createColumns(Strings.of("Name", "Age", "Occupation"), Strings.of("string", "int", "string"));
        newTable.setColumns(columns);
        return newTable;
    }

    private static Table getTableWithoutColumns() {
        Table newTable = new Table();
        // Do not set any Column names and types
        var columnNames = new LinkedHashMap<String, String>();
        newTable.setColumns(columnNames);
        return newTable;
    }

    private static LinkedHashMap<String, String> createColumns(String[] names, String[] types) {
        var columnNames = new LinkedHashMap<String, String>();

        for (int i = 0; i < names.length; i++) {
            columnNames.put(names[i], types[i]);
        }

        return columnNames;
    }

    private class Strings {
        public static String[] of(String... values) {
            return values;
        }
    }
}