# Unify

[![Java](https://img.shields.io/badge/Java-23-orange.svg)](https://openjdk.java.net/projects/jdk/23/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)](#)
[![Coverage](https://img.shields.io/badge/Coverage-JaCoCo-yellow.svg)](#)
[![Modules](https://img.shields.io/badge/Modules-5-blue.svg)](#modules)
[![Data Formats](https://img.shields.io/badge/Formats-CSV%20|%20JSON%20|%20XML%20|%20JDBC%20|%20REST-informational.svg)](#supported-data-formats)

**Unify** is a comprehensive Java data integration framework that provides a unified interface for working with diverse data sources and formats. Built on the adapter pattern, Unify enables seamless data transformation, mapping, and processing across multiple data formats including CSV, JSON, XML, databases (JDBC), REST APIs, and NoSQL databases.

## 🚀 Key Features

- **Universal Data Access**: Connect to multiple data sources through a unified table interface
- **Format Agnostic**: Support for CSV, JSON, XML, JDBC, REST APIs, and NoSQL databases
- **Flexible Mapping**: Configurable data mapping with serialization support (JSON, YAML, JDBC)
- **Performance Optimized**: Multiple table implementations optimized for different usage patterns
- **Modular Architecture**: Clean separation of concerns with dedicated modules for core, I/O, and integration
- **Comprehensive Testing**: Extensive test coverage with JUnit 5 and property-based testing
- **Production Ready**: Built with Java 23, Maven, and enterprise-grade logging

## 🏗️ Architecture Overview

Unify is built around a modular architecture that separates core functionality from I/O operations and provides flexible integration capabilities:

### Core Concepts

- **Table Interface (`ITable`)**: Central abstraction for tabular data with support for columns, rows, and type inference
- **Adapter Pattern**: Decouples table operations from specific data source implementations
- **Mapping Framework**: Configurable data transformation with support for multiple serialization formats
- **Connection Management**: Unified connection handling for databases, files, and web services

### Supported Data Formats

| Format | Read | Write | Features |
|--------|------|-------|----------|
| **CSV** | ✅ | ✅ | Header support, custom delimiters, encoding options |
| **JSON** | ✅ | ✅ | Nested objects, arrays, schema validation |
| **XML** | ✅ | ✅ | XPath queries, namespace support, schema validation |
| **JDBC** | ✅ | ✅ | Multiple databases (H2, SQLite, MySQL, PostgreSQL) |
| **REST APIs** | ✅ | ✅ | HTTP methods, authentication, response format detection |
| **NoSQL** | ✅ | ✅ | MongoDB support with document mapping |

## 📦 Modules

The Unify framework consists of five main modules:

### 1. **unify-core**
Core table implementations and fundamental data structures
- `TableCore`: Standard table implementation
- `OptimizedTableCore`: Performance-optimized for specific usage patterns
- `ProfiledOptimizedTableCore`: Includes performance profiling capabilities
- Collection utilities and exception handling

### 2. **unify-io-common**
Common I/O interfaces, adapters, and mapping framework
- Base adapter classes and interfaces
- Data source connection management
- Mapping configuration serialization (JSON, YAML, JDBC)
- Connection factories for different data sources

### 3. **unify-io-files**
File format specific implementations
- CSV, JSON, XML readers and writers
- JDBC database connectivity
- REST API client functionality
- NoSQL database support

### 4. **unify-integration**
Integration examples and demonstrations
- End-to-end workflow examples
- Database integration demos
- Performance benchmarks and testing utilities

### 5. **unify-coverage**
Code coverage reporting and analysis
- JaCoCo integration for comprehensive coverage reports
- Multi-module coverage aggregation

## 🚀 Quick Start

### Basic Table Operations

```java
// Create a table instance
ITable table = new TableCore("MyTable");

// Add columns
table.addColumn(new Column<>("id", "int"));
table.addColumn(new Column<>("name", "string"));
table.addColumn(new Column<>("value", "double"));

// Add data
Map<String, String> row = new HashMap<>();
row.put("id", "1");
row.put("name", "Sample");
row.put("value", "123.45");
table.addRow(row);
```

### CSV Integration

```java
// Read from CSV
ITable table = new TableCore();
CSVUtils.readFromCSV(table, "data.csv", true, false);

// Write to CSV
CSVUtils.writeToCSV(table, "output.csv", true);
```

### Database Integration

```java
// Create database connection
IDataSourceConnection dbConnection = DataSourceConnectionFactory
    .createDatabaseConnection("jdbc:h2:./demo", "sa", "");

// Create mapping configuration
MappingConfiguration config = new MappingConfiguration();
config.setSourceLocation("jdbc:h2:./demo");
config.addColumnMapping(new ColumnMapping("id", "ID", "int"));
config.addColumnMapping(new ColumnMapping("name", "NAME", "string"));

// Read data using JDBC adapter
JDBCTableAdapter adapter = new JDBCTableAdapter(table);
JDBCReader reader = new JDBCReader();
reader.readData(adapter, dbConnection, config.getOptions());
```

### REST API Integration

```java
// Create REST connection
IDataSourceConnection restConnection = DataSourceConnectionFactory
    .createRESTConnection("https://api.example.com/data");

// Configure request options
Map<String, Object> options = new HashMap<>();
options.put("method", "GET");
options.put("responseFormat", "json");

// Read data from REST API
RESTTableAdapter adapter = new RESTTableAdapter(table);
RESTReader reader = new RESTReader();
reader.readData(adapter, restConnection, options);
```

## 🛠️ Building and Testing

### Prerequisites
- Java 23 or higher
- Maven 3.8+

### Build Commands

```bash
# Build all modules
mvn clean compile

# Run tests
mvn test

# Generate coverage report
mvn verify

# Package all modules
mvn package
```

### Running Integration Tests

```bash
# Run specific integration tests
mvn test -Dtest=CSVToSQLiteDemoTest
mvn test -Dtest=CSVToH2DemoTest

# Run all integration tests
cd unify-integration
mvn test
```

## 📊 Performance Considerations

Unify provides multiple table implementations optimized for different scenarios:

- **`TableCore`**: General-purpose implementation suitable for most use cases
- **`OptimizedTableCore`**: Optimized for specific read/write patterns with appropriate collection types
- **`ProfiledOptimizedTableCore`**: Includes performance monitoring for bottleneck identification

Choose the appropriate implementation based on your specific performance requirements and usage patterns.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🔗 Related Documentation

- [Functional Design](docs/FUNCTIONAL_DESIGN.md) — draft product scope, workflows, requirements, and acceptance scenarios
- [UI/UX Mockup](docs/UI_MOCKUP.md) — local interactive HTML/JavaScript design for the main workflows
- [Technical Design](docs/TECHNICAL_DESIGN.md) — draft implementation design derived from the functional requirements
- [Unify Concepts Guide](docs/UNIFY_CONCEPTS_README.md)
- [Performance Considerations](docs/Are_there_any_performance_considerations.md)
- [Module-specific READMEs](unify-io-common/src/main/java/dev/mars/jtable/io/common/adapter/README.MD)

---

**Unify** - Bringing unity to diverse data sources through elegant Java abstractions.
