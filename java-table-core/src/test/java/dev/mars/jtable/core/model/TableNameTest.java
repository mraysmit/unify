package dev.mars.jtable.core.model;

import dev.mars.jtable.core.table.OptimizedTableCore;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for the Name attribute of ITable implementations.
 * This class tests that the Name attribute is correctly set and retrieved.
 */
public class TableNameTest {

    @Test
    public void testDefaultNames() {
        // Test that the default names are set correctly
        TableCore tableCore = new TableCore();
        Table table = new Table();
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore();

        assertNotNull(tableCore.getName(), "TableCore name should not be null");
        assertNotNull(table.getName(), "Table name should not be null");
        assertNotNull(optimizedTableCore.getName(), "OptimizedTableCore name should not be null");

        assertEquals("TableCore", tableCore.getName(), "TableCore default name should be 'TableCore'");
        assertEquals("Table", table.getName(), "Table default name should be 'Table'");
        assertEquals("OptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore default name should be 'OptimizedTableCore'");
    }

    @Test
    public void testCustomNames() {
        // Test that custom names are set correctly
        TableCore tableCore = new TableCore("CustomTableCore");
        Table table = new Table("CustomTable");
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore("CustomOptimizedTableCore");

        assertEquals("CustomTableCore", tableCore.getName(), "TableCore custom name should be 'CustomTableCore'");
        assertEquals("CustomTable", table.getName(), "Table custom name should be 'CustomTable'");
        assertEquals("CustomOptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore custom name should be 'CustomOptimizedTableCore'");
    }

    @Test
    public void testSetName() {
        // Test that setName works correctly
        TableCore tableCore = new TableCore();
        Table table = new Table();
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore();

        tableCore.setName("NewTableCore");
        table.setName("NewTable");
        optimizedTableCore.setName("NewOptimizedTableCore");

        assertEquals("NewTableCore", tableCore.getName(), "TableCore name should be 'NewTableCore'");
        assertEquals("NewTable", table.getName(), "Table name should be 'NewTable'");
        assertEquals("NewOptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore name should be 'NewOptimizedTableCore'");
    }

    @Test
    public void testNamePropagation() {
        // Test that the name is correctly propagated from Table to TableCore
        Table table = new Table("PropagatedTable");
        assertEquals("PropagatedTable", table.getName(), "Table name should be 'PropagatedTable'");
        assertEquals("PropagatedTable", getTableCore(table).getName(), "TableCore name should be 'PropagatedTable'");

        // Test that changing the name of Table changes the name of TableCore
        table.setName("ChangedTable");
        assertEquals("ChangedTable", table.getName(), "Table name should be 'ChangedTable'");
        assertEquals("ChangedTable", getTableCore(table).getName(), "TableCore name should be 'ChangedTable'");
    }

    // Helper method to access the tableCore field of Table
    private TableCore getTableCore(Table table) {
        try {
            java.lang.reflect.Field field = Table.class.getDeclaredField("tableCore");
            field.setAccessible(true);
            return (TableCore) field.get(table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access tableCore field", e);
        }
    }
}
