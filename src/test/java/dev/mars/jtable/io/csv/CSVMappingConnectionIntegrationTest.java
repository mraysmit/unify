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
        Arrays.asList(TEST_CSV_FILE, TEST_MAPPED_CSV_FILE)
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
