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
import dev.mars.jtable.io.common.adapter.ITableAdapter;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBCTableAdapter.
 * This class tests that the adapter correctly delegates method calls to the Table instance
 * and implements the IJDBCDataSource interface.
 */
class JDBCTableAdapterTestH2 {

    private TableCore table;
    private JDBCTableAdapter adapter;

    @BeforeEach
    void setUp() {
        // Create a new Table instance for each test
        table = new TableCore();
        
        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        table.setColumns(columns);
        
        // Create the adapter with the TableCore
        adapter = new JDBCTableAdapter(table);
    }

    @Test
    void testImplementsInterfaces() {
        // Test that the adapter implements the correct interfaces
        assertTrue(adapter instanceof ITableAdapter);
        assertTrue(adapter instanceof IJDBCDataSource);
    }

    @Test
    void testGetTable() {
        // Test that getTable returns the correct table instance
        assertSame(table, adapter.getTable());
    }

    @Test
    void testGetRowCount() {
        // Add a row to the table
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");
        table.addRow(row);
        
        // Test that getRowCount returns the correct value
        assertEquals(1, adapter.getRowCount());
        
        // Add another row
        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");
        table.addRow(row2);
        
        // Test that getRowCount returns the updated value
        assertEquals(2, adapter.getRowCount());
    }

    @Test
    void testGetColumnCount() {
        // Test that getColumnCount returns the correct value
        assertEquals(3, adapter.getColumnCount());
        
        // Add a new column
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Occupation", "string");
        columns.put("Salary", "double");
        table.setColumns(columns);
        
        // Test that getColumnCount returns the updated value
        assertEquals(4, adapter.getColumnCount());
    }

    @Test
    void testGetColumnName() {
        // Test that getColumnName returns the correct value
        assertEquals("Name", adapter.getColumnName(0));
        assertEquals("Age", adapter.getColumnName(1));
        assertEquals("Occupation", adapter.getColumnName(2));
    }

    @Test
    void testGetValueAt() {
        // Add a row to the table
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");
        table.addRow(row);
        
        // Test that getValueAt returns the correct value
        assertEquals("Alice", adapter.getValueAt(0, "Name"));
        assertEquals("30", adapter.getValueAt(0, "Age"));
        assertEquals("Engineer", adapter.getValueAt(0, "Occupation"));
    }

    @Test
    void testInferType() {
        // Test that inferType returns the correct value
        assertEquals("int", adapter.inferType("123"));
        assertEquals("double", adapter.inferType("123.45"));
        assertEquals("boolean", adapter.inferType("true"));
        assertEquals("string", adapter.inferType("hello"));
    }

    @Test
    void testSetColumns() {
        // Create a new set of columns
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "int");
        columns.put("Name", "string");
        columns.put("Salary", "double");
        
        // Set the columns through the adapter
        adapter.setColumns(columns);
        
        // Test that the columns were set correctly
        assertEquals(3, adapter.getColumnCount());
        assertEquals("ID", adapter.getColumnName(0));
        assertEquals("Name", adapter.getColumnName(1));
        assertEquals("Salary", adapter.getColumnName(2));
    }

    @Test
    void testAddRow() {
        // Create a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");
        
        // Add the row through the adapter
        adapter.addRow(row);
        
        // Test that the row was added correctly
        assertEquals(1, adapter.getRowCount());
        assertEquals("Alice", adapter.getValueAt(0, "Name"));
        assertEquals("30", adapter.getValueAt(0, "Age"));
        assertEquals("Engineer", adapter.getValueAt(0, "Occupation"));
    }
}