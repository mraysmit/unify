package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.IMappingSerializer;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.MappingSerializerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demonstration of MappingConfiguration usage including serialization and deserialization.
 * This class shows how to:
 * 1. Create a MappingConfiguration object
 * 2. Add column mappings
 * 3. Set options
 * 4. Serialize the configuration to JSON
 * 5. Deserialize the configuration from JSON
 * 6. Use the configuration with DataSourceTableAdapter
 */
public class MappingConfigurationDemo {

    /**
     * Main method to run the demonstration.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            System.out.println("Starting MappingConfiguration demonstration");

            // Create a MappingConfiguration
            MappingConfiguration config = createMappingConfiguration();
            System.out.println("Created MappingConfiguration with " + config.getColumnMappings().size() + " column mappings");

            // Serialize the configuration to JSON
            String jsonConfig = serializeToJson(config);
            System.out.println("Serialized MappingConfiguration to JSON:");
            System.out.println(jsonConfig);

            // Save the configuration to a file
            String jsonFilePath = "mapping_config_demo.json";
            saveToJsonFile(config, jsonFilePath);
            System.out.println("Saved MappingConfiguration to file: " + jsonFilePath);

            // Load the configuration from the file
            MappingConfiguration loadedConfig = loadFromJsonFile(jsonFilePath);
            System.out.println("Loaded MappingConfiguration from file with " + 
                    loadedConfig.getColumnMappings().size() + " column mappings");

            // Use the configuration with DataSourceTableAdapter
            useWithDataSourceTableAdapter(loadedConfig);

            // Clean up the demo file
            boolean deleted = new File(jsonFilePath).delete();
            System.out.println("Deleted demo file: " + jsonFilePath + " (success: " + deleted + ")");

            System.out.println("MappingConfiguration demonstration completed successfully");
        } catch (Exception e) {
            System.err.println("Error in demonstration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a sample MappingConfiguration.
     * This method demonstrates how to create a MappingConfiguration, add column mappings, and set options.
     *
     * @return the created MappingConfiguration
     */
    private static MappingConfiguration createMappingConfiguration() {
        // Create a new MappingConfiguration
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("sample_data.csv")
                .setOption("hasHeaderRow", true)
                .setOption("delimiter", ",")
                .setOption("quoteChar", "\"")
                .setOption("escapeChar", "\\")
                .setOption("skipEmptyRows", true)
                .setOption("trimValues", true);

        // Add column mappings
        config.addColumnMapping(new ColumnMapping("id", "personId", "int")
                        .setDefaultValue("0"))
                .addColumnMapping(new ColumnMapping("first_name", "firstName", "string")
                        .setDefaultValue(""))
                .addColumnMapping(new ColumnMapping("last_name", "lastName", "string")
                        .setDefaultValue(""))
                .addColumnMapping(new ColumnMapping("email", "emailAddress", "string")
                        .setDefaultValue("no-email@example.com"))
                .addColumnMapping(new ColumnMapping("age", "age", "int")
                        .setDefaultValue("0"))
                .addColumnMapping(new ColumnMapping("salary", "salary", "double")
                        .setDefaultValue("0.0"))
                .addColumnMapping(new ColumnMapping("is_active", "active", "boolean")
                        .setDefaultValue("false"));

        return config;
    }

    /**
     * Serializes a MappingConfiguration to JSON.
     * This method demonstrates how to use the MappingSerializerFactory to create a JSON serializer
     * and serialize a MappingConfiguration to a JSON string.
     *
     * @param config the MappingConfiguration to serialize
     * @return the JSON string representation of the MappingConfiguration
     * @throws IOException if there is an error during serialization
     */
    private static String serializeToJson(MappingConfiguration config) throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Serialize the configuration to JSON
        return serializer.serialize(config);
    }

    /**
     * Saves a MappingConfiguration to a JSON file.
     * This method demonstrates how to use the MappingSerializerFactory to create a JSON serializer
     * and save a MappingConfiguration to a file.
     *
     * @param config the MappingConfiguration to save
     * @param filePath the path to the file to save to
     * @throws IOException if there is an error saving the configuration
     */
    private static void saveToJsonFile(MappingConfiguration config, String filePath) throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Write the configuration to the file
        serializer.writeToFile(config, filePath);
    }

    /**
     * Loads a MappingConfiguration from a JSON file.
     * This method demonstrates how to use the MappingSerializerFactory to create a JSON serializer
     * and load a MappingConfiguration from a file.
     *
     * @param filePath the path to the file to load from
     * @return the loaded MappingConfiguration
     * @throws IOException if there is an error loading the configuration
     */
    private static MappingConfiguration loadFromJsonFile(String filePath) throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Read the configuration from the file
        return serializer.readFromFile(filePath);
    }

    /**
     * Uses a MappingConfiguration with a DataSourceTableAdapter.
     * This method demonstrates how to create a Table, wrap it in a DataSourceTableAdapter,
     * and use it with a MappingConfiguration.
     *
     * @param config the MappingConfiguration to use
     */
    private static void useWithDataSourceTableAdapter(MappingConfiguration config) {
        // Create a new Table
        ITable table = new TableCore();

        // Set up columns for the table based on the mapping configuration
        LinkedHashMap<String, String> columns = config.createColumnDefinitions();
        table.setColumns(columns);
        System.out.println("Set up table with " + columns.size() + " columns");

        // Create the adapter with the table
        DataSourceTableAdapter adapter = new DataSourceTableAdapter(table);
        System.out.println("Created DataSourceTableAdapter");

        // Add some sample data to the table
        addSampleData(adapter);
        System.out.println("Added " + adapter.getRowCount() + " rows to the table");

        // Display the table data
        displayTableData(adapter);
    }

    /**
     * Adds sample data to a DataSourceTableAdapter.
     * This method demonstrates how to add rows to a DataSourceTableAdapter.
     *
     * @param adapter the DataSourceTableAdapter to add data to
     */
    private static void addSampleData(DataSourceTableAdapter adapter) {
        // Add a row with all fields
        Map<String, String> row1 = new HashMap<>();
        row1.put("personId", "1");
        row1.put("firstName", "John");
        row1.put("lastName", "Doe");
        row1.put("emailAddress", "john.doe@example.com");
        row1.put("age", "30");
        row1.put("salary", "75000.50");
        row1.put("active", "true");
        adapter.addRow(row1);

        // Add a row with some missing fields (will use default values)
        Map<String, String> row2 = new HashMap<>();
        row2.put("personId", "2");
        row2.put("firstName", "Jane");
        row2.put("lastName", "Smith");
        row2.put("age", "25");
        adapter.addRow(row2);
    }

    /**
     * Displays the data in a DataSourceTableAdapter.
     * This method demonstrates how to access data in a DataSourceTableAdapter.
     *
     * @param adapter the DataSourceTableAdapter to display data from
     */
    private static void displayTableData(DataSourceTableAdapter adapter) {
        System.out.println("\nTable Data:");
        
        // Display column headers
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < adapter.getColumnCount(); i++) {
            if (i > 0) header.append(" | ");
            header.append(adapter.getColumnName(i));
        }
        System.out.println(header.toString());
        
        // Display separator
        System.out.println("-".repeat(header.length()));
        
        // Display rows
        for (int row = 0; row < adapter.getRowCount(); row++) {
            StringBuilder rowData = new StringBuilder();
            for (int col = 0; col < adapter.getColumnCount(); col++) {
                if (col > 0) rowData.append(" | ");
                rowData.append(adapter.getValueAt(row, adapter.getColumnName(col)));
            }
            System.out.println(rowData.toString());
        }
        System.out.println();
    }
}