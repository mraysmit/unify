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
package dev.mars.jtable.io.files.jdbc;

import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBCMappingReader.
 * This class tests reading data from a database using JDBCMappingReader with mapping configurations.
 */
class JDBCMappingReaderTestH2 {

    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String TEST_TABLE = "test_table";
    private TableCore table;
    private JDBCMappingReader reader;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new Table instance for each test
        table = new TableCore();
        
        // Create the reader
        reader = new JDBCMappingReader();
        
        // Set up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create a test table
            stmt.execute("CREATE TABLE " + TEST_TABLE + " (id INT, full_name VARCHAR(255), years INT)");
            
            // Insert test data
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (1, 'Alice Smith', 30)");
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (2, 'Bob Johnson', 25)");
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES (3, 'Charlie Brown', 35)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
        }
    }

    @Test
    void testReadFromDatabaseWithMapping() throws Exception {
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("full_name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("years", "Age", "int"));
        
        // Read data from the database
        reader.readFromDatabase(table, config);
        
        // Verify the table content
        assertEquals(3, table.getRowCount(), "Table should have 3 rows");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns");
        
        // Check column names
        assertEquals("ID", table.getColumnName(0), "First column should be 'ID'");
        assertEquals("Name", table.getColumnName(1), "Second column should be 'Name'");
        assertEquals("Age", table.getColumnName(2), "Third column should be 'Age'");
        
        // Check row values
        assertEquals("1", table.getValueAt(0, "ID"), "First row, ID column should be '1'");
        assertEquals("Alice Smith", table.getValueAt(0, "Name"), "First row, Name column should be 'Alice Smith'");
        assertEquals("30", table.getValueAt(0, "Age"), "First row, Age column should be '30'");
        
        assertEquals("2", table.getValueAt(1, "ID"), "Second row, ID column should be '2'");
        assertEquals("Bob Johnson", table.getValueAt(1, "Name"), "Second row, Name column should be 'Bob Johnson'");
        assertEquals("25", table.getValueAt(1, "Age"), "Second row, Age column should be '25'");
        
        assertEquals("3", table.getValueAt(2, "ID"), "Third row, ID column should be '3'");
        assertEquals("Charlie Brown", table.getValueAt(2, "Name"), "Third row, Name column should be 'Charlie Brown'");
        assertEquals("35", table.getValueAt(2, "Age"), "Third row, Age column should be '35'");
    }

    @Test
    void testReadFromDatabaseWithQuery() throws Exception {
        // Create a mapping configuration with a query
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("query", "SELECT * FROM " + TEST_TABLE + " WHERE years > 25")
                .setOption("username", "")
                .setOption("password", "")
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("full_name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("years", "Age", "int"));
        
        // Read data from the database
        reader.readFromDatabase(table, config);
        
        // Verify the table content
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(3, table.getColumnCount(), "Table should have 3 columns");
        
        // Check row values for Alice (age 30) and Charlie (age 35)
        boolean foundAlice = false;
        boolean foundCharlie = false;
        
        for (int i = 0; i < table.getRowCount(); i++) {
            String name = table.getValueAt(i, "Name");
            String age = table.getValueAt(i, "Age");
            
            if ("Alice Smith".equals(name)) {
                foundAlice = true;
                assertEquals("30", age, "Alice's age should be '30'");
            } else if ("Charlie Brown".equals(name)) {
                foundCharlie = true;
                assertEquals("35", age, "Charlie's age should be '35'");
            }
        }
        
        assertTrue(foundAlice, "Alice should be in the result set");
        assertTrue(foundCharlie, "Charlie should be in the result set");
    }

    @Test
    void testReadFromDatabaseWithDefaultValues() throws Exception {
        // Create a mapping configuration with a column that doesn't exist in the database
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("full_name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("years", "Age", "int"))
                .addColumnMapping(new ColumnMapping("occupation", "Job", "string").setDefaultValue("Unknown"));
        
        // Read data from the database
        reader.readFromDatabase(table, config);
        
        // Verify the table content
        assertEquals(3, table.getRowCount(), "Table should have 3 rows");
        assertEquals(4, table.getColumnCount(), "Table should have 4 columns");
        
        // Check that the default value was used for the missing column
        assertEquals("Unknown", table.getValueAt(0, "Job"), "First row, Job column should be 'Unknown'");
        assertEquals("Unknown", table.getValueAt(1, "Job"), "Second row, Job column should be 'Unknown'");
        assertEquals("Unknown", table.getValueAt(2, "Job"), "Third row, Job column should be 'Unknown'");
    }

    @Test
    void testInvalidConfiguration() {
        // Create a mapping configuration without tableName or query
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("username", "")
                .setOption("password", "")
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("full_name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("years", "Age", "int"));
        
        // Read data from the database should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reader.readFromDatabase(table, config);
        });
        
        // Verify the exception message
        assertTrue(exception.getMessage().contains("Either 'tableName' or 'query' must be specified"), 
                "Exception message should mention missing tableName or query");
    }

    @Test
    void testNullTable() {
        // Create a mapping configuration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation(TEST_DB_URL)
                .setOption("tableName", TEST_TABLE)
                .setOption("username", "")
                .setOption("password", "")
                .addColumnMapping(new ColumnMapping("id", "ID", "int"))
                .addColumnMapping(new ColumnMapping("full_name", "Name", "string"))
                .addColumnMapping(new ColumnMapping("years", "Age", "int"));
        
        // Read data from the database with a null table should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reader.readFromDatabase(null, config);
        });
        
        // Verify the exception message
        assertTrue(exception.getMessage().contains("Table cannot be null"), 
                "Exception message should mention null table");
    }

    @Test
    void testNullConfiguration() {
        // Read data from the database with a null configuration should throw an exception
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            reader.readFromDatabase(table, null);
        });
        
        // Verify the exception message
        assertTrue(exception.getMessage().contains("Mapping configuration cannot be null"), 
                "Exception message should mention null configuration");
    }
}