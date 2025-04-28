package dev.mars.jtable.io.files.jsbc;



import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;

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