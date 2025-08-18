/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.core.model;

import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quicktheories.core.Gen;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property-based tests for the Table class using QuickTheory
 * These tests verify that certain properties hold true for a wide range of inputs.
 */
public class TablePropertyTest {

    private TableCore table;

    @BeforeEach
    void setUp() {
        table = new TableCore();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("Age", "int");
        columnNames.put("Salary", "double");
        columnNames.put("IsActive", "boolean");
        columnNames.put("BirthDate", "date");
        columnNames.put("StartTime", "time");
        columnNames.put("CreatedAt", "datetime");
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
        // Test specific strings that should be treated as strings
        // These are strings from the testInferTypeString and testInferTypeEdgeCases tests in TableTest
        String[] testStrings = {
            "hello", "123abc", "true_false", "1,234", "1.2.3", "$100", "50%", "1+2", "1-2-3"
        };

        for (String value : testStrings) {
            assertEquals("string", table.inferType(value), "Value '" + value + "' should be inferred as string");
        }
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

                // Add date, time, and datetime values
                String birthDate = "2000-01-01";
                String startTime = "09:30:00";
                String createdAt = "2023-01-01T12:00:00";
                row.put("BirthDate", birthDate);
                row.put("StartTime", startTime);
                row.put("CreatedAt", createdAt);

                // Add the row to the table
                table.addRow(row);

                // Get the row index (it's the last row)
                int rowIndex = table.getRowCount() - 1;

                // Verify that the retrieved values match the original values
                boolean nameMatches = table.getValueAt(rowIndex, "Name").equals(name);
                boolean ageMatches = table.getValueAt(rowIndex, "Age").equals(age.toString());

                // For salary (double), we need to handle potential formatting differences
                // The actual value might be formatted differently than the input string
                Object salaryValue = table.getValueObject(rowIndex, "Salary");
                boolean salaryMatches = salaryValue instanceof Double && 
                                       Math.abs((Double)salaryValue - salary) < 0.0001;

                boolean isActiveMatches = table.getValueAt(rowIndex, "IsActive").equals(isActive.toString());

                // Verify date, time, and datetime values
                boolean birthDateMatches = table.getValueAt(rowIndex, "BirthDate").equals(birthDate);
                boolean startTimeMatches = table.getValueAt(rowIndex, "StartTime").equals(startTime);
                boolean createdAtMatches = table.getValueAt(rowIndex, "CreatedAt").equals(createdAt);

                return nameMatches && ageMatches && salaryMatches && isActiveMatches &&
                       birthDateMatches && startTimeMatches && createdAtMatches;
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

}
