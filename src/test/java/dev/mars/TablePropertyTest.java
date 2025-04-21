package dev.mars;


import dev.mars.csv.CSVUtils;
import dev.mars.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.*;

/**
 * Property-based tests for the Table class using QuickTheory
 * These tests verify that certain properties hold true for a wide range of inputs.
 */
public class TablePropertyTest {

    private Table table;

    @BeforeEach
    void setUp() {
        table = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("Age", "int");
        columnNames.put("Salary", "double");
        columnNames.put("IsActive", "boolean");
        table.setColumns(columnNames);
    }

    /**
     * Property: For any valid integer string, inferType should return "int"
     */
    @Test
    void testInferTypeIntProperty() {
        qt()
            .forAll(integers().all().map(Object::toString))
            .check(value -> table.inferType(value).equals("int"));
    }

    /**
     * Property: For any valid double string with decimal point, inferType should return "double"
     */
    @Test
    void testInferTypeDoubleProperty() {
        // Generate doubles and format them to ensure they have a decimal point
        qt()
            .forAll(doubles().between(-1000000, 1000000))
            .check(value -> {
                String doubleStr = String.format("%.2f", value);
                return table.inferType(doubleStr).equals("double");
            });
    }

    /**
     * Property: For any boolean value (true/false), inferType should return "boolean"
     */
    @Test
    void testInferTypeBooleanProperty() {
        qt()
            .forAll(booleans().all().map(Object::toString))
            .check(value -> table.inferType(value).equals("boolean"));
    }

    /**
     * Property: For any string that doesn't match int, double, or boolean patterns,
     * inferType should return "string"
     */
    @Test
    void testInferTypeStringProperty() {
        // Generate strings with letters and special characters
        qt()
            .forAll(strings().basicLatinAlphabet().ofLengthBetween(1, 20))
            .assuming(s -> !s.matches("-?\\d+") && !s.matches("-?\\d*\\.\\d+") && 
                    !s.equalsIgnoreCase("true") && !s.equalsIgnoreCase("false"))
            .check(value -> table.inferType(value).equals("string"));
    }

    /**
     * Property: Adding a row and then retrieving its values should return the original values
     */
    @Test
    void testAddRowAndRetrieveProperty() {
        // Generate random names (strings)
        Gen<String> nameGen = strings().basicLatinAlphabet().ofLengthBetween(1, 20);

        // Generate random ages (integers between 0 and 120)
        Gen<Integer> ageGen = integers().between(0, 120);

        // Generate random salaries (doubles between 0 and 1,000,000)
        Gen<Double> salaryGen = doubles().between(0, 1000000);

        // Generate random isActive flags (booleans)
        Gen<Boolean> isActiveGen = booleans().all();

        qt()
            .forAll(nameGen, ageGen, salaryGen, isActiveGen)
            .check((name, age, salary, isActive) -> {
                // Create a row with the generated values
                Map<String, String> row = new HashMap<>();
                row.put("Name", name);
                row.put("Age", age.toString());
                row.put("Salary", salary.toString());
                row.put("IsActive", isActive.toString());

                // Add the row to the table
                table.addRow(row);

                // Get the row index (it's the last row)
                int rowIndex = table.getRowCount() - 1;

                // Verify that the retrieved values match the original values
                boolean nameMatches = table.getValueAt(rowIndex, "Name").equals(name);
                boolean ageMatches = table.getValueAt(rowIndex, "Age").equals(age.toString());

                // For salary (double), we need to handle potential formatting differences
                // The actual value might be formatted differently than the input string
                Object salaryValue = table.getValue(rowIndex, "Salary");
                boolean salaryMatches = salaryValue instanceof Double && 
                                       Math.abs((Double)salaryValue - salary) < 0.0001;

                boolean isActiveMatches = table.getValueAt(rowIndex, "IsActive").equals(isActive.toString());

                return nameMatches && ageMatches && salaryMatches && isActiveMatches;
            });
    }

    /**
     * Property: Setting a value and then retrieving it should return the set value
     */
    @Test
    void testSetValueAndRetrieveProperty() {
        // First, add a row to the table
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Test");
        row.put("Age", "30");
        row.put("Salary", "50000");
        row.put("IsActive", "true");
        table.addRow(row);

        // Generate random ages (integers between 0 and 120)
        Gen<Integer> ageGen = integers().between(0, 120);

        qt()
            .forAll(ageGen)
            .check(newAge -> {
                // Set the age to the new value
                table.setValueAt(0, "Age", newAge.toString());

                // Verify that the retrieved value matches the set value
                return table.getValueAt(0, "Age").equals(newAge.toString());
            });
    }

    /**
     * Property: For any valid row data, adding it to the table should increase the row count by 1
     */
    @Test
    void testAddRowIncreasesRowCountProperty() {
        // Generate random names (strings)
        Gen<String> nameGen = strings().basicLatinAlphabet().ofLengthBetween(1, 20);

        // Generate random ages (integers between 0 and 120)
        Gen<Integer> ageGen = integers().between(0, 120);

        // Generate random salaries (doubles between 0 and 1,000,000)
        Gen<Double> salaryGen = doubles().between(0, 1000000);

        // Generate random isActive flags (booleans)
        Gen<Boolean> isActiveGen = booleans().all();

        qt()
            .forAll(nameGen, ageGen, salaryGen, isActiveGen)
            .check((name, age, salary, isActive) -> {
                // Record the current row count
                int initialRowCount = table.getRowCount();

                // Create a row with the generated values
                Map<String, String> row = new HashMap<>();
                row.put("Name", name);
                row.put("Age", age.toString());
                row.put("Salary", salary.toString());
                row.put("IsActive", isActive.toString());

                // Add the row to the table
                table.addRow(row);

                // Verify that the row count increased by 1
                return table.getRowCount() == initialRowCount + 1;
            });
    }

    /**
     * Property: Writing a table to CSV and reading it back should preserve the data
     */
    @Test
    void testCSVRoundTripProperty() {
        // Generate a small number of rows with random data
        qt()
            .withExamples(10) // Limit to 10 examples to avoid creating too many files
            .forAll(integers().between(1, 5)) // Number of rows to generate
            .check(rowCount -> {
                // Create a new table for this test
                Table sourceTable = new Table();
                var columnNames = new LinkedHashMap<String, String>();
                columnNames.put("Name", "string");
                columnNames.put("Age", "int");
                columnNames.put("Salary", "double");
                sourceTable.setColumns(columnNames);

                // Add random rows
                for (int i = 0; i < rowCount; i++) {
                    Map<String, String> row = new HashMap<>();
                    row.put("Name", "Person" + i);
                    row.put("Age", String.valueOf(20 + i));
                    row.put("Salary", String.valueOf(50000.0 + i * 1000.0));
                    sourceTable.addRow(row);
                }

                // Create a temporary file for the CSV
                String tempFileName = "temp_test_" + System.currentTimeMillis() + ".csv";

                try {
                    // Write the table to CSV
                    CSVUtils.writeToCSV(sourceTable, tempFileName, true);

                    // Create a new table to read the CSV into
                    Table targetTable = new Table();

                    // Read the CSV back into the new table
                    CSVUtils.readFromCSV(targetTable, tempFileName, true, false);

                    // Verify that the tables have the same number of rows
                    if (sourceTable.getRowCount() != targetTable.getRowCount()) {
                        return false;
                    }

                    // Verify that the data in each row matches
                    for (int i = 0; i < sourceTable.getRowCount(); i++) {
                        String sourceName = sourceTable.getValueAt(i, "Name");
                        String targetName = targetTable.getValueAt(i, "Name");
                        if (!sourceName.equals(targetName)) {
                            return false;
                        }

                        String sourceAge = sourceTable.getValueAt(i, "Age");
                        String targetAge = targetTable.getValueAt(i, "Age");
                        if (!sourceAge.equals(targetAge)) {
                            return false;
                        }

                        // For salary (double), we need to handle potential formatting differences
                        Object sourceSalary = sourceTable.getValue(i, "Salary");
                        Object targetSalary = targetTable.getValue(i, "Salary");
                        if (!(sourceSalary instanceof Double && targetSalary instanceof Double &&
                              Math.abs((Double)sourceSalary - (Double)targetSalary) < 0.0001)) {
                            return false;
                        }
                    }

                    return true;
                } finally {
                    // Clean up the temporary file
                    new File(tempFileName).delete();
                }
            });
    }
}