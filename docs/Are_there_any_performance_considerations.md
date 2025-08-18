# Unify Framework Design Considerations 



# Performance Considerations When Using Concurrent Collections

Concurrent collections in Java (such as `ConcurrentHashMap`, `CopyOnWriteArrayList`, and others in the `java.util.concurrent` package) are designed for thread-safe operations without explicit synchronization. While they provide thread safety, they come with several performance considerations:

## 1. Overhead vs. Traditional Collections

- **Synchronization Mechanisms**: Concurrent collections use various synchronization techniques that add overhead compared to non-concurrent collections.
- **Lock Contention**: Under high concurrency, threads may wait for locks, causing performance degradation.

## 2. Specific Collection Performance Characteristics

### ConcurrentHashMap
- Uses lock striping (multiple locks for different segments) to reduce contention
- Read operations are generally very fast and don't block
- Write operations only lock a small portion of the map
- Better performance than `Hashtable` or `Collections.synchronizedMap()`

### CopyOnWriteArrayList/Set
- Creates a new copy of the underlying array for every modification
- Excellent for read-heavy workloads with infrequent modifications
- Very expensive for write-heavy scenarios
- No contention for reads, but high memory usage during writes

### ConcurrentLinkedQueue/Deque
- Non-blocking algorithms using atomic operations
- Good throughput but potentially higher latency than blocking alternatives
- Performs well under high contention

## 3. Memory Considerations

- **Memory Overhead**: Concurrent collections typically use more memory than their non-concurrent counterparts.
- **Garbage Collection**: Collections like `CopyOnWriteArrayList` create garbage with each modification, potentially triggering more frequent GC cycles.

## 4. Choosing the Right Collection

- **Read-Write Ratio**: For read-heavy workloads, concurrent collections excel. For write-heavy workloads, consider alternatives.
- **Contention Level**: Higher contention benefits from non-blocking algorithms or fine-grained locking.
- **Iteration Requirements**: Some concurrent collections provide weakly consistent iterators that may not reflect all updates.

## 5. Optimization Strategies

- **Sizing**: Proper initial sizing reduces resizing operations, which can be expensive in concurrent contexts.
- **Batching**: Grouping operations can reduce contention and improve throughput.
- **Partitioning**: Dividing work across multiple collections can reduce contention points.

## 6. Monitoring and Tuning

- **Profiling**: Use profiling tools to identify bottlenecks in concurrent collection usage.
- **Benchmarking**: Test different collection types with realistic workloads to find the best fit.

In summary, concurrent collections trade some performance for thread safety. The right choice depends on your specific access patterns, contention levels, and memory constraints.



# JDBC Mapping Implementation

Based on the examination of the CSVMappingReader and CSVMappingWriter classes, I'll implement equivalent functionality in the JDBC package using MappingConfiguration. This will allow for more flexible mapping between tables and databases.

## Implementation Plan

I'll create two new classes:
1. `JDBCMappingReader` - For reading data from databases with column mapping
2. `JDBCMappingWriter` - For writing data to databases with column mapping

## JDBCMappingReader Implementation

```java
package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.MappingConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reader for database tables using a mapping configuration.
 * This class reads data from a database table according to a mapping configuration.
 */
public class JDBCMappingReader {
    /**
     * Reads data from a database into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     * @throws IllegalArgumentException if table or config is null, or if config has invalid settings
     * @throws SQLException if there is an error reading from the database
     */
    public void readFromDatabase(ITable table, MappingConfiguration config) throws SQLException {
        // Validate input parameters
        if (table == null) {
            String errorMsg = "Table cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (config == null) {
            String errorMsg = "Mapping configuration cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        String connectionString = config.getSourceLocation();
        if (connectionString == null || connectionString.trim().isEmpty()) {
            String errorMsg = "Source location (connection string) in mapping configuration cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate column mappings
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            String errorMsg = "Column mappings cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Extract database options
        String tableName = (String) config.getOption("tableName", null);
        String query = (String) config.getOption("query", null);
        String username = (String) config.getOption("username", "");
        String password = (String) config.getOption("password", "");

        if (tableName == null && query == null) {
            String errorMsg = "Either 'tableName' or 'query' must be specified in options";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Set up the table columns based on the mapping configuration
        table.setColumns(config.createColumnDefinitions());

        try (Connection connection = DriverManager.getConnection(connectionString, username, password)) {
            // Prepare the query
            String sql;
            if (query != null) {
                sql = query;
            } else {
                sql = "SELECT * FROM " + tableName;
            }

            try (PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Create a map of database column names to indices
                Map<String, Integer> dbColumnIndices = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    dbColumnIndices.put(metaData.getColumnName(i).toUpperCase(), i);
                }

                // Process all rows in the result set
                while (resultSet.next()) {
                    Map<String, String> rowValues = new HashMap<>();

                    // For each column mapping, get the value from the result set
                    for (ColumnMapping mapping : columnMappings) {
                        String targetColumnName = mapping.getTargetColumnName();
                        String value = null;

                        if (mapping.usesSourceColumnName()) {
                            String sourceColumnName = mapping.getSourceColumnName();
                            if (sourceColumnName != null && !sourceColumnName.isEmpty()) {
                                // Look up the column index in the result set
                                Integer columnIndex = dbColumnIndices.get(sourceColumnName.toUpperCase());
                                if (columnIndex != null) {
                                    value = resultSet.getString(columnIndex);
                                } else {
                                    System.err.println("Warning: Source column '" + sourceColumnName + "' not found in result set");
                                }
                            }
                        } else if (mapping.usesSourceColumnIndex()) {
                            int sourceColumnIndex = mapping.getSourceColumnIndex();
                            if (sourceColumnIndex >= 0 && sourceColumnIndex < columnCount) {
                                value = resultSet.getString(sourceColumnIndex + 1);
                            } else {
                                System.err.println("Warning: Source column index " + sourceColumnIndex + 
                                    " is out of bounds (0-" + (columnCount - 1) + ")");
                            }
                        }

                        // Use default value if the value is null or empty
                        if (value == null || value.isEmpty()) {
                            value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                        }

                        rowValues.put(targetColumnName, value);
                    }

                    // Add the row to the table
                    table.addRow(rowValues);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error reading from database: " + e.getMessage());
            throw e;
        }
    }
}
```

## JDBCMappingWriter Implementation

```java
package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.MappingConfiguration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Writer for database tables using a mapping configuration.
 * This class writes data to a database table according to a mapping configuration.
 */
public class JDBCMappingWriter {
    /**
     * Writes data from a table to a database according to a mapping configuration.
     *
     * @param table the table to write from
     * @param config the mapping configuration
     * @throws IllegalArgumentException if table or config is null, or if config has invalid settings
     * @throws SQLException if there is an error writing to the database
     */
    public void writeToDatabase(ITable table, MappingConfiguration config) throws SQLException {
        // Validate input parameters
        if (table == null) {
            String errorMsg = "Table cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        if (config == null) {
            String errorMsg = "Mapping configuration cannot be null";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        String connectionString = config.getSourceLocation();
        if (connectionString == null || connectionString.trim().isEmpty()) {
            String errorMsg = "Source location (connection string) in mapping configuration cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Validate column mappings
        List<ColumnMapping> columnMappings = config.getColumnMappings();
        if (columnMappings == null || columnMappings.isEmpty()) {
            String errorMsg = "Column mappings cannot be null or empty";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Extract database options
        String tableName = (String) config.getOption("tableName", null);
        String username = (String) config.getOption("username", "");
        String password = (String) config.getOption("password", "");
        boolean createTable = (boolean) config.getOption("createTable", false);

        if (tableName == null) {
            String errorMsg = "'tableName' must be specified in options";
            System.err.println(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        try (Connection connection = DriverManager.getConnection(connectionString, username, password)) {
            // Create the table if requested
            if (createTable) {
                createTable(connection, tableName, table, columnMappings);
            }

            // Generate the INSERT statement
            StringBuilder insertSql = new StringBuilder("INSERT INTO " + tableName + " (");
            StringBuilder placeholders = new StringBuilder(") VALUES (");

            for (int i = 0; i < columnMappings.size(); i++) {
                ColumnMapping mapping = columnMappings.get(i);
                String sourceColumnName = mapping.usesSourceColumnName() ? mapping.getSourceColumnName() : null;
                
                // For the database column, we use the target column name from the mapping
                String dbColumnName = mapping.getTargetColumnName();
                
                insertSql.append(dbColumnName);
                placeholders.append("?");

                if (i < columnMappings.size() - 1) {
                    insertSql.append(", ");
                    placeholders.append(", ");
                }
            }
            insertSql.append(placeholders).append(")");

            // Insert the data
            try (PreparedStatement statement = connection.prepareStatement(insertSql.toString())) {
                for (int rowIndex = 0; rowIndex < table.getRowCount(); rowIndex++) {
                    for (int i = 0; i < columnMappings.size(); i++) {
                        ColumnMapping mapping = columnMappings.get(i);
                        String value = "";

                        // Get the value from the source column
                        if (mapping.usesSourceColumnName()) {
                            String sourceColumnName = mapping.getSourceColumnName();
                            if (sourceColumnName != null && !sourceColumnName.isEmpty()) {
                                try {
                                    value = table.getValueAt(rowIndex, sourceColumnName);
                                } catch (Exception e) {
                                    System.err.println("Warning: Error getting value for column '" + sourceColumnName + 
                                        "' at row " + rowIndex + ": " + e.getMessage());
                                }
                            } else {
                                System.err.println("Warning: Source column name is null or empty at index " + i);
                            }
                        } else if (mapping.usesSourceColumnIndex()) {
                            int sourceColumnIndex = mapping.getSourceColumnIndex();
                            if (sourceColumnIndex >= 0 && sourceColumnIndex < table.getColumnCount()) {
                                try {
                                    String columnName = table.getColumnName(sourceColumnIndex);
                                    value = table.getValueAt(rowIndex, columnName);
                                } catch (Exception e) {
                                    System.err.println("Warning: Error getting value for column index " + sourceColumnIndex + 
                                        " at row " + rowIndex + ": " + e.getMessage());
                                }
                            } else {
                                System.err.println("Warning: Source column index " + sourceColumnIndex + 
                                    " is out of bounds (0-" + (table.getColumnCount() - 1) + ")");
                            }
                        } else {
                            System.err.println("Warning: Mapping at index " + i + 
                                " does not specify a source column name or index");
                        }

                        // Use default value if the value is null or empty
                        if (value == null || value.isEmpty()) {
                            value = mapping.getDefaultValue() != null ? mapping.getDefaultValue() : "";
                        }

                        statement.setString(i + 1, value);
                    }
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error writing to database: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Creates a table in the database based on the column mappings.
     *
     * @param connection the database connection
     * @param tableName the name of the table to create
     * @param table the table to get column information from
     * @param columnMappings the column mappings
     * @throws SQLException if there is an error creating the table
     */
    private void createTable(Connection connection, String tableName, ITable table, List<ColumnMapping> columnMappings) throws SQLException {
        StringBuilder createSql = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");

        for (int i = 0; i < columnMappings.size(); i++) {
            ColumnMapping mapping = columnMappings.get(i);
            String dbColumnName = mapping.getTargetColumnName();
            String dbColumnType = mapTableTypeToSqlType(mapping.getTargetColumnType());
            
            createSql.append(dbColumnName).append(" ").append(dbColumnType);

            if (i < columnMappings.size() - 1) {
                createSql.append(", ");
            }
        }
        createSql.append(")");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createSql.toString());
        }
    }

    /**
     * Maps Table types to SQL types.
     *
     * @param tableType the Table type
     * @return the corresponding SQL type
     */
    private String mapTableTypeToSqlType(String tableType) {
        switch (tableType) {
            case "int":
                return "INTEGER";
            case "double":
                return "DOUBLE";
            case "boolean":
                return "BOOLEAN";
            case "string":
            default:
                return "VARCHAR(255)";
        }
    }
}
```

## Test Class Implementation

```java
package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JDBCMappingTest extends AbstractDatabaseTest {

    private JDBCMappingReader mappingReader;
    private JDBCMappingWriter mappingWriter;
    private MappingConfiguration config;

    public JDBCMappingTest() {
        super(new DatabaseTestConfig_H2());
    }

    @BeforeEach
    @Override
    void setUp() throws Exception {
        super.setUp();
        mappingReader = new JDBCMappingReader();
        mappingWriter = new JDBCMappingWriter();
        
        // Create a basic mapping configuration
        config = new MappingConfiguration()
            .setSourceLocation(config.getConnectionString())
            .setOption("username", config.getUsername())
            .setOption("password", config.getPassword())
            .setOption("tableName", testTableName);
            
        // Add column mappings
        config.addColumnMapping(new ColumnMapping("NAME", "Name", "string"));
        config.addColumnMapping(new ColumnMapping("AGE", "Age", "int"));
        config.addColumnMapping(new ColumnMapping("OCCUPATION", "Job", "string"));
        config.addColumnMapping(new ColumnMapping("SALARY", "Income", "double"));
        config.addColumnMapping(new ColumnMapping("ISEMPLOYED", "Employed", "boolean"));
    }

    @Test
    void testReadFromDatabase() throws SQLException {
        // Read data using the mapping reader
        mappingReader.readFromDatabase(table, config);

        // Verify data
        assertEquals(5, table.getRowCount());
        assertEquals(5, table.getColumnCount());

        // Verify column names (should be the target names from the mapping)
        assertEquals("Name", table.getColumnName(0));
        assertEquals("Age", table.getColumnName(1));
        assertEquals("Job", table.getColumnName(2));
        assertEquals("Income", table.getColumnName(3));
        assertEquals("Employed", table.getColumnName(4));

        // Verify first row
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("30", table.getValueAt(0, "Age"));
        assertEquals("Engineer", table.getValueAt(0, "Job"));
        assertEquals("75000.5", table.getValueAt(0, "Income"));
        assertEquals("TRUE", table.getValueAt(0, "Employed"));
    }

    @Test
    void testWriteToDatabase() throws SQLException {
        // Set up a table with data
        Table sourceTable = new Table();
        
        // Add columns
        Map<String, String> columns = new HashMap<>();
        columns.put("Name", "string");
        columns.put("Age", "int");
        columns.put("Job", "string");
        columns.put("Income", "double");
        columns.put("Employed", "boolean");
        sourceTable.setColumns(columns);
        
        // Add a row
        Map<String, String> row = new HashMap<>();
        row.put("Name", "John");
        row.put("Age", "35");
        row.put("Job", "Developer");
        row.put("Income", "85000.0");
        row.put("Employed", "true");
        sourceTable.addRow(row);
        
        // Create a mapping configuration for writing
        MappingConfiguration writeConfig = new MappingConfiguration()
            .setSourceLocation(config.getConnectionString())
            .setOption("username", config.getUsername())
            .setOption("password", config.getPassword())
            .setOption("tableName", "mapped_table")
            .setOption("createTable", true);
            
        // Add column mappings (source column name -> target DB column name)
        writeConfig.addColumnMapping(new ColumnMapping("Name", "EMPLOYEE_NAME", "string"));
        writeConfig.addColumnMapping(new ColumnMapping("Age", "EMPLOYEE_AGE", "int"));
        writeConfig.addColumnMapping(new ColumnMapping("Job", "POSITION", "string"));
        writeConfig.addColumnMapping(new ColumnMapping("Income", "SALARY", "double"));
        writeConfig.addColumnMapping(new ColumnMapping("Employed", "IS_ACTIVE", "boolean"));
        
        // Write the data
        mappingWriter.writeToDatabase(sourceTable, writeConfig);
        
        // Verify the data was written correctly
        try (Connection connection = DriverManager.getConnection(
                config.getConnectionString(), config.getUsername(), config.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM mapped_table")) {
            
            assertTrue(resultSet.next());
            assertEquals("John", resultSet.getString("EMPLOYEE_NAME"));
            assertEquals(35, resultSet.getInt("EMPLOYEE_AGE"));
            assertEquals("Developer", resultSet.getString("POSITION"));
            assertEquals(85000.0, resultSet.getDouble("SALARY"));
            assertEquals(true, resultSet.getBoolean("IS_ACTIVE"));
            
            // Should only be one row
            assertFalse(resultSet.next());
        }
        
        // Clean up
        try (Connection connection = DriverManager.getConnection(
                config.getConnectionString(), config.getUsername(), config.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS mapped_table");
        }
    }

    @Test
    void testQueryWithMapping() throws SQLException {
        // Create a mapping configuration with a query
        MappingConfiguration queryConfig = new MappingConfiguration()
            .setSourceLocation(config.getConnectionString())
            .setOption("username", config.getUsername())
            .setOption("password", config.getPassword())
            .setOption("query", "SELECT * FROM " + testTableName + " WHERE AGE > 25");
            
        // Add column mappings
        queryConfig.addColumnMapping(new ColumnMapping("NAME", "EmployeeName", "string"));
        queryConfig.addColumnMapping(new ColumnMapping("AGE", "EmployeeAge", "int"));
        
        // Read data using the mapping reader
        Table queryTable = new Table();
        mappingReader.readFromDatabase(queryTable, queryConfig);
        
        // Verify data
        assertEquals(3, queryTable.getRowCount()); // Only 3 people are older than 25
        assertEquals(2, queryTable.getColumnCount()); // Only mapped 2 columns
        
        // Verify column names
        assertEquals("EmployeeName", queryTable.getColumnName(0));
        assertEquals("EmployeeAge", queryTable.getColumnName(1));
        
        // Verify all ages are > 25
        for (int i = 0; i < queryTable.getRowCount(); i++) {
            int age = Integer.parseInt(queryTable.getValueAt(i, "EmployeeAge"));
            assertTrue(age > 25);
        }
    }
}
```

## Integration with Existing Code

To integrate these new classes with the existing code, we should add methods to the JDBCProcessor class (similar to CSVProcessor):

```java
package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.dev.mars.jtable.io.common.mapping.MappingConfiguration;

import java.sql.SQLException;

/**
 * Utility class for JDBC operations.
 * This class provides static methods for reading from and writing to databases.
 */
public class JDBCProcessor {
    private static final JDBCReader jdbcReader = new JDBCReader();
    private static final JDBCWriter jdbcWriter = new JDBCWriter();
    private static final JDBCMappingReader jdbcMappingReader = new JDBCMappingReader();
    private static final JDBCMappingWriter jdbcMappingWriter = new JDBCMappingWriter();
    
    /**
     * Reads data from a database table into a table according to a mapping configuration.
     *
     * @param table the table to read into
     * @param config the mapping configuration
     */
    public static void readFromDatabase(ITable table, MappingConfiguration config) {
        try {
            jdbcMappingReader.readFromDatabase(table, config);
        } catch (SQLException e) {
            System.err.println("Error reading from database: " + e.getMessage());
        }
    }
    
    /**
     * Writes data from a table to a database table according to a mapping configuration.
     *
     * @param table the table to write from
     * @param config the mapping configuration
     */
    public static void writeToDatabase(ITable table, MappingConfiguration config) {
        try {
            jdbcMappingWriter.writeToDatabase(table, config);
        } catch (SQLException e) {
            System.err.println("Error writing to database: " + e.getMessage());
        }
    }
    
    // Existing methods would remain...
}
```

## Summary

The implementation provides:

1. **JDBCMappingReader** - Reads data from a database using a mapping configuration
    - Supports both table names and custom SQL queries
    - Maps database columns to table columns based on configuration

2. **JDBCMappingWriter** - Writes data to a database using a mapping configuration
    - Creates tables if needed
    - Maps table columns to database columns based on configuration

3. **JDBCMappingTest** - Tests the functionality of both classes
    - Tests reading with column mapping
    - Tests writing with column mapping
    - Tests using custom queries with mapping

These classes provide the same functionality as the CSV mapping classes but for JDBC database connections, allowing for flexible mapping between tables and databases.