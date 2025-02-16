package dev.mars;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    private Table table;

    @BeforeEach
    void setUp() {
        table = new Table();

        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "Name");
        columnNames.put(1, "Age");
        columnNames.put(2, "Occupation");

        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("Name", "string");
        columnTypes.put("Age", "int");
        columnTypes.put("Occupation", "string");

        table.setColumnNames(columnNames, columnTypes);
    }

    @Test
    void testAddRow() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");

        table.addRow(row);

        assertEquals(1, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Occupation"));
    }

    @Test
    void testSetValueAt() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");

        table.addRow(row);
        table.setValueAt(0, "Age", "31");

        assertEquals("31", table.getValueAt(0, "Age"));
    }

    @Test
    void testInvalidRowSize() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");

        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }

    @Test
    void testInvalidValueType() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "thirty");
        row.put("Occupation", "Engineer");

        assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
    }

    @Test
    void testGetValueAtInvalidRowIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> table.getValueAt(-1, "Name"));
        assertThrows(IndexOutOfBoundsException.class, () -> table.getValueAt(1, "Name"));
    }

    @Test
    void testGetValueAtInvalidColumnName() {
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("Age", "30");
        row.put("Occupation", "Engineer");

        table.addRow(row);

        assertThrows(IllegalArgumentException.class, () -> table.getValueAt(0, "InvalidColumn"));
    }
}