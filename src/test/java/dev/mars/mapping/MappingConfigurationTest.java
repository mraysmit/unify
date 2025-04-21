package dev.mars.mapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test class for MappingConfiguration.
 * This class tests the functionality of the MappingConfiguration class.
 */
class MappingConfigurationTest {

    private MappingConfiguration config;

    @BeforeEach
    void setUp() {
        // Create a new MappingConfiguration instance for each test
        config = new MappingConfiguration();
    }

    @Test
    void testSetSourceLocation() {
        // Set a source location
        MappingConfiguration returnedConfig = config.setSourceLocation("data.csv");
        
        // Verify the source location was set correctly
        assertEquals("data.csv", config.getSourceLocation());
        
        // Verify method chaining works
        assertSame(config, returnedConfig);
    }

    @Test
    void testAddColumnMapping() {
        // Create a column mapping
        ColumnMapping mapping = new ColumnMapping("SourceColumn", "TargetColumn", "string");
        
        // Add the column mapping
        MappingConfiguration returnedConfig = config.addColumnMapping(mapping);
        
        // Verify the column mapping was added correctly
        List<ColumnMapping> mappings = config.getColumnMappings();
        assertEquals(1, mappings.size());
        assertSame(mapping, mappings.get(0));
        
        // Verify method chaining works
        assertSame(config, returnedConfig);
    }

    @Test
    void testAddMultipleColumnMappings() {
        // Create column mappings
        ColumnMapping mapping1 = new ColumnMapping("SourceColumn1", "TargetColumn1", "string");
        ColumnMapping mapping2 = new ColumnMapping("SourceColumn2", "TargetColumn2", "int");
        ColumnMapping mapping3 = new ColumnMapping(2, "TargetColumn3", "double");
        
        // Add the column mappings
        config.addColumnMapping(mapping1)
              .addColumnMapping(mapping2)
              .addColumnMapping(mapping3);
        
        // Verify the column mappings were added correctly
        List<ColumnMapping> mappings = config.getColumnMappings();
        assertEquals(3, mappings.size());
        assertSame(mapping1, mappings.get(0));
        assertSame(mapping2, mappings.get(1));
        assertSame(mapping3, mappings.get(2));
    }

    @Test
    void testSetOption() {
        // Set an option
        MappingConfiguration returnedConfig = config.setOption("hasHeaderRow", true);
        
        // Verify the option was set correctly
        Map<String, Object> options = config.getOptions();
        assertEquals(1, options.size());
        assertEquals(true, options.get("hasHeaderRow"));
        
        // Verify method chaining works
        assertSame(config, returnedConfig);
    }

    @Test
    void testSetMultipleOptions() {
        // Set multiple options
        config.setOption("hasHeaderRow", true)
              .setOption("delimiter", ",")
              .setOption("skipEmptyRows", true);
        
        // Verify the options were set correctly
        Map<String, Object> options = config.getOptions();
        assertEquals(3, options.size());
        assertEquals(true, options.get("hasHeaderRow"));
        assertEquals(",", options.get("delimiter"));
        assertEquals(true, options.get("skipEmptyRows"));
    }

    @Test
    void testGetOption() {
        // Set an option
        config.setOption("hasHeaderRow", true);
        
        // Get the option with a default value
        Object value1 = config.getOption("hasHeaderRow", false);
        Object value2 = config.getOption("nonExistentOption", "default");
        
        // Verify the correct values were returned
        assertEquals(true, value1);
        assertEquals("default", value2);
    }

    @Test
    void testCreateColumnDefinitions() {
        // Add column mappings
        config.addColumnMapping(new ColumnMapping("SourceName", "Name", "string"))
              .addColumnMapping(new ColumnMapping("SourceAge", "Age", "int"))
              .addColumnMapping(new ColumnMapping("SourceSalary", "Salary", "double"));
        
        // Create column definitions
        LinkedHashMap<String, String> columnDefs = config.createColumnDefinitions();
        
        // Verify the column definitions were created correctly
        assertEquals(3, columnDefs.size());
        assertEquals("string", columnDefs.get("Name"));
        assertEquals("int", columnDefs.get("Age"));
        assertEquals("double", columnDefs.get("Salary"));
    }

    @Test
    void testCreateColumnDefinitionsWithDuplicateTargetNames() {
        // Add column mappings with duplicate target names
        config.addColumnMapping(new ColumnMapping("SourceName1", "Name", "string"))
              .addColumnMapping(new ColumnMapping("SourceName2", "Name", "int")); // Same target name, different type
        
        // Create column definitions
        LinkedHashMap<String, String> columnDefs = config.createColumnDefinitions();
        
        // Verify the last mapping for a target name is used
        assertEquals(1, columnDefs.size());
        assertEquals("int", columnDefs.get("Name"));
    }

    @Test
    void testCompleteConfiguration() {
        // Create a complete configuration
        config.setSourceLocation("data.csv")
              .setOption("hasHeaderRow", true)
              .setOption("delimiter", ",")
              .addColumnMapping(new ColumnMapping("SourceName", "Name", "string"))
              .addColumnMapping(new ColumnMapping("SourceAge", "Age", "int"))
              .addColumnMapping(new ColumnMapping("SourceSalary", "Salary", "double").setDefaultValue("0.0"));
        
        // Verify the configuration is correct
        assertEquals("data.csv", config.getSourceLocation());
        
        Map<String, Object> options = config.getOptions();
        assertEquals(2, options.size());
        assertEquals(true, options.get("hasHeaderRow"));
        assertEquals(",", options.get("delimiter"));
        
        List<ColumnMapping> mappings = config.getColumnMappings();
        assertEquals(3, mappings.size());
        
        ColumnMapping nameMapping = mappings.get(0);
        assertEquals("SourceName", nameMapping.getSourceColumnName());
        assertEquals("Name", nameMapping.getTargetColumnName());
        assertEquals("string", nameMapping.getTargetColumnType());
        
        ColumnMapping ageMapping = mappings.get(1);
        assertEquals("SourceAge", ageMapping.getSourceColumnName());
        assertEquals("Age", ageMapping.getTargetColumnName());
        assertEquals("int", ageMapping.getTargetColumnType());
        
        ColumnMapping salaryMapping = mappings.get(2);
        assertEquals("SourceSalary", salaryMapping.getSourceColumnName());
        assertEquals("Salary", salaryMapping.getTargetColumnName());
        assertEquals("double", salaryMapping.getTargetColumnType());
        assertEquals("0.0", salaryMapping.getDefaultValue());
        
        LinkedHashMap<String, String> columnDefs = config.createColumnDefinitions();
        assertEquals(3, columnDefs.size());
        assertEquals("string", columnDefs.get("Name"));
        assertEquals("int", columnDefs.get("Age"));
        assertEquals("double", columnDefs.get("Salary"));
    }
}