package dev.mars.jtable.io.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.csv.CSVMappingReader;
import dev.mars.jtable.io.datasource.DataSourceConnectionFactory;
import dev.mars.jtable.io.file.FileConnection;
import dev.mars.jtable.io.jdbc.JDBCConnection;
import dev.mars.jtable.io.jdbc.JDBCMappingWriter;
import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that demonstrate reading data from CSV files using mapping configurations
 * and writing that data to an H2 database using custom mapping configurations.
 */
public class CSVToJDBCIntegrationTest {

    private static final String TEST_CSV_FILE = "test_csv_to_jdbc.csv";
    private static final String H2_CONNECTION_STRING = "jdbc:h2:mem:csvtojdbctest;DB_CLOSE_DELAY=-1";
    private static final String H2_USERNAME = "";
    private static final String H2_PASSWORD = "";
    private static final String TEST_TABLE_NAME_PREFIX = "employee_data";

    private String testTableName;
    private JDBCConnection jdbcConnection;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure test files don't exist before tests
        deleteTestFiles();

        // Create a JDBC connection
        jdbcConnection = new JDBCConnection(
            H2_CONNECTION_STRING,
            H2_USERNAME,
            H2_PASSWORD
        );
        jdbcConnection.connect();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test files after tests
        deleteTestFiles();

        // Clean up database
        if (jdbcConnection != null && jdbcConnection.isConnected()) {
            try (Connection connection = (Connection) jdbcConnection.getRawConnection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP TABLE IF EXISTS " + testTableName);
            } catch (Exception e) {
                System.err.println("Error dropping table: " + e.getMessage());
            }
            jdbcConnection.disconnect();
        }
    }

    private void deleteTestFiles() {
        try {
            Files.deleteIfExists(Paths.get(TEST_CSV_FILE));
        } catch (Exception e) {
            System.err.println("Error deleting file: " + TEST_CSV_FILE + " - " + e.getMessage());
        }
    }

    @Test
    void testReadFromCSVAndWriteToH2Database() throws Exception {
        // Generate a unique table name for this test
        testTableName = TEST_TABLE_NAME_PREFIX + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Create a test CSV file with employee data
        String csvContent = "Name,Age,Department,Salary,IsActive\n" +
                           "John Doe,35,Engineering,85000.50,true\n" +
                           "Jane Smith,28,Marketing,65000.75,true\n" +
                           "Bob Johnson,42,Finance,95000.25,true\n" +
                           "Alice Brown,31,HR,72000.00,true\n" +
                           "Charlie Wilson,24,Intern,45000.00,false\n";
        Files.write(Paths.get(TEST_CSV_FILE), csvContent.getBytes());

        // Create a mapping configuration for reading from CSV
        MappingConfiguration csvReadConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "EmployeeName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "EmployeeAge", "int"))
                .addColumnMapping(new ColumnMapping("Department", "Division", "string"))
                .addColumnMapping(new ColumnMapping("Salary", "AnnualSalary", "double"))
                .addColumnMapping(new ColumnMapping("IsActive", "IsCurrentEmployee", "boolean"));

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection fileConnection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(fileConnection.connect(), "File connection should be established");

        // Read the CSV file using CSVMappingReader
        TableCore table = new TableCore();
        CSVMappingReader csvReader = new CSVMappingReader();
        csvReader.readFromCSV(table, csvReadConfig);

        // Verify the table content from CSV
        assertEquals(5, table.getRowCount(), "Table should have 5 rows");
        assertEquals(5, table.getColumnCount(), "Table should have 5 columns");
        assertEquals("John Doe", table.getValueAt(0, "EmployeeName"), "First row, EmployeeName column should match");
        assertEquals("35", table.getValueAt(0, "EmployeeAge"), "First row, EmployeeAge column should match");
        assertEquals("Engineering", table.getValueAt(0, "Division"), "First row, Division column should match");

        // Create a mapping configuration for writing to H2 database
        MappingConfiguration jdbcWriteConfig = new MappingConfiguration()
                .setSourceLocation(H2_CONNECTION_STRING)
                .setOption("username", H2_USERNAME)
                .setOption("password", H2_PASSWORD)
                .setOption("tableName", testTableName)
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("EmployeeName", "FULL_NAME", "string"))
                .addColumnMapping(new ColumnMapping("EmployeeAge", "AGE", "int"))
                .addColumnMapping(new ColumnMapping("Division", "DEPARTMENT", "string"))
                .addColumnMapping(new ColumnMapping("AnnualSalary", "SALARY", "double"))
                .addColumnMapping(new ColumnMapping("IsCurrentEmployee", "IS_ACTIVE", "boolean"));

        // Write the table to H2 database using JDBCMappingWriter
        JDBCMappingWriter jdbcWriter = new JDBCMappingWriter();
        jdbcWriter.writeToDatabase(table, jdbcWriteConfig);

        // Verify the data was written correctly to the database
        // Create a new connection for verification since JDBCMappingWriter closes its connection
        try (Connection verifyConnection = DriverManager.getConnection(H2_CONNECTION_STRING, H2_USERNAME, H2_PASSWORD);
             Statement statement = verifyConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + testTableName)) {
            assertTrue(resultSet.next(), "Result set should have at least one row");
            assertEquals(5, resultSet.getInt(1), "Database table should have 5 rows");
        }

        // Verify specific data in the database
        try (Connection verifyConnection = DriverManager.getConnection(H2_CONNECTION_STRING, H2_USERNAME, H2_PASSWORD);
             Statement statement = verifyConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + testTableName + " WHERE FULL_NAME = 'John Doe'")) {
            assertTrue(resultSet.next(), "Should find John Doe in the database");
            assertEquals(35, resultSet.getInt("AGE"), "Age should match");
            assertEquals("Engineering", resultSet.getString("DEPARTMENT"), "Department should match");
            assertEquals(85000.50, resultSet.getDouble("SALARY"), 0.01, "Salary should match");
            assertTrue(resultSet.getBoolean("IS_ACTIVE"), "IsActive should match");
        }

        // Disconnect file connection
        fileConnection.disconnect();
    }

    @Test
    void testReadFromCSVWithMissingColumnsAndWriteToH2Database() throws Exception {
        // Generate a unique table name for this test
        testTableName = TEST_TABLE_NAME_PREFIX + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // Create a test CSV file with some columns missing
        // Note: We're intentionally not including Salary and IsActive columns to test default values
        String csvContent = "Name,Age,Department\n" +
                           "John Doe,35,Engineering\n" +
                           "Jane Smith,28,Marketing\n" +
                           "Bob Johnson,42,Finance\n";
        Files.write(Paths.get(TEST_CSV_FILE), csvContent.getBytes());

        // Create a mapping configuration for reading from CSV with default values for missing columns
        MappingConfiguration csvReadConfig = new MappingConfiguration()
                .setSourceLocation(TEST_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", true)
                .addColumnMapping(new ColumnMapping("Name", "EmployeeName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "EmployeeAge", "int"))
                .addColumnMapping(new ColumnMapping("Department", "Division", "string"))
                // These columns don't exist in the CSV, but we provide default values
                .addColumnMapping(new ColumnMapping("AnnualSalary", "AnnualSalary", "double").setDefaultValue("50000.00"))
                .addColumnMapping(new ColumnMapping("IsCurrentEmployee", "IsCurrentEmployee", "boolean").setDefaultValue("true"));

        // Create a FileConnection using DataSourceConnectionFactory
        FileConnection fileConnection = (FileConnection) DataSourceConnectionFactory.createConnection(TEST_CSV_FILE);
        assertTrue(fileConnection.connect(), "File connection should be established");

        // Create table and reader
        TableCore table = new TableCore();
        CSVMappingReader csvReader = new CSVMappingReader();

        // Capture System.err to check for warnings
        PrintStream originalErr = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        try {
            // Read the CSV file using CSVMappingReader
            csvReader.readFromCSV(table, csvReadConfig);

            // Check if there are any warnings about columns not found
            String errOutput = errContent.toString();

            // Verify that we don't get warnings for our properly configured columns
            assertFalse(errOutput.contains("Warning: Column 'Salary' not found in headers"), 
                "Should not warn about Salary column not found");

            // Verify that we don't get warnings for our properly configured columns
            assertFalse(errOutput.contains("Warning: Column 'IsActive' not found in headers"), 
                "Should not warn about IsActive column not found");
        } finally {
            // Restore original System.err
            System.setErr(originalErr);
        }

        // Verify the table content from CSV with default values
        assertEquals(3, table.getRowCount(), "Table should have 3 rows");
        assertEquals(5, table.getColumnCount(), "Table should have 5 columns");
        assertEquals("John Doe", table.getValueAt(0, "EmployeeName"), "First row, EmployeeName column should match");
        assertEquals("35", table.getValueAt(0, "EmployeeAge"), "First row, EmployeeAge column should match");
        assertEquals("Engineering", table.getValueAt(0, "Division"), "First row, Division column should match");
        assertEquals("50000.00", table.getValueAt(0, "AnnualSalary"), "First row, AnnualSalary column should use default value");
        assertEquals("true", table.getValueAt(0, "IsCurrentEmployee"), "First row, IsCurrentEmployee column should use default value");

        // Create a mapping configuration for writing to H2 database
        MappingConfiguration jdbcWriteConfig = new MappingConfiguration()
                .setSourceLocation(H2_CONNECTION_STRING)
                .setOption("username", H2_USERNAME)
                .setOption("password", H2_PASSWORD)
                .setOption("tableName", testTableName)
                .setOption("createTable", true)
                .addColumnMapping(new ColumnMapping("EmployeeName", "FULL_NAME", "string"))
                .addColumnMapping(new ColumnMapping("EmployeeAge", "AGE", "int"))
                .addColumnMapping(new ColumnMapping("Division", "DEPARTMENT", "string"))
                .addColumnMapping(new ColumnMapping("AnnualSalary", "SALARY", "double"))
                .addColumnMapping(new ColumnMapping("IsCurrentEmployee", "IS_ACTIVE", "boolean"));

        // Write the table to H2 database using JDBCMappingWriter
        JDBCMappingWriter jdbcWriter = new JDBCMappingWriter();
        jdbcWriter.writeToDatabase(table, jdbcWriteConfig);

        // Verify the data was written correctly to the database
        // Create a new connection for verification since JDBCMappingWriter closes its connection
        try (Connection verifyConnection = DriverManager.getConnection(H2_CONNECTION_STRING, H2_USERNAME, H2_PASSWORD);
             Statement statement = verifyConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + testTableName)) {
            assertTrue(resultSet.next(), "Result set should have at least one row");
            assertEquals(3, resultSet.getInt(1), "Database table should have 3 rows");
        }

        // Verify specific data in the database including default values
        try (Connection verifyConnection = DriverManager.getConnection(H2_CONNECTION_STRING, H2_USERNAME, H2_PASSWORD);
             Statement statement = verifyConnection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM " + testTableName + " WHERE FULL_NAME = 'John Doe'")) {
            assertTrue(resultSet.next(), "Should find John Doe in the database");
            assertEquals(35, resultSet.getInt("AGE"), "Age should match");
            assertEquals("Engineering", resultSet.getString("DEPARTMENT"), "Department should match");
            assertEquals(50000.00, resultSet.getDouble("SALARY"), 0.01, "Salary should match default value");
            assertTrue(resultSet.getBoolean("IS_ACTIVE"), "IsActive should match default value");
        }

        // Disconnect file connection
        fileConnection.disconnect();
    }
}
