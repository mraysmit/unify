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
 * Integration tests for CSV mapping functionality.
 * This class tests the integration between TableBuilder, CSVUtils, and MappingConfiguration.
 */
public class CSVMappingIntegrationTest {

    private static final String TEST_CSV_FILE = "test_data.csv";
    private static final String TEST_OUTPUT_CSV_FILE = "test_output.csv";
    private static final String TEST_MAPPED_CSV_FILE = "test_mapped.csv";

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
        Arrays.asList(TEST_CSV_FILE, TEST_OUTPUT_CSV_FILE, TEST_MAPPED_CSV_FILE)
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(Paths.get(file));
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                    }
                });
    }

    @Test
    void testWriteAndReadCSVWithoutMapping() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("Name")
                .addIntColumn("Age")
                .addStringColumn("Occupation")
                .setCreateDefaultValue(true)
                .addRow("Name", "Alice", "Age", "30", "Occupation", "Engineer")
                .addRow("Name", "Bob", "Age", "25", "Occupation", "Designer")
                .addRow("Name", "Charlie", "Age", "35", "Occupation", "Developer")
                .build();

        // Write the table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, TEST_CSV_FILE, true);

        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Read the content of the CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_CSV_FILE));
        assertEquals(4, lines.size(), "CSV should have 4 lines (header + 3 data rows)");
        assertEquals("Name,Age,Occupation", lines.get(0), "Header line should match");
        assertTrue(lines.get(1).startsWith("Alice,30,Engineer"), "First data row should match");
        
        // Read the CSV file back into a new table
        TableCore targetTable = new TableCore();
        CSVProcessor.readFromCSV(targetTable, TEST_CSV_FILE, true, false);
        
        // Validate the table content
        assertEquals(3, targetTable.getRowCount(), "Table should have 3 rows");
        assertEquals(3, targetTable.getColumnCount(), "Table should have 3 columns");
        assertEquals("Alice", targetTable.getValueAt(0, "Name"), "First row, Name column should match");
        assertEquals("30", targetTable.getValueAt(0, "Age"), "First row, Age column should match");
        assertEquals("Engineer", targetTable.getValueAt(0, "Occupation"), "First row, Occupation column should match");
    }

    @Test
    void testWriteAndReadCSVWithMapping() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("FirstName")
                .addIntColumn("Age")
                .addDoubleColumn("Salary")
                .setCreateDefaultValue(true)
                .addRow("FirstName", "Alice", "Age", "30", "Salary", "50000")
                .addRow("FirstName", "Bob", "Age", "25", "Salary", "60000")
                .addRow("FirstName", "Charlie", "Age", "35", "Salary", "70000")
                .build();

        // Write the table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, TEST_CSV_FILE, true);

        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("FirstName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Salary", "Income", "double"));
        
        // Read the CSV file using the mapping configuration
        TableCore mappedTable = new TableCore();
        CSVProcessor.readFromCSV(mappedTable, config);
        
        // Validate the mapped table content
        assertEquals(3, mappedTable.getRowCount(), "Mapped table should have 3 rows");
        assertEquals(3, mappedTable.getColumnCount(), "Mapped table should have 3 columns");
        assertEquals("Alice", mappedTable.getValueAt(0, "Name"), "First row, Name column should match");
        assertEquals("30", mappedTable.getValueAt(0, "Age"), "First row, Age column should match");
        assertEquals("50000.0", mappedTable.getValueAt(0, "Income"), "First row, Income column should match");
        
        // Write the mapped table to a new CSV file
        CSVProcessor.writeToCSV(mappedTable, TEST_MAPPED_CSV_FILE, true);
        
        // Verify the mapped CSV file was created
        File mappedCsvFile = new File(TEST_MAPPED_CSV_FILE);
        assertTrue(mappedCsvFile.exists(), "Mapped CSV file should exist");
        
        // Read the content of the mapped CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_MAPPED_CSV_FILE));
        assertEquals(4, lines.size(), "Mapped CSV should have 4 lines (header + 3 data rows)");
        assertEquals("Name,Age,Income", lines.get(0), "Header line should match");
        assertTrue(lines.get(1).startsWith("Alice,30,50000"), "First data row should match");
    }

    @Test
    void testComplexMappingWithColumnReordering() throws Exception {
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

        // Write the table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, TEST_CSV_FILE, true);
        
        // Create a mapping configuration with column reordering and renaming
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                // Combine FirstName and LastName into FullName
                .addColumnMapping(new ColumnMapping("FirstName", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("LastName", "LastNameOnly", "string"))
                // Keep Age as is
                .addColumnMapping(new ColumnMapping("Age", "EmployeeAge", "int"))
                // Rename Department
                .addColumnMapping(new ColumnMapping("Department", "Division", "string"))
                // Convert Salary to Income
                .addColumnMapping(new ColumnMapping("Salary", "AnnualIncome", "double"));
        
        // Read the CSV file using the mapping configuration
        TableCore mappedTable = new TableCore();
        CSVProcessor.readFromCSV(mappedTable, config);
        
        // Validate the mapped table content
        assertEquals(3, mappedTable.getRowCount(), "Mapped table should have 3 rows");
        assertEquals(5, mappedTable.getColumnCount(), "Mapped table should have 5 columns");
        
        // Check the first row
        assertEquals("John", mappedTable.getValueAt(0, "FullName"), "First row, FullName column should match");
        assertEquals("Doe", mappedTable.getValueAt(0, "LastNameOnly"), "First row, LastNameOnly column should match");
        assertEquals("30", mappedTable.getValueAt(0, "EmployeeAge"), "First row, EmployeeAge column should match");
        assertEquals("IT", mappedTable.getValueAt(0, "Division"), "First row, Division column should match");
        assertEquals("55000.0", mappedTable.getValueAt(0, "AnnualIncome"), "First row, AnnualIncome column should match");
        
        // Write the mapped table to a new CSV file
        CSVProcessor.writeToCSV(mappedTable, TEST_MAPPED_CSV_FILE, true);
        
        // Read the content of the mapped CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_MAPPED_CSV_FILE));
        assertEquals(4, lines.size(), "Mapped CSV should have 4 lines (header + 3 data rows)");
        assertTrue(lines.get(0).contains("FullName") && lines.get(0).contains("EmployeeAge") && 
                  lines.get(0).contains("AnnualIncome"), "Header line should contain mapped column names");
    }

    @Test
    void testMappingWithSourceColumnIndices() throws Exception {
        // Create a table with numeric data
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("Product")
                .addDoubleColumn("Price")
                .addIntColumn("Quantity")
                .addDoubleColumn("Total")
                .setCreateDefaultValue(true)
                .addRow("Product", "Laptop", "Price", "1200.50", "Quantity", "2", "Total", "2401.00")
                .addRow("Product", "Mouse", "Price", "25.99", "Quantity", "5", "Total", "129.95")
                .addRow("Product", "Keyboard", "Price", "55.50", "Quantity", "3", "Total", "166.50")
                .build();

        // Write the table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, TEST_CSV_FILE, true);
        
        // Create a mapping configuration using column indices instead of names
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping(0, "Item", "string"))
                .addColumnMapping(new ColumnMapping(1, "UnitPrice", "double"))
                .addColumnMapping(new ColumnMapping(2, "Count", "int"))
                .addColumnMapping(new ColumnMapping(3, "SubTotal", "double"));
        
        // Read the CSV file using the mapping configuration
        TableCore mappedTable = new TableCore();
        CSVProcessor.readFromCSV(mappedTable, config);
        
        // Validate the mapped table content
        assertEquals(3, mappedTable.getRowCount(), "Mapped table should have 3 rows");
        assertEquals(4, mappedTable.getColumnCount(), "Mapped table should have 4 columns");
        
        // Check the first row
        assertEquals("Laptop", mappedTable.getValueAt(0, "Item"), "First row, Item column should match");
        assertEquals("1200.5", mappedTable.getValueAt(0, "UnitPrice"), "First row, UnitPrice column should match");
        assertEquals("2", mappedTable.getValueAt(0, "Count"), "First row, Count column should match");
        assertEquals("2401.0", mappedTable.getValueAt(0, "SubTotal"), "First row, SubTotal column should match");
        
        // Write the mapped table to a new CSV file
        CSVProcessor.writeToCSV(mappedTable, TEST_MAPPED_CSV_FILE, true);
        
        // Read the content of the mapped CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_MAPPED_CSV_FILE));
        assertEquals(4, lines.size(), "Mapped CSV should have 4 lines (header + 3 data rows)");
        assertEquals("Item,UnitPrice,Count,SubTotal", lines.get(0), "Header line should match");
    }

    @Test
    void testMappingWithDefaultValues() throws Exception {
        // Create a table with some missing data
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("Name")
                .addStringColumn("Email")
                .addStringColumn("Phone")
                .setCreateDefaultValue(true)
                .addRow("Name", "Alice", "Email", "alice@example.com", "Phone", "555-1234")
                .addRow("Name", "Bob", "Email", "bob@example.com") // Missing phone
                .addRow("Name", "Charlie", "Phone", "555-5678") // Missing email
                .build();

        // Write the table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, TEST_CSV_FILE, true);
        
        // Create a mapping configuration with default values
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Email", "EmailAddress", "string").setDefaultValue("no-email@example.com"))
                .addColumnMapping(new ColumnMapping("Phone", "PhoneNumber", "string").setDefaultValue("000-0000"));
        
        // Read the CSV file using the mapping configuration
        TableCore mappedTable = new TableCore();
        CSVProcessor.readFromCSV(mappedTable, config);
        
        // Validate the mapped table content
        assertEquals(3, mappedTable.getRowCount(), "Mapped table should have 3 rows");
        
        // Check that default values were applied
        assertEquals("Bob", mappedTable.getValueAt(1, "FullName"), "Second row, FullName should match");
        assertEquals("bob@example.com", mappedTable.getValueAt(1, "EmailAddress"), "Second row, EmailAddress should match");
        assertEquals("000-0000", mappedTable.getValueAt(1, "PhoneNumber"), "Second row, PhoneNumber should have default value");
        
        assertEquals("Charlie", mappedTable.getValueAt(2, "FullName"), "Third row, FullName should match");
        assertEquals("no-email@example.com", mappedTable.getValueAt(2, "EmailAddress"), "Third row, EmailAddress should have default value");
        assertEquals("555-5678", mappedTable.getValueAt(2, "PhoneNumber"), "Third row, PhoneNumber should match");
    }
}