package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the CSVMappingWriter class.
 * This class tests the functionality of the CSVMappingWriter class.
 */
class CSVMappingWriterTest {

    private static final String TEST_CSV_FILE = "test_mapping_writer.csv";
    private static final String TEST_OUTPUT_CSV_FILE = "test_mapping_output.csv";

    @BeforeEach
    void setUp() {
        // Ensure test files don't exist before tests
        deleteTestFiles();
    }

    @AfterEach
    void tearDown() {
        // Clean up test files after tests
        deleteTestFiles();
    }

    private void deleteTestFiles() {
        Arrays.asList(TEST_CSV_FILE, TEST_OUTPUT_CSV_FILE)
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(Paths.get(file));
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                    }
                });
    }

    @Test
    void testWriteCSVWithMapping() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("FirstName")
                .addStringColumn("LastName")
                .addIntColumn("Age")
                .addStringColumn("Department")
                .addDoubleColumn("Salary")
                .setCreateDefaultValue(true)
                .addRow("FirstName", "John", "LastName", "Doe", "Age", "30", "Department", "IT", "Salary", "55000")
                .addRow("FirstName", "Jane", "LastName", "Smith", "Age", "28", "Department", "HR", "Salary", "48000")
                .addRow("FirstName", "Bob", "LastName", "Johnson", "Age", "35", "Department", "Finance", "Salary", "65000")
                .build();

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("FirstName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("LastName", "Surname", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Salary", "Income", "double"));
        
        // Write the table to a CSV file using the mapping configuration
        CSVProcessor.writeToCSV(sourceTable, config);
        
        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Read the content of the CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_CSV_FILE));
        assertEquals(4, lines.size(), "CSV should have 4 lines (header + 3 data rows)");
        
        // Check header row
        String headerLine = lines.get(0);
        assertTrue(headerLine.contains("Name") && headerLine.contains("Surname") && 
                  headerLine.contains("Years") && headerLine.contains("Income"), 
                  "Header line should contain mapped column names");
        
        // Check data rows
        assertTrue(lines.get(1).contains("John") && lines.get(1).contains("Doe") && 
                  lines.get(1).contains("30") && lines.get(1).contains("55000"), 
                  "First data row should contain mapped values");
    }

    @Test
    void testWriteCSVWithoutHeaderRow() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("Name")
                .addIntColumn("Age")
                .addStringColumn("Occupation")
                .setCreateDefaultValue(true)
                .addRow("Name", "Alice", "Age", "30", "Occupation", "Engineer")
                .addRow("Name", "Bob", "Age", "25", "Occupation", "Designer")
                .build();

        // Create a mapping configuration without header row
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", false)
                .addColumnMapping(new ColumnMapping("Name", "FirstName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));
        
        // Write the table to a CSV file using the mapping configuration
        CSVProcessor.writeToCSV(sourceTable, config);
        
        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Read the content of the CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_CSV_FILE));
        assertEquals(2, lines.size(), "CSV should have 2 lines (2 data rows, no header)");
        
        // Check data rows
        assertTrue(lines.get(0).contains("Alice") && lines.get(0).contains("30") && 
                  lines.get(0).contains("Engineer"), 
                  "First data row should contain original values");
    }

    @Test
    void testWriteCSVWithDefaultValues() throws Exception {
        // Create a table with some missing data
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("Name")
                .addStringColumn("Email")
                .addStringColumn("Phone")
                .setCreateDefaultValue(true)
                .addRow("Name", "Alice", "Email", "alice@example.com") // Missing phone
                .addRow("Name", "Bob", "Phone", "555-1234") // Missing email
                .build();

        // Create a mapping configuration with default values
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Email", "EmailAddress", "string").setDefaultValue("no-email@example.com"))
                .addColumnMapping(new ColumnMapping("Phone", "PhoneNumber", "string").setDefaultValue("000-0000"));
        
        // Write the table to a CSV file using the mapping configuration
        CSVProcessor.writeToCSV(sourceTable, config);
        
        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Read the content of the CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_CSV_FILE));
        assertEquals(3, lines.size(), "CSV should have 3 lines (header + 2 data rows)");
        
        // Check that default values were applied
        assertTrue(lines.get(1).contains("Alice") && lines.get(1).contains("alice@example.com") && 
                  lines.get(1).contains("000-0000"), 
                  "First data row should have default phone number");
        
        assertTrue(lines.get(2).contains("Bob") && lines.get(2).contains("no-email@example.com") && 
                  lines.get(2).contains("555-1234"), 
                  "Second data row should have default email");
    }

    @Test
    void testWriteAndReadWithMapping() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("ProductID")
                .addStringColumn("ProductName")
                .addDoubleColumn("Price")
                .addIntColumn("Quantity")
                .setCreateDefaultValue(true)
                .addRow("ProductID", "P001", "ProductName", "Laptop", "Price", "1200.50", "Quantity", "10")
                .addRow("ProductID", "P002", "ProductName", "Mouse", "Price", "25.99", "Quantity", "50")
                .addRow("ProductID", "P003", "ProductName", "Keyboard", "Price", "55.50", "Quantity", "30")
                .build();

        // Create a mapping configuration for writing
        MappingConfiguration writeConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("ProductID", "ID", "string"))
                .addColumnMapping(new ColumnMapping("ProductName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Price", "UnitPrice", "double"))
                .addColumnMapping(new ColumnMapping("Quantity", "Stock", "int"));
        
        // Write the table to a CSV file using the mapping configuration
        CSVProcessor.writeToCSV(sourceTable, writeConfig);
        
        // Create a mapping configuration for reading
        MappingConfiguration readConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("ID", "ProductCode", "string"))
                .addColumnMapping(new ColumnMapping("Name", "Item", "string"))
                .addColumnMapping(new ColumnMapping("UnitPrice", "Cost", "double"))
                .addColumnMapping(new ColumnMapping("Stock", "Available", "int"));
        
        // Read the CSV file using the mapping configuration
        TableCore targetTable = new TableCore();
        CSVProcessor.readFromCSV(targetTable, readConfig);
        
        // Validate the mapped table content
        assertEquals(3, targetTable.getRowCount(), "Mapped table should have 3 rows");
        assertEquals(4, targetTable.getColumnCount(), "Mapped table should have 4 columns");
        
        // Check the first row
        assertEquals("P001", targetTable.getValueAt(0, "ProductCode"), "First row, ProductCode should match");
        assertEquals("Laptop", targetTable.getValueAt(0, "Item"), "First row, Item should match");
        assertEquals("1200.5", targetTable.getValueAt(0, "Cost"), "First row, Cost should match");
        assertEquals("10", targetTable.getValueAt(0, "Available"), "First row, Available should match");
    }
}