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
 * Complex integration tests for CSV mapping functionality.
 * This class tests more advanced scenarios for CSV mapping.
 */
public class CSVMappingReaderComplexTest {

    private static final String EMPLOYEE_CSV_FILE = "employee_data.csv";
    private static final String PRODUCT_CSV_FILE = "product_data.csv";
    private static final String SALES_CSV_FILE = "sales_data.csv";
    private static final String TRANSFORMED_CSV_FILE = "transformed_data.csv";

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
    void testLargeDatasetWithMapping() throws Exception {
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
            double salary = 40000 + (i * 1000); // Salaries between 41000 and 90000 :-)
            String hireDate = "2020-" + String.format("%02d", (i % 12) + 1) + "-" + String.format("%02d", (i % 28) + 1);
            boolean isManager = i % 10 == 0; // Every 10th employee is a manager. We probably need more LOL.

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

        // Write the employee data to a CSV file
        CSVProcessor.writeToCSV(employeeTable, EMPLOYEE_CSV_FILE, true);

        // Verify the CSV file was created and has the expected number of rows
        File csvFile = new File(EMPLOYEE_CSV_FILE);
        assertTrue(csvFile.exists(), "CSV file should exist");

        List<String> lines = Files.readAllLines(Paths.get(EMPLOYEE_CSV_FILE));
        assertEquals(NO_OF_ROWS + 1, lines.size(), "CSV should have 51 lines (header + 50 data rows)");

        // Create a mapping configuration for employee data
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(EMPLOYEE_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("ID", "EmployeeID", "string"))
                .addColumnMapping(new ColumnMapping("FirstName", "FirstName", "string"))
                .addColumnMapping(new ColumnMapping("LastName", "LastName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Department", "Department", "string"))
                .addColumnMapping(new ColumnMapping("Salary", "AnnualSalary", "double"))
                .addColumnMapping(new ColumnMapping("HireDate", "HireDate", "string"))
                .addColumnMapping(new ColumnMapping("IsManager", "IsManager", "boolean"));

        // Read the CSV file using the mapping configuration
        TableCore mappedTable = new TableCore();
        CSVProcessor.readFromCSV(mappedTable, config);

        // Validate the mapped table content
        assertEquals(NO_OF_ROWS, mappedTable.getRowCount(), "Mapped table should have 50 rows");
        assertEquals(8, mappedTable.getColumnCount(), "Mapped table should have 8 columns");

        // Check a few random rows
        assertEquals("EMP001", mappedTable.getValueAt(0, "EmployeeID"), "First row, EmployeeID should match");
        assertEquals("FirstName25", mappedTable.getValueAt(24, "FirstName"), "25th row, FirstName should match");
        assertEquals("LastName50", mappedTable.getValueAt(49, "LastName"), "50th row, LastName should match");

        // Write the mapped table to a new CSV file
        CSVProcessor.writeToCSV(mappedTable, TRANSFORMED_CSV_FILE, true);

        // Verify the transformed CSV file was created
        File transformedFile = new File(TRANSFORMED_CSV_FILE);
        assertTrue(transformedFile.exists(), "Transformed CSV file should exist");
    }

    @Test
    void testMultipleCSVFilesWithRelationships() throws Exception {
        // Create product data
        TableBuilder productBuilder = new TableBuilder();
        ITable productTable = productBuilder
                .addStringColumn("ProductID")
                .addStringColumn("ProductName")
                .addDoubleColumn("UnitPrice")
                .addIntColumn("UnitsInStock")
                .setCreateDefaultValue(true)
                .addRow("ProductID", "P001", "ProductName", "Laptop", "UnitPrice", "1200.50", "UnitsInStock", "10")
                .addRow("ProductID", "P002", "ProductName", "Mouse", "UnitPrice", "25.99", "UnitsInStock", "50")
                .addRow("ProductID", "P003", "ProductName", "Keyboard", "UnitPrice", "55.50", "UnitsInStock", "30")
                .addRow("ProductID", "P004", "ProductName", "Monitor", "UnitPrice", "299.99", "UnitsInStock", "15")
                .addRow("ProductID", "P005", "ProductName", "Headphones", "UnitPrice", "89.95", "UnitsInStock", "25")
                .build();

        // Write product data to CSV
        CSVProcessor.writeToCSV(productTable, PRODUCT_CSV_FILE, true);

        // Create sales data with references to products
        TableBuilder salesBuilder = new TableBuilder();
        ITable salesTable = salesBuilder
                .addStringColumn("SaleID")
                .addStringColumn("ProductID")
                .addIntColumn("Quantity")
                .addDoubleColumn("TotalPrice")
                .addStringColumn("SaleDate")
                .setCreateDefaultValue(true)
                .addRow("SaleID", "S001", "ProductID", "P001", "Quantity", "2", "TotalPrice", "2401.00", "SaleDate", "2023-01-15")
                .addRow("SaleID", "S002", "ProductID", "P002", "Quantity", "5", "TotalPrice", "129.95", "SaleDate", "2023-01-16")
                .addRow("SaleID", "S003", "ProductID", "P003", "Quantity", "3", "TotalPrice", "166.50", "SaleDate", "2023-01-17")
                .addRow("SaleID", "S004", "ProductID", "P001", "Quantity", "1", "TotalPrice", "1200.50", "SaleDate", "2023-01-18")
                .addRow("SaleID", "S005", "ProductID", "P005", "Quantity", "4", "TotalPrice", "359.80", "SaleDate", "2023-01-19")
                .addRow("SaleID", "S006", "ProductID", "P004", "Quantity", "2", "TotalPrice", "599.98", "SaleDate", "2023-01-20")
                .addRow("SaleID", "S007", "ProductID", "P002", "Quantity", "10", "TotalPrice", "259.90", "SaleDate", "2023-01-21")
                .addRow("SaleID", "S008", "ProductID", "P003", "Quantity", "2", "TotalPrice", "111.00", "SaleDate", "2023-01-22")
                .build();

        // Write sales data to CSV
        CSVProcessor.writeToCSV(salesTable, SALES_CSV_FILE, true);

        // Verify both CSV files were created
        assertTrue(new File(PRODUCT_CSV_FILE).exists(), "Product CSV file should exist");
        assertTrue(new File(SALES_CSV_FILE).exists(), "Sales CSV file should exist");

        // Create mapping configurations for both files
        MappingConfiguration productConfig = new MappingConfiguration()
                .setSourceLocation(PRODUCT_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("ProductID", "ID", "string"))
                .addColumnMapping(new ColumnMapping("ProductName", "Name", "string"))
                .addColumnMapping(new ColumnMapping("UnitPrice", "Price", "double"))
                .addColumnMapping(new ColumnMapping("UnitsInStock", "Stock", "int"));

        MappingConfiguration salesConfig = new MappingConfiguration()
                .setSourceLocation(SALES_CSV_FILE)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("SaleID", "ID", "string"))
                .addColumnMapping(new ColumnMapping("ProductID", "ProductRef", "string"))
                .addColumnMapping(new ColumnMapping("Quantity", "Qty", "int"))
                .addColumnMapping(new ColumnMapping("TotalPrice", "Total", "double"))
                .addColumnMapping(new ColumnMapping("SaleDate", "Date", "string"));

        // Read both CSV files using their respective mapping configurations
        TableCore productMappedTable = new TableCore();
        CSVProcessor.readFromCSV(productMappedTable, productConfig);

        TableCore salesMappedTable = new TableCore();
        CSVProcessor.readFromCSV(salesMappedTable, salesConfig);

        // Validate the mapped tables
        assertEquals(5, productMappedTable.getRowCount(), "Product table should have 5 rows");
        assertEquals(8, salesMappedTable.getRowCount(), "Sales table should have 8 rows");

        // Verify relationships between tables by checking some sales records against product data
        // For example, find a sale for product P001 and verify the product exists in the product table
        String productRef = salesMappedTable.getValueAt(0, "ProductRef");
        assertEquals("P001", productRef, "First sale should reference product P001");

        // Find the corresponding product in the product table
        boolean foundProduct = false;
        for (int i = 0; i < productMappedTable.getRowCount(); i++) {
            if (productMappedTable.getValueAt(i, "ID").equals(productRef)) {
                foundProduct = true;
                assertEquals("Laptop", productMappedTable.getValueAt(i, "Name"), "Product name should match");
                assertEquals("1200.5", productMappedTable.getValueAt(i, "Price"), "Product price should match");
                break;
            }
        }
        assertTrue(foundProduct, "Should find the referenced product in the product table");
    }

    @Test
    void testCSVTransformationWithCalculatedFields() throws Exception {
        // Create a table with data that needs transformation
        TableBuilder builder = new TableBuilder();
        ITable sourceTable = builder
                .addStringColumn("ProductID")
                .addStringColumn("ProductName")
                .addDoubleColumn("UnitPrice")
                .addIntColumn("Quantity")
                .setCreateDefaultValue(true)
                .addRow("ProductID", "P001", "ProductName", "Laptop", "UnitPrice", "1200.50", "Quantity", "2")
                .addRow("ProductID", "P002", "ProductName", "Mouse", "UnitPrice", "25.99", "Quantity", "5")
                .addRow("ProductID", "P003", "ProductName", "Keyboard", "UnitPrice", "55.50", "Quantity", "3")
                .addRow("ProductID", "P004", "ProductName", "Monitor", "UnitPrice", "299.99", "Quantity", "1")
                .addRow("ProductID", "P005", "ProductName", "Headphones", "UnitPrice", "89.95", "Quantity", "4")
                .build();

        // Write the source table to a CSV file
        CSVProcessor.writeToCSV(sourceTable, PRODUCT_CSV_FILE, true);

        // Read the CSV file into a new table
        TableCore rawTable = new TableCore();
        CSVProcessor.readFromCSV(rawTable, PRODUCT_CSV_FILE, true, false);

        // Create a new table with calculated fields
        TableCore transformedTable = new TableCore();

        // Set up columns for the transformed table
        LinkedHashMap<String, String> columnDefs = new LinkedHashMap<>();
        columnDefs.put("ProductID", "string");
        columnDefs.put("ProductName", "string");
        columnDefs.put("UnitPrice", "double");
        columnDefs.put("Quantity", "int");
        columnDefs.put("TotalPrice", "double"); // Calculated field: UnitPrice * Quantity
        columnDefs.put("Discount", "double"); // Calculated field: 10% of TotalPrice
        columnDefs.put("FinalPrice", "double"); // Calculated field: TotalPrice - Discount
        transformedTable.setColumns(columnDefs);

        // Copy data from raw table and calculate new fields
        for (int i = 0; i < rawTable.getRowCount(); i++) {
            Map<String, String> row = new HashMap<>();

            // Copy existing fields
            row.put("ProductID", rawTable.getValueAt(i, "ProductID"));
            row.put("ProductName", rawTable.getValueAt(i, "ProductName"));
            row.put("UnitPrice", rawTable.getValueAt(i, "UnitPrice"));
            row.put("Quantity", rawTable.getValueAt(i, "Quantity"));

            // Calculate new fields
            double unitPrice = Double.parseDouble(rawTable.getValueAt(i, "UnitPrice"));
            int quantity = Integer.parseInt(rawTable.getValueAt(i, "Quantity"));
            double totalPrice = unitPrice * quantity;
            double discount = totalPrice * 0.1; // 10% discount
            double finalPrice = totalPrice - discount;

            row.put("TotalPrice", String.valueOf(totalPrice));
            row.put("Discount", String.valueOf(discount));
            row.put("FinalPrice", String.valueOf(finalPrice));

            transformedTable.addRow(row);
        }

        // Write the transformed table to a CSV file
        CSVProcessor.writeToCSV(transformedTable, TRANSFORMED_CSV_FILE, true);

        // Verify the transformed CSV file was created
        File transformedFile = new File(TRANSFORMED_CSV_FILE);
        assertTrue(transformedFile.exists(), "Transformed CSV file should exist");

        // Read the transformed CSV file to verify its content
        List<String> lines = Files.readAllLines(Paths.get(TRANSFORMED_CSV_FILE));
        assertEquals(6, lines.size(), "Transformed CSV should have 6 lines (header + 5 data rows)");
        assertTrue(lines.get(0).contains("TotalPrice") && lines.get(0).contains("Discount") && 
                  lines.get(0).contains("FinalPrice"), "Header should contain calculated field names");

        // Read the transformed CSV file back into a table
        TableCore verificationTable = new TableCore();
        CSVProcessor.readFromCSV(verificationTable, TRANSFORMED_CSV_FILE, true, false);

        // Verify the calculated fields
        for (int i = 0; i < verificationTable.getRowCount(); i++) {
            double unitPrice = Double.parseDouble(verificationTable.getValueAt(i, "UnitPrice"));
            int quantity = Integer.parseInt(verificationTable.getValueAt(i, "Quantity"));
            double expectedTotalPrice = unitPrice * quantity;
            double expectedDiscount = expectedTotalPrice * 0.1;
            double expectedFinalPrice = expectedTotalPrice - expectedDiscount;

            double actualTotalPrice = Double.parseDouble(verificationTable.getValueAt(i, "TotalPrice"));
            double actualDiscount = Double.parseDouble(verificationTable.getValueAt(i, "Discount"));
            double actualFinalPrice = Double.parseDouble(verificationTable.getValueAt(i, "FinalPrice"));

            assertEquals(expectedTotalPrice, actualTotalPrice, 0.001, "TotalPrice calculation should be correct");
            assertEquals(expectedDiscount, actualDiscount, 0.001, "Discount calculation should be correct");
            assertEquals(expectedFinalPrice, actualFinalPrice, 0.001, "FinalPrice calculation should be correct");
        }
    }
}
