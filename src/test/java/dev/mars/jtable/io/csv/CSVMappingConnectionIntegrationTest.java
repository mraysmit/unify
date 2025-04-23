package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.file.FileConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for CSV mapping functionality with FileConnection and DataSourceConnectionFactory.
 * This class tests the integration between CSVMappingReader, CSVMappingWriter, FileConnection, and DataSourceConnectionFactory.
 */
public class CSVMappingConnectionIntegrationTest {

    private static final String TEST_CSV_FILE = "test_connection_data.csv";
    private static final String TEST_MAPPED_CSV_FILE = "test_connection_mapped.csv";
    private static final String NON_EXISTENT_FILE = "non_existent_file.csv";
    private static final String INVALID_COLUMNS_FILE = "invalid_columns_file.csv";

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
        Arrays.asList(TEST_CSV_FILE, TEST_MAPPED_CSV_FILE, INVALID_COLUMNS_FILE)
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(Paths.get(file));
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                    }
                });
    }

    @Test
    void testCSVMappingReaderWithFileConnection() throws Exception {
        // Create a test CSV file
        String csvContent = "Name,Age,Occupation\nAlice,30,Engineer\nBob,25,Designer\n";
        Files.write(Paths.get(TEST_CSV_FILE), csvContent.getBytes());

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(connection.connect(), "Connection should be established");

        // Modify the config to use the connection's location
        config.setSourceLocation(connection.getLocation());

        // Read the CSV file using CSVMappingReader
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(table, config);

        // Verify the table content
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns");
        assertEquals("Alice", table.getValueAt(0, "FullName"), "First row, FullName column should match");
        assertEquals("30", table.getValueAt(0, "Years"), "First row, Years column should match");
        assertEquals("Engineer", table.getValueAt(0, "Job"), "First row, Job column should match");

        // Disconnect
        connection.disconnect();
        assertFalse(connection.isConnected(), "Connection should be disconnected");
    }

    @Test
    void testCSVMappingWriterWithFileConnection() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("FirstName")
                .addIntColumn("Age")
                .addStringColumn("Occupation")
                .setCreateDefaultValue(true)
                .addRow("FirstName", "Alice", "Age", "30", "Occupation", "Engineer")
                .addRow("FirstName", "Bob", "Age", "25", "Occupation", "Designer")
                .build();

        // Create an empty file first so the connection can be established
        Files.createFile(Paths.get(TEST_CSV_FILE));

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(connection.connect(), "Connection should be established");

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(connection.getLocation())
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("FirstName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Write the table to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(sourceTable, config);

        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        // Read the content of the CSV file to validate it
        List<String> lines = Files.readAllLines(Paths.get(TEST_CSV_FILE));
        assertEquals(3, lines.size(), "CSV should have 3 lines (header + 2 data rows)");
        assertEquals("Name,Years,Job", lines.get(0), "Header line should match");
        assertEquals("Alice,30,Engineer", lines.get(1), "First data row should match");
        assertEquals("Bob,25,Designer", lines.get(2), "Second data row should match");

        // Disconnect
        connection.disconnect();
        assertFalse(connection.isConnected(), "Connection should be disconnected");
    }

    @Test
    void testCSVProcessorWithMappingAndFileConnection() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("FirstName")
                .addIntColumn("Age")
                .addStringColumn("Occupation")
                .setCreateDefaultValue(true)
                .addRow("FirstName", "Alice", "Age", "30", "Occupation", "Engineer")
                .addRow("FirstName", "Bob", "Age", "25", "Occupation", "Designer")
                .build();

        // Create an empty file first so the connection can be established
        Files.createFile(Paths.get(TEST_CSV_FILE));

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(connection.connect(), "Connection should be established");

        // Create a mapping configuration for writing
        MappingConfiguration writeConfig = new MappingConfiguration()
                .setSourceLocation(connection.getLocation())
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("FirstName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Write the table to a CSV file using CSVProcessor
        CSVProcessor.writeToCSV(sourceTable, writeConfig);

        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        // Create a mapping configuration for reading
        MappingConfiguration readConfig = new MappingConfiguration()
                .setSourceLocation(connection.getLocation())
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Years", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Job", "Occupation", "string"));

        // Read the CSV file using CSVProcessor
        TableCore targetTable = new TableCore();
        CSVProcessor.readFromCSV(targetTable, readConfig);

        // Verify the table content
        assertEquals(2, targetTable.getRowCount(), "Table should have 2 rows");
        assertEquals(3, targetTable.getColumnCount(), "Table should have 3 columns");
        assertEquals("Alice", targetTable.getValueAt(0, "FullName"), "First row, FullName column should match");
        assertEquals("30", targetTable.getValueAt(0, "Age"), "First row, Age column should match");
        assertEquals("Engineer", targetTable.getValueAt(0, "Occupation"), "First row, Occupation column should match");

        // Disconnect
        connection.disconnect();
        assertFalse(connection.isConnected(), "Connection should be disconnected");
    }

    @Test
    void testCSVMappingReaderWithColumnNameNotFound() throws Exception {
        // Create a test CSV file with specific columns
        String csvContent = "Name,Age,City\nAlice,30,New York\nBob,25,San Francisco\n";
        Files.write(Paths.get(INVALID_COLUMNS_FILE), csvContent.getBytes());

        // Create a mapping configuration with a column name that doesn't exist in the file
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(INVALID_COLUMNS_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string").setDefaultValue("Unknown")); // This column doesn't exist

        // Try to read the file
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(table, config);

        // Verify that the table has the expected rows and columns
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns");
        assertEquals("Alice", table.getValueAt(0, "FullName"), "First row, FullName column should match");
        assertEquals("30", table.getValueAt(0, "Years"), "First row, Years column should match");
        assertEquals("Unknown", table.getValueAt(0, "Job"), "First row, Job column should use default value");
        assertEquals("Bob", table.getValueAt(1, "FullName"), "Second row, FullName column should match");
        assertEquals("25", table.getValueAt(1, "Years"), "Second row, Years column should match");
        assertEquals("Unknown", table.getValueAt(1, "Job"), "Second row, Job column should use default value");
    }

    @Test
    void testCSVMappingReaderWithDifferentNumberOfColumns() throws Exception {
        // Create a test CSV file with fewer columns than expected
        String csvContent = "ID,Name\n1,Alice\n2,Bob\n";
        Files.write(Paths.get(INVALID_COLUMNS_FILE), csvContent.getBytes());

        // Create a mapping configuration with more columns than the file has
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(INVALID_COLUMNS_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("ID", "UserID", "string"))
                .addColumnMapping(new ColumnMapping("Name", "UserName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "UserAge", "int").setDefaultValue("0"))
                .addColumnMapping(new ColumnMapping("Department", "UserDept", "string").setDefaultValue("Unknown"));

        // Try to read the file
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(table, config);

        // Verify that the table has the expected rows and columns
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(4, table.getColumnCount(), "Table should have 4 columns");
        assertEquals("1", table.getValueAt(0, "UserID"), "First row, UserID column should match");
        assertEquals("Alice", table.getValueAt(0, "UserName"), "First row, UserName column should match");
        assertEquals("0", table.getValueAt(0, "UserAge"), "First row, UserAge column should use default value");
        assertEquals("Unknown", table.getValueAt(0, "UserDept"), "First row, UserDept column should use default value");
    }

    @Test
    void testCSVMappingReaderWithEmptySourceFile() throws Exception {
        // Create an empty CSV file with just a header
        String csvContent = "Name,Age,Occupation\n";
        Files.write(Paths.get(INVALID_COLUMNS_FILE), csvContent.getBytes());

        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(INVALID_COLUMNS_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Try to read the file
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(table, config);

        // Verify that the table is empty (no rows added)
        assertEquals(0, table.getRowCount(), "Table should have 0 rows when reading from an empty source file");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns based on the mapping");
    }

    @Test
    void testCSVMappingReaderWithNoHeaderWhenExpected() throws Exception {
        // Create a CSV file with no header row, just data
        String csvContent = "Alice,30,Engineer\nBob,25,Designer\n";
        Files.write(Paths.get(INVALID_COLUMNS_FILE), csvContent.getBytes());

        // Create a mapping configuration that expects a header row
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(INVALID_COLUMNS_FILE)
                .setOption("hasHeaderRow", true) // Expecting a header, but there isn't one
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Try to read the file
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(table, config);

        // Verify the behavior - the first row of data will be treated as the header
        // So we should only have one row of data (the second row in the file)
        assertEquals(1, table.getRowCount(), "Table should have 1 row when first row is treated as header");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns based on the mapping");

        // The first row of data (Alice,30,Engineer) is treated as the header
        // So the mapping will try to find columns named "Alice", "30", "Engineer"
        // Since these don't match our mapping ("Name", "Age", "Occupation"), we'll get null values or default values

        // The second row of data (Bob,25,Designer) will be the only data row
        // But since the column names don't match, we'll get null values or default values
        // We can't make specific assertions about the values because the behavior depends on how findColumnIndex works
    }

    @Test
    void testCSVMappingReaderWithNonExistentFile() {
        // Create a mapping configuration with a non-existent file
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(NON_EXISTENT_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Years", "int"))
                .addColumnMapping(new ColumnMapping("Occupation", "Job", "string"));

        // Try to read the non-existent file
        TableCore table = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();

        try {
            // This should throw a FileNotFoundException
            reader.readFromCSV(table, config);
            fail("Expected FileNotFoundException was not thrown");
        } catch (FileNotFoundException e) {
            // Expected exception, verify the error message
            assertTrue(e.getMessage().contains("does not exist"), 
                "Error message should indicate file does not exist");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }

        // Verify that the table is empty (no rows added)
        assertEquals(0, table.getRowCount(), "Table should have 0 rows when reading from a non-existent file");
    }

    @Test
    void testDataSourceConnectionFactoryWithCSVMapping() throws Exception {
        // Create a table using TableBuilder
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("ID")
                .addStringColumn("Name")
                .addIntColumn("Age")
                .setCreateDefaultValue(true)
                .addRow("ID", "1", "Name", "Alice", "Age", "30")
                .addRow("ID", "2", "Name", "Bob", "Age", "25")
                .build();

        // Create an empty file first so the connection can be established
        Files.createFile(Paths.get(TEST_CSV_FILE));

        // Create a connection using DataSourceConnectionFactory
        FileConnection connection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertEquals("csv", connection.getConnectionType(), "Connection type should be csv");
        assertTrue(connection.connect(), "Connection should be established");

        // Create a mapping configuration for writing
        MappingConfiguration writeConfig = new MappingConfiguration()
                .setSourceLocation(connection.getLocation())
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("ID", "UserID", "string"))
                .addColumnMapping(new ColumnMapping("Name", "UserName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "UserAge", "int"));

        // Write the table to a CSV file
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(sourceTable, writeConfig);

        // Verify the CSV file was created
        File csvFile = new File(TEST_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        // Create a new connection for reading
        FileConnection readConnection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(readConnection.connect(), "Read connection should be established");

        // Create a new mapping configuration for reading
        MappingConfiguration readConfig = new MappingConfiguration()
                .setSourceLocation(readConnection.getLocation())
                .setOption("hasHeaderRow", true)
                .addColumnMapping(new ColumnMapping("UserID", "UserID", "string"))
                .addColumnMapping(new ColumnMapping("UserName", "UserName", "string"))
                .addColumnMapping(new ColumnMapping("UserAge", "UserAge", "int"));

        // Read the CSV file
        TableCore targetTable = new TableCore();
        CSVMappingReader reader = new CSVMappingReader();
        reader.readFromCSV(targetTable, readConfig);

        // Verify the table content
        assertEquals(2, targetTable.getRowCount(), "Table should have 2 rows");
        assertEquals(3, targetTable.getColumnCount(), "Table should have 3 columns");
        assertEquals("1", targetTable.getValueAt(0, "UserID"), "First row, UserID column should match");
        assertEquals("Alice", targetTable.getValueAt(0, "UserName"), "First row, UserName column should match");
        assertEquals("30", targetTable.getValueAt(0, "UserAge"), "First row, UserAge column should match");

        // Disconnect both connections
        connection.disconnect();
        readConnection.disconnect();
        assertFalse(connection.isConnected(), "Connection should be disconnected");
        assertFalse(readConnection.isConnected(), "Read connection should be disconnected");
    }
}
