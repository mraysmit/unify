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
package dev.mars.jtable.io.common.mapping;

import dev.mars.jtable.io.common.datasource.DbConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the mapping serialization framework.
 * This class tests the JSON, YAML, and JDBC serializers.
 */
class MappingSerializerTest {

    private MappingConfiguration config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a sample mapping configuration for testing
        config = new MappingConfiguration();
        config.setSourceLocation("jdbc:h2:mem:test");

        // Add some column mappings
        config.addColumnMapping(new ColumnMapping("id", "ID", "int"));
        config.addColumnMapping(new ColumnMapping("name", "NAME", "string"));
        config.addColumnMapping(new ColumnMapping("age", "AGE", "int"));
        config.addColumnMapping(new ColumnMapping("email", "EMAIL", "string"));

        // Add some options
        config.setOption("tableName", "users");
        config.setOption("createTable", true);
        config.setOption("batchSize", 100);
    }

    @Test
    void testJSONSerialization() throws IOException {
        // Create a JSON serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("json");

        // Serialize the configuration to a string
        String json = serializer.serialize(config);

        // Verify that the JSON string contains expected values
        assertTrue(json.contains("\"sourceLocation\""));
        assertTrue(json.contains("jdbc:h2:mem:test"));
        assertTrue(json.contains("\"columnMappings\""));
        assertTrue(json.contains("\"options\""));
        assertTrue(json.contains("\"tableName\""));
        assertTrue(json.contains("\"users\""));

        // Deserialize the JSON string back to a configuration
        MappingConfiguration deserializedConfig = serializer.deserialize(json);

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());
        assertEquals(config.getOption("tableName", null), deserializedConfig.getOption("tableName", null));
        assertEquals(config.getOption("createTable", false), deserializedConfig.getOption("createTable", false));
        assertEquals(config.getOption("batchSize", 0), deserializedConfig.getOption("batchSize", 0));
    }

    @Test
    void testYAMLSerialization() throws IOException {
        // Create a YAML serializer
        IMappingSerializer serializer = MappingSerializerFactory.createSerializer("yaml");

        // Serialize the configuration to a string
        String yaml = serializer.serialize(config);

        // Verify that the YAML string contains expected values
        assertTrue(yaml.contains("sourceLocation:"));
        assertTrue(yaml.contains("jdbc:h2:mem:test"));
        assertTrue(yaml.contains("columnMappings:"));
        assertTrue(yaml.contains("options:"));
        assertTrue(yaml.contains("tableName:"));
        assertTrue(yaml.contains("users"));

        // Deserialize the YAML string back to a configuration
        MappingConfiguration deserializedConfig = serializer.deserialize(yaml);

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());
        assertEquals(config.getOption("tableName", null), deserializedConfig.getOption("tableName", null));
        assertEquals(config.getOption("createTable", false), deserializedConfig.getOption("createTable", false));
        assertEquals(config.getOption("batchSize", 0), deserializedConfig.getOption("batchSize", 0));
    }

    @Test
    void testFileWriteAndRead() throws IOException {
        // Create a JSON serializer
        IMappingSerializer jsonSerializer = MappingSerializerFactory.createSerializer("json");

        // Create a file path
        File jsonFile = tempDir.resolve("config.json").toFile();

        // Write the configuration to the file
        jsonSerializer.writeToFile(config, jsonFile.getAbsolutePath());

        // Verify that the file exists
        assertTrue(jsonFile.exists());

        // Read the configuration from the file
        MappingConfiguration deserializedConfig = jsonSerializer.readFromFile(jsonFile.getAbsolutePath());

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());

        // Create a YAML serializer
        IMappingSerializer yamlSerializer = MappingSerializerFactory.createSerializer("yaml");

        // Create a file path
        File yamlFile = tempDir.resolve("config.yaml").toFile();

        // Write the configuration to the file
        yamlSerializer.writeToFile(config, yamlFile.getAbsolutePath());

        // Verify that the file exists
        assertTrue(yamlFile.exists());

        // Read the configuration from the file
        deserializedConfig = yamlSerializer.readFromFile(yamlFile.getAbsolutePath());

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());
    }

    @Test
    void testFileWriteAndReadWithStandardLocation() throws IOException {
        // Create the mappings directory if it doesn't exist
        Path mappingsDir = Paths.get("java-table-io-common", "src", "test", "resources", "mappings");
        if (!Files.exists(mappingsDir)) {
            Files.createDirectories(mappingsDir);
        }

        // Create a JSON serializer
        IMappingSerializer jsonSerializer = MappingSerializerFactory.createSerializer("json");

        // Create a file path using the standard location
        String jsonFileName = "test-config.json";
        String jsonFilePath = mappingsDir.resolve(jsonFileName).toString();

        // Write the configuration to the file
        jsonSerializer.writeToFile(config, jsonFilePath);

        // Verify that the file exists
        assertTrue(Files.exists(Paths.get(jsonFilePath)));

        // Read the configuration from the file
        MappingConfiguration deserializedConfig = jsonSerializer.readFromFile(jsonFilePath);

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());

        // Create a YAML serializer
        IMappingSerializer yamlSerializer = MappingSerializerFactory.createSerializer("yaml");

        // Create a file path using the standard location
        String yamlFileName = "test-config.yaml";
        String yamlFilePath = mappingsDir.resolve(yamlFileName).toString();

        // Write the configuration to the file
        yamlSerializer.writeToFile(config, yamlFilePath);

        // Verify that the file exists
        assertTrue(Files.exists(Paths.get(yamlFilePath)));

        // Read the configuration from the file
        deserializedConfig = yamlSerializer.readFromFile(yamlFilePath);

        // Verify that the deserialized configuration matches the original
        assertEquals(config.getSourceLocation(), deserializedConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), deserializedConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), deserializedConfig.getOptions().size());

        // Clean up
        Files.deleteIfExists(Paths.get(jsonFilePath));
        Files.deleteIfExists(Paths.get(yamlFilePath));
    }

    @Test
    void testSerializerFactory() {
        // Test creating a serializer by format
        IMappingSerializer jsonSerializer = MappingSerializerFactory.createSerializer("json");
        assertEquals("json", jsonSerializer.getFormat());
        assertTrue(jsonSerializer instanceof JSONMappingSerializer);

        IMappingSerializer yamlSerializer = MappingSerializerFactory.createSerializer("yaml");
        assertEquals("yaml", yamlSerializer.getFormat());
        assertTrue(yamlSerializer instanceof YAMLMappingSerializer);

        IMappingSerializer ymlSerializer = MappingSerializerFactory.createSerializer("yml");
        assertEquals("yaml", ymlSerializer.getFormat());
        assertTrue(ymlSerializer instanceof YAMLMappingSerializer);

        // Test creating a serializer by file path
        IMappingSerializer jsonFileSerializer = MappingSerializerFactory.createSerializerForFile("config.json");
        assertEquals("json", jsonFileSerializer.getFormat());
        assertTrue(jsonFileSerializer instanceof JSONMappingSerializer);

        IMappingSerializer yamlFileSerializer = MappingSerializerFactory.createSerializerForFile("config.yaml");
        assertEquals("yaml", yamlFileSerializer.getFormat());
        assertTrue(yamlFileSerializer instanceof YAMLMappingSerializer);

        IMappingSerializer ymlFileSerializer = MappingSerializerFactory.createSerializerForFile("config.yml");
        assertEquals("yaml", ymlFileSerializer.getFormat());
        assertTrue(ymlFileSerializer instanceof YAMLMappingSerializer);

        // Test creating a JDBC serializer
        JDBCMappingSerializer jdbcSerializerRaw = MappingSerializerFactory.createJDBCSerializer("jdbc:h2:mem:test", "sa", "");
        assertEquals("jdbc", jdbcSerializerRaw.getFormat());
        assertTrue(jdbcSerializerRaw instanceof JDBCMappingSerializer);

        // Test creating a JDBC serializer
        JDBCMappingSerializer jdbcSerializer = MappingSerializerFactory.createJDBCSerializer(new DbConnection("jdbc:h2:mem:test", "sa", ""));
        assertEquals("jdbc", jdbcSerializer.getFormat());
        assertTrue(jdbcSerializer instanceof JDBCMappingSerializer);

        // Test invalid format
        assertThrows(IllegalArgumentException.class, () -> MappingSerializerFactory.createSerializer("invalid"));

        // Test invalid file path
        assertThrows(IllegalArgumentException.class, () -> MappingSerializerFactory.createSerializerForFile("config"));
        assertThrows(IllegalArgumentException.class, () -> MappingSerializerFactory.createSerializerForFile("config.invalid"));
    }

    /**
     * Test for JDBC serializer database operations.
     * This test was previously commented out because it required the H2 database driver,
     * which is now added as a dependency in the pom.xml file.
     */
    @Test
    void testJDBCSerializerDatabaseOperations() throws IOException {
        // Create a JDBC serializer with an in-memory H2 database
        JDBCMappingSerializer jdbcSerializer = MappingSerializerFactory.createJDBCSerializer(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");

        // Test writing a configuration to the database
        String id = "test-config";
        String name = "Test Configuration";
        jdbcSerializer.writeToDatabase(id, name, config);

        // Test reading the configuration from the database
        MappingConfiguration readConfig = jdbcSerializer.readFromDatabase(id);
        assertNotNull(readConfig);
        assertEquals(config.getSourceLocation(), readConfig.getSourceLocation());
        assertEquals(config.getColumnMappings().size(), readConfig.getColumnMappings().size());
        assertEquals(config.getOptions().size(), readConfig.getOptions().size());
        assertEquals(config.getOption("tableName", null), readConfig.getOption("tableName", null));
        assertEquals(config.getOption("createTable", false), readConfig.getOption("createTable", false));
        assertEquals(config.getOption("batchSize", 0), readConfig.getOption("batchSize", 0));

        // Test writing another configuration to the database
        MappingConfiguration config2 = new MappingConfiguration();
        config2.setSourceLocation("jdbc:h2:mem:test2");
        config2.addColumnMapping(new ColumnMapping("id", "ID", "int"));
        config2.addColumnMapping(new ColumnMapping("name", "NAME", "string"));
        config2.setOption("tableName", "products");

        String id2 = "test-config2";
        String name2 = "Test Configuration 2";
        jdbcSerializer.writeToDatabase(id2, name2, config2);

        // Test reading all configurations from the database
        Map<String, MappingConfiguration> allConfigs = jdbcSerializer.readAllFromDatabase();
        assertEquals(2, allConfigs.size());
        assertTrue(allConfigs.containsKey(id));
        assertTrue(allConfigs.containsKey(id2));

        // Verify the second configuration
        MappingConfiguration readConfig2 = allConfigs.get(id2);
        assertEquals(config2.getSourceLocation(), readConfig2.getSourceLocation());
        assertEquals(config2.getColumnMappings().size(), readConfig2.getColumnMappings().size());
        assertEquals(config2.getOptions().size(), readConfig2.getOptions().size());
        assertEquals(config2.getOption("tableName", null), readConfig2.getOption("tableName", null));
    }

    @Test
    void testJDBCSerializerUnsupportedOperations() {
        // Create a JDBC serializer
        JDBCMappingSerializer jdbcSerializer = MappingSerializerFactory.createJDBCSerializer(
                "jdbc:h2:mem:testdb2;DB_CLOSE_DELAY=-1", "sa", "");

        // Test that writing to a file throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, 
                () -> jdbcSerializer.writeToFile(config, "test.json"));

        // Test that reading from a file throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, 
                () -> jdbcSerializer.readFromFile("test.json"));

        // Test that writing to an output stream throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, 
                () -> jdbcSerializer.write(config, null));

        // Test that reading from an input stream throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, 
                () -> jdbcSerializer.read(null));
    }
}
