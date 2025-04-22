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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complex tests for CSVMappingWriter functionality.
 * This class tests more advanced scenarios for CSVMappingWriter.
 */
public class CSVMappingWriterComplexTest {

    private static final String EMPLOYEE_CSV_FILE = "employee_data_writer.csv";
    private static final String PRODUCT_CSV_FILE = "product_data_writer.csv";
    private static final String SALES_CSV_FILE = "sales_data_writer.csv";
    private static final String TRANSFORMED_CSV_FILE = "transformed_data_writer.csv";

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
        Arrays.asList(EMPLOYEE_CSV_FILE, PRODUCT_CSV_FILE, SALES_CSV_FILE, TRANSFORMED_CSV_FILE)
                .forEach(file -> {
                    try {
                        Files.deleteIfExists(Paths.get(file));
                    } catch (Exception e) {
                        System.err.println("Error deleting file: " + file + " - " + e.getMessage());
                    }
                });
    }

    @Test
    void testLargeDatasetWithMappingWriter() throws Exception {
        final int NO_OF_ROWS = 50;

        // Create a large dataset using TableBuilder
        TableBuilder builder = new TableBuilder();
        builder.addStringColumn("ID")
               .addStringColumn("FirstName")
               .addStringColumn("LastName")
               .addIntColumn("Age")
               .addStringColumn("Department")
               .addDoubleColumn("Salary")
               .addStringColumn("HireDate")
               .addBooleanColumn("IsManager")
               .setCreateDefaultValue(true);

        // Add 50 rows of employee data
        for (int i = 1; i <= NO_OF_ROWS; i++) {
            String id = "EMP" + String.format("%03d", i);
            String firstName = "FirstName" + i;
            String lastName = "LastName" + i;
            int age = 25 + (i % 30); // Ages between 25 and 54
            String department = i % 5 == 0 ? "HR" : i % 4 == 0 ? "Finance" : i % 3 == 0 ? "IT" : i % 2 == 0 ? "Sales" : "Marketing";
            double salary = 40000 + (i * 1000); // Salaries between 41000 and 90000
            String hireDate = "2020-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1);
            boolean isManager = i % 10 == 0; // Every 10th employee is a manager

            builder.addRow(
                "ID", id,
                "FirstName", firstName,
                "LastName", lastName,
                "Age", String.valueOf(age),
                "Department", department,
                "Salary", String.valueOf(salary),
                "HireDate", hireDate,
                "IsManager", String.valueOf(isManager)
            );
        }

        ITable employeeTable = builder.build();

        // Create a mapping configuration for writing employee data
        MappingConfiguration employeeConfig = new MappingConfiguration()
                .setSourceLocation(EMPLOYEE_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("ID", "EmployeeID", "string"))
                .addColumnMapping(new ColumnMapping("FirstName", "First", "string"))
                .addColumnMapping(new ColumnMapping("LastName", "Last", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Department", "Dept", "string"))
                .addColumnMapping(new ColumnMapping("Salary", "AnnualSalary", "double"))
                .addColumnMapping(new ColumnMapping("HireDate", "StartDate", "string"))
                .addColumnMapping(new ColumnMapping("IsManager", "Manager", "boolean"));

        // Write the employee data to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(employeeTable, employeeConfig);

        // Verify the CSV file was created and has the expected number of rows
        File csvFile = new File(EMPLOYEE_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        List<String> lines = Files.readAllLines(Paths.get(EMPLOYEE_CSV_FILE));
        assertEquals(NO_OF_ROWS + 1, lines.size(), "CSV should have header + data rows");

        // Check header row
        String headerLine = lines.get(0);
        assertTrue(headerLine.contains("EmployeeID") && headerLine.contains("First") && 
                  headerLine.contains("Last") && headerLine.contains("AnnualSalary"), 
                  "Header line should contain mapped column names");

        // Check a sample data row
        String firstDataRow = lines.get(1);
        assertTrue(firstDataRow.contains("EMP001") && firstDataRow.contains("FirstName1") && 
                  firstDataRow.contains("LastName1"), 
                  "First data row should contain mapped values");
    }

    @Test
    void testMultipleCSVFilesWithMappingWriter() throws Exception {
        // Create product data
        TableBuilder productBuilder = new TableBuilder();
        ITable productTable = productBuilder
                .addStringColumn("ProductID")
                .addStringColumn("ProductName")
                .addDoubleColumn("Price")
                .addIntColumn("StockQuantity")
                .setCreateDefaultValue(true)
                .addRow("ProductID", "P001", "ProductName", "Laptop", "Price", "1200.50", "StockQuantity", "10")
                .addRow("ProductID", "P002", "ProductName", "Mouse", "Price", "25.99", "StockQuantity", "50")
                .addRow("ProductID", "P003", "ProductName", "Keyboard", "Price", "55.50", "StockQuantity", "30")
                .addRow("ProductID", "P004", "ProductName", "Monitor", "Price", "299.99", "StockQuantity", "15")
                .addRow("ProductID", "P005", "ProductName", "Headphones", "Price", "89.95", "StockQuantity", "25")
                .build();

        // Create sales data
        TableBuilder salesBuilder = new TableBuilder();
        ITable salesTable = salesBuilder
                .addStringColumn("SaleID")
                .addStringColumn("ProductID")
                .addStringColumn("EmployeeID")
                .addIntColumn("Quantity")
                .addDoubleColumn("TotalAmount")
                .addStringColumn("SaleDate")
                .setCreateDefaultValue(true)
                .addRow("SaleID", "S001", "ProductID", "P001", "EmployeeID", "EMP001", "Quantity", "2", "TotalAmount", "2401.00", "SaleDate", "2023-01-15")
                .addRow("SaleID", "S002", "ProductID", "P002", "EmployeeID", "EMP002", "Quantity", "5", "TotalAmount", "129.95", "SaleDate", "2023-01-16")
                .addRow("SaleID", "S003", "ProductID", "P003", "EmployeeID", "EMP001", "Quantity", "3", "TotalAmount", "166.50", "SaleDate", "2023-01-17")
                .addRow("SaleID", "S004", "ProductID", "P004", "EmployeeID", "EMP003", "Quantity", "1", "TotalAmount", "299.99", "SaleDate", "2023-01-18")
                .addRow("SaleID", "S005", "ProductID", "P005", "EmployeeID", "EMP002", "Quantity", "2", "TotalAmount", "179.90", "SaleDate", "2023-01-19")
                .build();

        // Create mapping configurations
        MappingConfiguration productConfig = new MappingConfiguration()
                .setSourceLocation(PRODUCT_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("ProductID", "ID", "string"))
                .addColumnMapping(new ColumnMapping("ProductName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("Price", "UnitPrice", "double"))
                .addColumnMapping(new ColumnMapping("StockQuantity", "InStock", "int"));

        MappingConfiguration salesConfig = new MappingConfiguration()
                .setSourceLocation(SALES_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("SaleID", "ID", "string"))
                .addColumnMapping(new ColumnMapping("ProductID", "ProductRef", "string"))
                .addColumnMapping(new ColumnMapping("EmployeeID", "EmployeeRef", "string"))
                .addColumnMapping(new ColumnMapping("Quantity", "Units", "int"))
                .addColumnMapping(new ColumnMapping("TotalAmount", "Revenue", "double"))
                .addColumnMapping(new ColumnMapping("SaleDate", "Date", "string"));

        // Write the data to CSV files using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(productTable, productConfig);
        writer.writeToCSV(salesTable, salesConfig);

        // Verify the CSV files were created
        File productCsvFile = new File(PRODUCT_CSV_FILE);
        File salesCsvFile = new File(SALES_CSV_FILE);
        assertTrue(productCsvFile.exists(), "Product CSV file should exist");
        assertTrue(salesCsvFile.exists(), "Sales CSV file should exist");

        // Check product file content
        List<String> productLines = Files.readAllLines(Paths.get(PRODUCT_CSV_FILE));
        assertEquals(6, productLines.size(), "Product CSV should have header + 5 data rows");
        assertEquals("ID,Name,UnitPrice,InStock", productLines.get(0), "Product header line should match");

        // Check sales file content
        List<String> salesLines = Files.readAllLines(Paths.get(SALES_CSV_FILE));
        assertEquals(6, salesLines.size(), "Sales CSV should have header + 5 data rows");
        assertEquals("ID,ProductRef,EmployeeRef,Units,Revenue,Date", salesLines.get(0), "Sales header line should match");

        // Verify relationships between files
        // Check that product IDs in sales file match product IDs in product file
        boolean foundMatchingProduct = false;
        String productId = "P001";
        String productRefInSales = "";

        for (int i = 1; i < productLines.size(); i++) {
            String[] productFields = productLines.get(i).split(",");
            if (productFields[0].equals(productId)) {
                foundMatchingProduct = true;
                break;
            }
        }

        for (int i = 1; i < salesLines.size(); i++) {
            String[] salesFields = salesLines.get(i).split(",");
            if (salesFields[1].equals(productId)) {
                productRefInSales = salesFields[1];
                break;
            }
        }

        assertTrue(foundMatchingProduct, "Product ID should exist in product file");
        assertEquals(productId, productRefInSales, "Product ID in sales file should match product ID in product file");
    }

    @Test
    void testCSVTransformationWithMappingWriter() throws Exception {
        // Create employee data
        TableBuilder employeeBuilder = new TableBuilder();
        ITable employeeTable = employeeBuilder
                .addStringColumn("ID")
                .addStringColumn("FirstName")
                .addStringColumn("LastName")
                .addIntColumn("Age")
                .addStringColumn("Department")
                .addDoubleColumn("Salary")
                .addIntColumn("YearsOfService")
                .setCreateDefaultValue(true)
                .addRow("ID", "EMP001", "FirstName", "John", "LastName", "Doe", "Age", "35", "Department", "IT", "Salary", "75000", "YearsOfService", "5")
                .addRow("ID", "EMP002", "FirstName", "Jane", "LastName", "Smith", "Age", "28", "Department", "HR", "Salary", "65000", "YearsOfService", "3")
                .addRow("ID", "EMP003", "FirstName", "Bob", "LastName", "Johnson", "Age", "42", "Department", "Finance", "Salary", "85000", "YearsOfService", "8")
                .addRow("ID", "EMP004", "FirstName", "Alice", "LastName", "Brown", "Age", "31", "Department", "IT", "Salary", "72000", "YearsOfService", "4")
                .addRow("ID", "EMP005", "FirstName", "Charlie", "LastName", "Davis", "Age", "45", "Department", "Sales", "Salary", "90000", "YearsOfService", "10")
                .build();

        // Create a mapping configuration with calculated fields
        MappingConfiguration transformConfig = new MappingConfiguration()
                .setSourceLocation(TRANSFORMED_CSV_FILE)
                .setOption("withHeaderRow", true)
                .addColumnMapping(new ColumnMapping("EmployeeID", "EmployeeID", "string"))
                .addColumnMapping(new ColumnMapping("FirstName", "FirstName", "string"))
                .addColumnMapping(new ColumnMapping("LastName", "LastName", "string"))
                .addColumnMapping(new ColumnMapping("FullName", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Department", "Department", "string"))
                .addColumnMapping(new ColumnMapping("AnnualSalary", "AnnualSalary", "double"))
                .addColumnMapping(new ColumnMapping("Bonus", "Bonus", "double"))
                .addColumnMapping(new ColumnMapping("RetirementContribution", "RetirementContribution", "double"))
                .addColumnMapping(new ColumnMapping("TotalCompensation", "TotalCompensation", "double"));

        // Create a custom table with calculated fields
        TableCore transformedTable = new TableCore();
        var columns = new LinkedHashMap<String, String>();
        columns.put("EmployeeID", "string");
        columns.put("FirstName", "string");
        columns.put("LastName", "string");
        columns.put("FullName", "string");
        columns.put("Department", "string");
        columns.put("AnnualSalary", "double");
        columns.put("Bonus", "double");
        columns.put("RetirementContribution", "double");
        columns.put("TotalCompensation", "double");
        transformedTable.setColumns(columns);

        // Add rows with calculated fields
        for (int i = 0; i < employeeTable.getRowCount(); i++) {
            String id = employeeTable.getValueAt(i, "ID");
            String firstName = employeeTable.getValueAt(i, "FirstName");
            String lastName = employeeTable.getValueAt(i, "LastName");
            String fullName = firstName + " " + lastName;
            String department = employeeTable.getValueAt(i, "Department");
            double salary = Double.parseDouble(employeeTable.getValueAt(i, "Salary"));
            int yearsOfService = Integer.parseInt(employeeTable.getValueAt(i, "YearsOfService"));

            // Calculate bonus: 5% of salary per year of service, up to 25%
            double bonusRate = Math.min(0.05 * yearsOfService, 0.25);
            double bonus = salary * bonusRate;

            // Calculate retirement contribution: 6% of salary
            double retirementContribution = salary * 0.06;

            // Calculate total compensation: salary + bonus
            double totalCompensation = salary + bonus;

            Map<String, String> row = new HashMap<>();
            row.put("EmployeeID", id);
            row.put("FirstName", firstName);
            row.put("LastName", lastName);
            row.put("FullName", fullName);
            row.put("Department", department);
            row.put("AnnualSalary", String.valueOf(salary));
            row.put("Bonus", String.valueOf(bonus));
            row.put("RetirementContribution", String.valueOf(retirementContribution));
            row.put("TotalCompensation", String.valueOf(totalCompensation));

            transformedTable.addRow(row);
        }

        // Write the transformed data to a CSV file using CSVMappingWriter
        CSVMappingWriter writer = new CSVMappingWriter();
        writer.writeToCSV(transformedTable, transformConfig);

        // Verify the CSV file was created
        File csvFile = new File(TRANSFORMED_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        // Check file content
        List<String> lines = Files.readAllLines(Paths.get(TRANSFORMED_CSV_FILE));
        assertEquals(6, lines.size(), "CSV should have header + 5 data rows");

        // Check header row
        String headerLine = lines.get(0);
        assertTrue(headerLine.contains("EmployeeID") && headerLine.contains("FullName") && 
                  headerLine.contains("Bonus") && headerLine.contains("TotalCompensation"), 
                  "Header line should contain calculated field names");

        // Check a sample data row to verify calculated fields
        String firstDataRow = lines.get(1);
        assertTrue(firstDataRow.contains("EMP001") && firstDataRow.contains("John Doe"), 
                  "First data row should contain original and calculated values");

        // Verify that the bonus calculation is correct for the first employee
        // Bonus for EMP001: 5 years * 5% = 25% of 75000 = 18750
        assertTrue(firstDataRow.contains("18750.0"), "Bonus calculation should be correct");
    }
}
