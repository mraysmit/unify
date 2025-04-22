package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.core.table.TableBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CSVMappingWriter class that are equivalent to CSVProcessorTest.
 * This class tests the CSVMappingWriter functionality with scenarios similar to those in CSVProcessorTest.
 */
public class CSVMappingWriterProcessorTest {

    private ITable table;
    private final String testFileName = "test_mapping_processor.csv";

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
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Occupation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(table, config);

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
    void testWriteToCSVWithoutHeaderRow() {
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", false)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Occupation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(table, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify there's no header row
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String firstLine = reader.readLine();
            assertEquals("Alice,30,Engineer", firstLine);

            String secondLine = reader.readLine();
            assertEquals("Bob,25,Designer", secondLine);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testEmptyTable() {
        Table emptyTable = new Table();
        
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", false);
        
        // Write the empty table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(emptyTable, config);
        
        File file = new File(testFileName);
        assertTrue(file.exists());
        
        // Verify the file is empty or has only a header row
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertTrue(lines.isEmpty(), "File should be empty for an empty table");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSingleRowTable() {
        Table singleRowTable = getTableWithColumns();
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Charlie");
        row.put("Age", "40");
        row.put("Occupation", "Artist");
        singleRowTable.addRow(row);

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Occupation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(singleRowTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(2, lines.size(), "File should have header + 1 data row");
            assertEquals("Name,Age,Occupation", lines.get(0), "Header should match");
            assertEquals("Charlie,40,Artist", lines.get(1), "Data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSingleColumnTable() {
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

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(singleColumnTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(3, lines.size(), "File should have header + 2 data rows");
            assertEquals("FullName", lines.get(0), "Header should match");
            assertEquals("Alice", lines.get(1), "First data row should match");
            assertEquals("Bob", lines.get(2), "Second data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSpecialCharacters() {
        Table specialCharTable = getTableWithColumns();
        Map<String, String> row = new HashMap<>();
        row.put("Name", "D@vid");
        row.put("Age", "35");
        row.put("Occupation", "D&veloper");
        specialCharTable.addRow(row);

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Occupation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(specialCharTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(2, lines.size(), "File should have header + 1 data row");
            assertEquals("Name,Age,Occupation", lines.get(0), "Header should match");
            assertEquals("D@vid,35,D&veloper", lines.get(1), "Data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMixedDataTypes() {
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

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("IsEmployed", "Employed", "boolean"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(mixedDataTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(2, lines.size(), "File should have header + 1 data row");
            assertEquals("Name,Age,Employed", lines.get(0), "Header should match");
            assertEquals("Alice,30,true", lines.get(1), "Data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testSpecialCharactersInHeader() {
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

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("N@me", "N@me", "string"))
                .addColumnMapping(new ColumnMapping("A#e", "A#e", "int"))
                .addColumnMapping(new ColumnMapping("Occu*pation", "Occu*pation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(specialHeaderTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(2, lines.size(), "File should have header + 1 data row");
            assertEquals("N@me,A#e,Occu*pation", lines.get(0), "Header should match");
            assertEquals("Alice,30,Engineer", lines.get(1), "Data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testLargeDataSet() {
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

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("ID", "Identifier", "int"))
                .addColumnMapping(new ColumnMapping("Value", "Data", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(largeDataTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Verify the file has the expected number of lines
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(1001, lines.size(), "File should have header + 1000 data rows");
            assertEquals("Identifier,Data", lines.get(0), "Header should match");
            assertEquals("0,Value0", lines.get(1), "First data row should match");
            assertEquals("999,Value999", lines.get(1000), "Last data row should match");
        } catch (IOException e) {
            e.printStackTrace();
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

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(testFileName)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Occupation", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(emptyValuesTable, config);

        File file = new File(testFileName);
        assertTrue(file.exists());

        // Read the file content to verify
        try {
            List<String> lines = Files.readAllLines(Paths.get(testFileName));
            assertEquals(3, lines.size(), "File should have header + 2 data rows");
            assertEquals("Name,Age,Occupation", lines.get(0), "Header should match");
            assertEquals("Alice,30,", lines.get(1), "First data row should match");
            assertEquals(",25,Designer", lines.get(2), "Second data row should match");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Table getTableWithColumns() {
        Table newTable = new Table();
        var columns = createColumns(new String[]{"Name", "Age", "Occupation"}, new String[]{"string", "int", "string"});
        newTable.setColumns(columns);
        return newTable;
    }

    private static LinkedHashMap<String, String> createColumns(String[] names, String[] types) {
        var columnNames = new LinkedHashMap<String, String>();

        for (int i = 0; i < names.length; i++) {
            columnNames.put(names[i], types[i]);
        }

        return columnNames;
    }
}