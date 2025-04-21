# Table Adapter Pattern

This package contains adapter classes that connect data sources to Table instances. The adapter pattern is used to decouple the Table class from the data source interfaces, allowing the Table class to focus on its core functionality without being concerned with how data is read from or written to various data sources.

## Adapter Interfaces

- `ITableAdapter`: The base interface for all adapters. It defines the common method `getTable()` that returns the Table instance that the adapter is connected to.

## Adapter Implementations

- `DataSourceTableAdapter`: Adapts a Table instance to the generic `IDataSource` interface.
- `CSVTableAdapter`: Adapts a Table instance to the CSV-specific `ICSVDataSource` interface.
- `JSONTableAdapter`: Adapts a Table instance to the JSON-specific `IJSONDataSource` interface.
- `XMLTableAdapter`: Adapts a Table instance to the XML-specific `IXMLDataSource` interface.
- `JDBCTableAdapter`: Adapts a Table instance to the JDBC-specific `IJDBCDataSource` interface.

## Usage

To use an adapter, create an instance of the appropriate adapter class and pass it to the data source reader or writer:

```java
// Create a Table instance
Table table = new Table();

// Create an adapter for the Table instance
CSVTableAdapter adapter = new CSVTableAdapter(table);

// Use the adapter with a CSV reader or writer
csvReader.readFromCSV(adapter, "data.csv", true, false);
csvWriter.writeToCSV(adapter, "output.csv", true);
```

Alternatively, you can use the `CSVUtils` class, which creates the adapter for you:

```java
// Create a Table instance
Table table = new Table();

// Use CSVUtils to read from or write to a CSV file
CSVUtils.readFromCSV(table, "data.csv", true, false);
CSVUtils.writeToCSV(table, "output.csv", true);
```

## Benefits

- **Decoupling**: The Table class is decoupled from the data source interfaces, allowing it to focus on its core functionality.
- **Flexibility**: New data source types can be added without modifying the Table class.
- **Testability**: The Table class and the data source interfaces can be tested independently.
- **Maintainability**: Changes to the data source interfaces or the Table class don't affect each other.