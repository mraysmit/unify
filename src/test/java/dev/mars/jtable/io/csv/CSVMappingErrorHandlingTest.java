package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling in CSV mapping classes.
 * This class demonstrates best practices for validating error and warning messages
 * in the MappingConfiguration, CSVMappingReader, and CSVMappingWriter classes.
 */
public class CSVMappingErrorHandlingTest {

    private static final String TEST_CSV_FILE = "test_error_handling.csv";
    private static final String NON_EXISTENT_FILE = "non_existent_file.csv";
    private static final String INVALID_COLUMNS_FILE = "invalid_columns_file.csv";
    
    // For capturing System.err output
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Redirect System.err to capture output
        System.setErr(new PrintStream(errContent));
        
        // Ensure test files don't exist before tests
        deleteTestFiles();
    }

    @AfterEach
    void tearDown() {
        // Restore original System.err
        System.setErr(originalErr);
        
        // Clean up test files after tests
        deleteTestFiles();
    }

    private void deleteTestFiles() {
        Arrays.asList(TEST_CSV_FILE, INVALID_COLUMNS_FILE)
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(Paths.get(file));
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                    }
                });
    }

    /**
     * Test that validates error messages when CSVMappingReader is given a null table.
     */
    @Test
    void testCSVMappingReaderWithNullTable() {
        // Create a valid mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"));
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        
        // Try to read with a null table
        try {
            reader.readFromCSV(null, config);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Table cannot be null", e.getMessage(), 
                "Error message should indicate table cannot be null");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Table cannot be null"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates error messages when CSVMappingReader is given a null configuration.
     */
    @Test
    void testCSVMappingReaderWithNullConfig() {
        // Create a valid table
        TableCore table = new TableCore();
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        
        // Try to read with a null configuration
        try {
            reader.readFromCSV(table, null);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Mapping configuration cannot be null", e.getMessage(), 
                "Error message should indicate configuration cannot be null");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Mapping configuration cannot be null"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates error messages when CSVMappingReader is given a configuration with no source location.
     */
    @Test
    void testCSVMappingReaderWithNoSourceLocation() {
        // Create a table
        TableCore table = new TableCore();
        
        // Create a mapping configuration with no source location
        MappingConfiguration config = new MappingConfiguration()
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"));
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        
        // Try to read with a configuration that has no source location
        try {
            reader.readFromCSV(table, config);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Source location in mapping configuration cannot be null or empty", e.getMessage(), 
                "Error message should indicate source location cannot be null or empty");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Source location in mapping configuration cannot be null or empty"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates error messages when CSVMappingReader is given a configuration with no column mappings.
     */
    @Test
    void testCSVMappingReaderWithNoColumnMappings() {
        // Create a table
        TableCore table = new TableCore();
        
        // Create a mapping configuration with no column mappings
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true);
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        
        // Try to read with a configuration that has no column mappings
        try {
            reader.readFromCSV(table, config);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Column mappings cannot be null or empty", e.getMessage(), 
                "Error message should indicate column mappings cannot be null or empty");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Column mappings cannot be null or empty"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates error messages when CSVMappingReader is given a non-existent file.
     */
    @Test
    void testCSVMappingReaderWithNonExistentFile() {
        // Create a table
        TableCore table = new TableCore();
        
        // Create a mapping configuration with a non-existent file
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(NON_EXISTENT_FILE)
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"));
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        
        // Try to read a non-existent file
        try {
            reader.readFromCSV(table, config);
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException e) {
            // Expected exception
            assertTrue(e.getMessage().contains("does not exist"), 
                "Error message should indicate file does not exist");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("CSV file does not exist"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates warning messages when CSVMappingReader encounters a column name not found in the CSV file.
     */
    @Test
    void testCSVMappingReaderWithColumnNameNotFound() throws Exception {
        // Create a test CSV file with specific columns
        String csvContent = "Name,Age,City\nAlice,30,New York\nBob,25,San Francisco\n";
        Files.write(Paths.get(TEST_CSV_FILE), csvContent.getBytes());
        
        // Create a mapping configuration with a column name that doesn't exist in the file
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string").setDefaultValue("Unknown")); // This column doesn't exist
        
        // Create a CSVMappingReader
        CSVMappingReader reader = new CSVMappingReader();
        TableCore table = new TableCore();
        
        // Read the file
        reader.readFromCSV(table, config);
        
        // Verify that the warning message was logged to System.err
        assertTrue(errContent.toString().contains("Warning: Column 'Occupation' not found in CSV file"), 
            "Warning about column not found should be logged to System.err");
        
        // Verify that the default value was used
        assertEquals("Unknown", table.getValueAt(0, "Job"), "Default value should be used for missing column");
    }

    /**
     * Test that validates error messages when CSVMappingWriter is given a null table.
     */
    @Test
    void testCSVMappingWriterWithNullTable() {
        // Create a valid mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"));
        
        // Create a CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        
        // Try to write with a null table
        try {
            writer.writeToCSV(null, config);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Table cannot be null", e.getMessage(), 
                "Error message should indicate table cannot be null");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Table cannot be null"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates error messages when CSVMappingWriter is given a null configuration.
     */
    @Test
    void testCSVMappingWriterWithNullConfig() {
        // Create a valid table
        TableBuilder builder = new TableBuilder();
        ITable table = builder
                .addStringColumn("Name")
                .addIntColumn("Age")
                .addRow("Name", "Alice", "Age", "30")
                .build();
        
        // Create a CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        
        // Try to write with a null configuration
        try {
            writer.writeToCSV(table, null);
            fail("Expected IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            // Expected exception
            assertEquals("Mapping configuration cannot be null", e.getMessage(), 
                "Error message should indicate configuration cannot be null");
            
            // Verify the error was also logged to System.err
            assertTrue(errContent.toString().contains("Mapping configuration cannot be null"), 
                "Error should be logged to System.err");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Test that validates warning messages when CSVMappingWriter encounters a source column name not found in the table.
     */
    @Test
    void testCSVMappingWriterWithSourceColumnNameNotFound() throws Exception {
        // Create a table with specific columns
        TableBuilder builder = new TableBuilder();
        ITable table = builder
                .addStringColumn("Name")
                .addIntColumn("Age")
                .addRow("Name", "Alice", "Age", "30")
                .build();
        
        // Create a mapping configuration with a source column name that doesn't exist in the table
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string").setDefaultValue("Unknown")); // This column doesn't exist
        
        // Create a CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        
        // Write the table
        writer.writeToCSV(table, config);
        
        // Verify that the warning message was logged to System.err
        assertTrue(errContent.toString().contains("Warning: Source column name is null or empty") || 
                   errContent.toString().contains("Warning: Error getting value for column 'Occupation'"), 
            "Warning about source column not found should be logged to System.err");
        
        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");
        
        // Read the content of the CSV file to validate it
        String fileContent = new String(Files.readAllBytes(Paths.get(TEST_CSV_FILE)));
        assertTrue(fileContent.contains("FullName") && fileContent.contains("Years") && fileContent.contains("Job"), 
            "CSV should contain all mapped column names in header");
        assertTrue(fileContent.contains("Alice") && fileContent.contains("30") && fileContent.contains("Unknown"), 
            "CSV should contain values and default value for missing column");
    }
}