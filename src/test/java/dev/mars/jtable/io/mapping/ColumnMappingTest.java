package dev.mars.jtable.io.mapping;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ColumnMapping.
 * This class tests the functionality of the ColumnMapping class.
 */
class ColumnMappingTest {

    @Test
    void testConstructorWithSourceColumnName() {
        // Create a ColumnMapping with a source column name
        ColumnMapping mapping = new ColumnMapping("SourceColumn", "TargetColumn", "string");
        
        // Verify the properties were set correctly
        assertEquals("SourceColumn", mapping.getSourceColumnName());
        assertEquals("TargetColumn", mapping.getTargetColumnName());
        assertEquals("string", mapping.getTargetColumnType());
        assertEquals(-1, mapping.getSourceColumnIndex());
        assertNull(mapping.getDefaultValue());
        assertTrue(mapping.usesSourceColumnName());
        assertFalse(mapping.usesSourceColumnIndex());
    }

    @Test
    void testConstructorWithSourceColumnIndex() {
        // Create a ColumnMapping with a source column index
        ColumnMapping mapping = new ColumnMapping(2, "TargetColumn", "int");
        
        // Verify the properties were set correctly
        assertNull(mapping.getSourceColumnName());
        assertEquals("TargetColumn", mapping.getTargetColumnName());
        assertEquals("int", mapping.getTargetColumnType());
        assertEquals(2, mapping.getSourceColumnIndex());
        assertNull(mapping.getDefaultValue());
        assertFalse(mapping.usesSourceColumnName());
        assertTrue(mapping.usesSourceColumnIndex());
    }

    @Test
    void testSetDefaultValue() {
        // Create a ColumnMapping
        ColumnMapping mapping = new ColumnMapping("SourceColumn", "TargetColumn", "double");
        
        // Set a default value
        ColumnMapping returnedMapping = mapping.setDefaultValue("0.0");
        
        // Verify the default value was set correctly
        assertEquals("0.0", mapping.getDefaultValue());
        
        // Verify method chaining works
        assertSame(mapping, returnedMapping);
    }

    @Test
    void testUsesSourceColumnName() {
        // Create a ColumnMapping with a source column name
        ColumnMapping mapping1 = new ColumnMapping("SourceColumn", "TargetColumn", "string");
        
        // Create a ColumnMapping with a source column index
        ColumnMapping mapping2 = new ColumnMapping(0, "TargetColumn", "string");
        
        // Verify usesSourceColumnName returns the correct value
        assertTrue(mapping1.usesSourceColumnName());
        assertFalse(mapping2.usesSourceColumnName());
    }

    @Test
    void testUsesSourceColumnIndex() {
        // Create a ColumnMapping with a source column name
        ColumnMapping mapping1 = new ColumnMapping("SourceColumn", "TargetColumn", "string");
        
        // Create a ColumnMapping with a source column index
        ColumnMapping mapping2 = new ColumnMapping(0, "TargetColumn", "string");
        
        // Verify usesSourceColumnIndex returns the correct value
        assertFalse(mapping1.usesSourceColumnIndex());
        assertTrue(mapping2.usesSourceColumnIndex());
    }

    @Test
    void testNegativeSourceColumnIndex() {
        // Create a ColumnMapping with a negative source column index
        ColumnMapping mapping = new ColumnMapping(-5, "TargetColumn", "string");
        
        // Verify usesSourceColumnIndex returns false for negative index
        assertFalse(mapping.usesSourceColumnIndex());
    }

    @Test
    void testZeroSourceColumnIndex() {
        // Create a ColumnMapping with source column index 0
        ColumnMapping mapping = new ColumnMapping(0, "TargetColumn", "string");
        
        // Verify usesSourceColumnIndex returns true for index 0
        assertTrue(mapping.usesSourceColumnIndex());
    }

    @Test
    void testDifferentTargetColumnTypes() {
        // Test with different target column types
        ColumnMapping stringMapping = new ColumnMapping("Source", "Target", "string");
        ColumnMapping intMapping = new ColumnMapping("Source", "Target", "int");
        ColumnMapping doubleMapping = new ColumnMapping("Source", "Target", "double");
        ColumnMapping booleanMapping = new ColumnMapping("Source", "Target", "boolean");
        
        // Verify the target column types were set correctly
        assertEquals("string", stringMapping.getTargetColumnType());
        assertEquals("int", intMapping.getTargetColumnType());
        assertEquals("double", doubleMapping.getTargetColumnType());
        assertEquals("boolean", booleanMapping.getTargetColumnType());
    }
}