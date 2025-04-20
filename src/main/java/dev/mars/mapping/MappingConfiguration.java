package dev.mars.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for mapping data from a source to a Table.
 * This class defines how data should be read from a source and mapped to a Table.
 */
public class MappingConfiguration {
    private String sourceLocation;
    private List<ColumnMapping> columnMappings;
    private Map<String, Object> options;

    /**
     * Creates a new MappingConfiguration.
     */
    public MappingConfiguration() {
        this.columnMappings = new ArrayList<>();
        this.options = new LinkedHashMap<>();
    }

    /**
     * Gets the source location.
     *
     * @return the source location
     */
    public String getSourceLocation() {
        return sourceLocation;
    }

    /**
     * Sets the source location.
     *
     * @param sourceLocation the source location
     * @return this MappingConfiguration for method chaining
     */
    public MappingConfiguration setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
        return this;
    }

    /**
     * Gets the column mappings.
     *
     * @return the column mappings
     */
    public List<ColumnMapping> getColumnMappings() {
        return columnMappings;
    }

    /**
     * Adds a column mapping.
     *
     * @param columnMapping the column mapping to add
     * @return this MappingConfiguration for method chaining
     */
    public MappingConfiguration addColumnMapping(ColumnMapping columnMapping) {
        this.columnMappings.add(columnMapping);
        return this;
    }

    /**
     * Gets the options.
     *
     * @return the options
     */
    public Map<String, Object> getOptions() {
        return options;
    }

    /**
     * Sets an option.
     *
     * @param key the option key
     * @param value the option value
     * @return this MappingConfiguration for method chaining
     */
    public MappingConfiguration setOption(String key, Object value) {
        this.options.put(key, value);
        return this;
    }

    /**
     * Gets an option.
     *
     * @param key the option key
     * @param defaultValue the default value to return if the option is not set
     * @return the option value, or the default value if the option is not set
     */
    public Object getOption(String key, Object defaultValue) {
        return this.options.getOrDefault(key, defaultValue);
    }

    /**
     * Creates a LinkedHashMap of column names to column types based on the column mappings.
     *
     * @return a LinkedHashMap of column names to column types
     */
    public LinkedHashMap<String, String> createColumnDefinitions() {
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (ColumnMapping mapping : columnMappings) {
            columns.put(mapping.getTargetColumnName(), mapping.getTargetColumnType());
        }
        return columns;
    }
}