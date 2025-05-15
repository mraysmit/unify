# Mapping Serialization Framework

This package provides a framework for serializing and deserializing mapping configurations to and from different formats (JSON, YAML) and data sources (JDBC).

## Overview

The framework consists of the following components:

1. `IMappingSerializer` - Interface that defines methods for serializing and deserializing mapping configurations
2. `AbstractMappingSerializer` - Abstract base class that implements common functionality
3. `JSONMappingSerializer` - Implementation for JSON serialization
4. `YAMLMappingSerializer` - Implementation for YAML serialization
5. `JDBCMappingSerializer` - Implementation for JDBC serialization
6. `MappingSerializerFactory` - Factory for creating serializers based on format or file extension

## Usage

### Creating a Serializer

You can create a serializer using the factory:

```java
// Create a serializer by format
IMappingSerializer jsonSerializer = MappingSerializerFactory.createSerializer("json");
IMappingSerializer yamlSerializer = MappingSerializerFactory.createSerializer("yaml");

// Create a serializer by file path
IMappingSerializer jsonFileSerializer = MappingSerializerFactory.createSerializerForFile("config.json");
IMappingSerializer yamlFileSerializer = MappingSerializerFactory.createSerializerForFile("config.yaml");

// Create a JDBC serializer
JDBCMappingSerializer jdbcSerializer = MappingSerializerFactory.createJDBCSerializer(
    "jdbc:h2:mem:test", "username", "password");
```

### Serializing and Deserializing

```java
// Create a mapping configuration
MappingConfiguration config = new MappingConfiguration();
config.setSourceLocation("jdbc:h2:mem:test");
config.addColumnMapping(new ColumnMapping("id", "ID", "int"));
config.addColumnMapping(new ColumnMapping("name", "NAME", "string"));
config.setOption("tableName", "users");

// Serialize to a string
String json = jsonSerializer.serialize(config);
String yaml = yamlSerializer.serialize(config);

// Deserialize from a string
MappingConfiguration jsonConfig = jsonSerializer.deserialize(json);
MappingConfiguration yamlConfig = yamlSerializer.deserialize(yaml);

// Write to a file
jsonSerializer.writeToFile(config, "config.json");
yamlSerializer.writeToFile(config, "config.yaml");

// Read from a file
MappingConfiguration jsonFileConfig = jsonSerializer.readFromFile("config.json");
MappingConfiguration yamlFileConfig = yamlSerializer.readFromFile("config.yaml");

// Write to a database
jdbcSerializer.writeToDatabase("config1", "My Configuration", config);

// Read from a database
MappingConfiguration dbConfig = jdbcSerializer.readFromDatabase("config1");

// Read all configurations from a database
Map<String, MappingConfiguration> allConfigs = jdbcSerializer.readAllFromDatabase();
```

## Extending the Framework

You can extend the framework by implementing the `IMappingSerializer` interface or extending the `AbstractMappingSerializer` class. For example, to add support for a new format:

```java
public class XMLMappingSerializer extends AbstractMappingSerializer {
    @Override
    public String getFormat() {
        return "xml";
    }
    
    @Override
    public String serialize(MappingConfiguration config) throws IOException {
        // Implement XML serialization
    }
    
    @Override
    public MappingConfiguration deserialize(String content) throws IOException {
        // Implement XML deserialization
    }
}
```

Then update the factory to support the new format:

```java
public static IMappingSerializer createSerializer(String format) {
    // ...
    switch (format) {
        case "json":
            return new JSONMappingSerializer();
        case "yaml":
        case "yml":
            return new YAMLMappingSerializer();
        case "xml":
            return new XMLMappingSerializer();
        default:
            throw new IllegalArgumentException("Unsupported format: " + format);
    }
}
```