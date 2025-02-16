package dev.mars;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CSVUtilsTest {

    private Table table;
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
        CSVUtils.writeToCSV(table, testFileName, true);
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
        CSVUtils.writeToCSV(table, testFileName, false);
        File file = new File(testFileName);
        assertTrue(file.exists());
    }

    @Test
    void testReadFromCSVWithoutHeader() {
        CSVUtils.writeToCSV(table, testFileName, false);

        final Table newTable = getTableWithoutColumns();
        CSVUtils.readFromCSV(newTable, testFileName, false);

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
        CSVUtils.readFromCSV(newTable, testFileName, true);

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
        CSVUtils.writeToCSV(emptyTable, testFileName, false);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, false);
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

        CSVUtils.writeToCSV(singleRowTable, testFileName, withHeaders);
        Table newTable = getTableWithColumns();
        CSVUtils.readFromCSV(newTable, testFileName, withHeaders);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Charlie", newTable.getValueAt(0, "Name"));
        assertEquals("40", newTable.getValueAt(0, "Age"));
        assertEquals("Artist", newTable.getValueAt(0, "Occupation"));
    }

    @Test
    void testSingleColumnTable() {
        final var withHeaders = true;
        Table singleColumnTable = new Table();
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "Name");
        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("Name", "string");
        singleColumnTable.setColumnNames(columnNames, columnTypes);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        singleColumnTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        singleColumnTable.addRow(row2);

        CSVUtils.writeToCSV(singleColumnTable, testFileName, withHeaders);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, withHeaders);

        assertEquals(2, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("Bob", newTable.getValueAt(1, "Name"));
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

        CSVUtils.writeToCSV(specialCharTable, testFileName, withHeaders);
        Table newTable = getTableWithColumns();
        CSVUtils.readFromCSV(newTable, testFileName, withHeaders);

        assertEquals(1, newTable.getRowCount());
        assertEquals("D@vid", newTable.getValueAt(0, "Name"));
        assertEquals("35", newTable.getValueAt(0, "Age"));
        assertEquals("D&veloper", newTable.getValueAt(0, "Occupation"));
    }

    @Test
    void testWriteToCSVWithMixedDataTypes() {
        Table mixedDataTable = new Table();
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "Name");
        columnNames.put(1, "Age");
        columnNames.put(2, "IsEmployed");
        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("Name", "string");
        columnTypes.put("Age", "int");
        columnTypes.put("IsEmployed", "boolean");
        mixedDataTable.setColumnNames(columnNames, columnTypes);

        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("IsEmployed", "true");
        mixedDataTable.addRow(row);

        CSVUtils.writeToCSV(mixedDataTable, testFileName, true);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, true);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "Name"));
        assertEquals("30", newTable.getValueAt(0, "Age"));
        assertEquals("true", newTable.getValueAt(0, "IsEmployed"));
    }

    @Test
    void testWriteToCSVWithEmptyHeaderRow() {
        Table emptyHeaderTable = new Table();
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "");
        columnNames.put(1, "");
        columnNames.put(2, "");
        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("", "string");
        emptyHeaderTable.setColumnNames(columnNames, columnTypes);

        Map<String, String> row = new HashMap<>();
        row.put("", "Value1");
        row.put("", "Value2");
        row.put("", "Value3");
        emptyHeaderTable.addRow(row);

        CSVUtils.writeToCSV(emptyHeaderTable, testFileName, true);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, true);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Value1", newTable.getValueAt(0, "Column1"));
        assertEquals("Value2", newTable.getValueAt(0, "Column2"));
        assertEquals("Value3", newTable.getValueAt(0, "Column3"));
    }

    @Test
    void testWriteToCSVWithSpecialCharactersInHeader() {
        Table specialHeaderTable = new Table();
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "N@me");
        columnNames.put(1, "A#e");
        columnNames.put(2, "Occu*pation");
        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("N@me", "string");
        columnTypes.put("A#e", "int");
        columnTypes.put("Occu*pation", "string");
        specialHeaderTable.setColumnNames(columnNames, columnTypes);

        Map<String, String> row = new HashMap<>();
        row.put("N@me", "Alice");
        row.put("A#e", "30");
        row.put("Occu*pation", "Engineer");
        specialHeaderTable.addRow(row);

        CSVUtils.writeToCSV(specialHeaderTable, testFileName, true);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, true);

        assertEquals(1, newTable.getRowCount());
        assertEquals("Alice", newTable.getValueAt(0, "N@me"));
        assertEquals("30", newTable.getValueAt(0, "A#e"));
        assertEquals("Engineer", newTable.getValueAt(0, "Occu*pation"));
    }


    @Test
    void testWriteToCSVWithLargeDataSet() {
        Table largeDataTable = new Table();
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "ID");
        columnNames.put(1, "Value");
        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("ID", "int");
        columnTypes.put("Value", "string");
        largeDataTable.setColumnNames(columnNames, columnTypes);

        for (int i = 0; i < 1000; i++) {
            Map<String, String> row = new HashMap<>();
            row.put("ID", String.valueOf(i));
            row.put("Value", "Value" + i);
            largeDataTable.addRow(row);
        }

        CSVUtils.writeToCSV(largeDataTable, testFileName, true);
        Table newTable = new Table();
        CSVUtils.readFromCSV(newTable, testFileName, true);

        assertEquals(1000, newTable.getRowCount());
        for (int i = 0; i < 1000; i++) {
            assertEquals(String.valueOf(i), newTable.getValueAt(i, "ID"));
            assertEquals("Value" + i, newTable.getValueAt(i, "Value"));
        }
    }

    private static Table getTableWithColumns() {
        Table newTable = new Table();

        // Set column names and types before reading the data rows
        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "Name");
        columnNames.put(1, "Age");
        columnNames.put(2, "Occupation");

        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("Name", "string");
        columnTypes.put("Age", "int");
        columnTypes.put("Occupation", "string");

        newTable.setColumnNames(columnNames, columnTypes);
        return newTable;
    }

    private static Table getTableWithoutColumns() {
        Table newTable = new Table();

        // Do not set any Column names and types
        Map<Integer, String> columnNames = new HashMap<>();
        Map<String, String> columnTypes = new HashMap<>();

        newTable.setColumnNames(columnNames, columnTypes);
        return newTable;
    }
}