package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.integration.config.MappingConfigurationManager;
import dev.mars.jtable.integration.csv.CSVProcessor;
import dev.mars.jtable.integration.db.DbConnectionManager;
import dev.mars.jtable.integration.db.SQLiteProcessor;
import dev.mars.jtable.integration.db.SQLiteQueryManager;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.files.jdbc.JDBCReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Demonstration of CSV to SQLite database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to a SQLite database using MappingConfiguration
 * 3. Save and load mapping configurations using JSON serialization
 * 

 * - MappingConfigurationManager: Handles mapping configuration creation and management
 * - CSVProcessor: Handles CSV file operations
 * - DbConnectionManager: Handles database connection creation and management
 * - SQLiteProcessor: Handles SQLite database operations
 */
public class CSVToSQLiteDemo {
    private static final Logger logger = LoggerFactory.getLogger(CSVToSQLiteDemo.class);

    //Demo CSV file path
    private static final String DEMO_CSV_FILE_NAME = "demo_data.csv";

    // File names for mapping configurations
    private static final String CSV_MAPPING_FILE = "csv_sqlite_mapping.json";
    private static final String SQLITE_MAPPING_FILE = "sqlite_mapping.json";

    // Default table name for SQLite database
    public static final String DEFAULT_SQLITE_TABLE_NAME = "person_data";

    // Manager and processor instances
    private final MappingConfigurationManager configManager;
    private final CSVProcessor csvProcessor;
    private final DbConnectionManager dbConnectionManager;
    private final SQLiteProcessor sqliteProcessor;
    private final SQLiteQueryManager sqliteQueryManager;

    public CSVToSQLiteDemo() {
        logger.debug("Initializing CSVToSQLiteDemo");
        this.configManager = new MappingConfigurationManager();
        logger.debug("Initialized MappingConfigurationManager");
        this.csvProcessor = new CSVProcessor();
        logger.debug("Initialized CSVProcessor");
        this.dbConnectionManager = new DbConnectionManager();
        logger.debug("Initialized DbConnectionManager");
        this.sqliteProcessor = new SQLiteProcessor();
        logger.debug("Initialized SQLiteProcessor");
        this.sqliteQueryManager = new SQLiteQueryManager(dbConnectionManager, new JDBCReader());
        logger.debug("Initialized SQLiteQueryManager");
        logger.debug("CSVToSQLiteDemo initialization complete");
    }

    public static void main(String[] args) {
        var demo = new CSVToSQLiteDemo();
        demo.run();
    }

    /**
     * Runs the demonstration.
     */
    public void run() {
        try {
            logger.info("Starting CSV to SQLite with mapping demonstration");
            logger.debug("Demo process starting with the following parameters:");
            logger.debug("- CSV file name: {}", DEMO_CSV_FILE_NAME);
            logger.debug("- CSV mapping file: {}", CSV_MAPPING_FILE);
            logger.debug("- SQLite mapping file: {}", SQLITE_MAPPING_FILE);
            logger.debug("- Default SQLite table name: {}", DEFAULT_SQLITE_TABLE_NAME);

            String csvFileName = DEMO_CSV_FILE_NAME;
            String csvContent = getDemoCSVFilePath();
            logger.debug("Generated CSV content with {} characters", csvContent.length());

            // Create a demo CSV file if it doesn't exist
            createDemoCSVIfNotExists(csvFileName, csvContent);
            logger.debug("Demo CSV file created or already exists: {}", csvFileName);

            // Create a table to hold the data
            ITable table = new TableCore("CSVToSQLiteDemo-InputTable");
            logger.debug("Created empty TableCore with name '{}' and initial capacity", table.getName());

            // Get the CSV file location
            String fileLocation = csvProcessor.getCSVFileLocation(csvFileName);
            logger.debug("Resolved CSV file location: {}", fileLocation);

            // Create CSV column mappings and options
            List<ColumnMapping> csvColumnMappings = getDemoCSVColumnMappings();
            logger.debug("Created CSV column mappings with {} mappings", csvColumnMappings.size());
            for (int i = 0; i < csvColumnMappings.size(); i++) {
                ColumnMapping mapping = csvColumnMappings.get(i);
                logger.debug("CSV mapping {}: {} -> {} (type: {}, default: {})", 
                    i, mapping.getSourceColumnName(), mapping.getTargetColumnName(), 
                    mapping.getTargetColumnType(), mapping.getDefaultValue());
            }

            Map<String, Object> csvOptions = getDemoCSVOptions();
            logger.debug("Created CSV options with {} options", csvOptions.size());
            for (Map.Entry<String, Object> entry : csvOptions.entrySet()) {
                logger.debug("CSV option: {} = {}", entry.getKey(), entry.getValue());
            }

            // Prepare the mapping configuration
            logger.debug("Creating CSV mapping configuration with file location: {}", fileLocation);
            MappingConfiguration csvConfig = configManager.createCSVMappingConfiguration(
                    fileLocation,
                    csvColumnMappings,
                    csvOptions,
                    CSV_MAPPING_FILE);
            logger.debug("CSV mapping configuration created successfully");

            // Read from CSV
            logger.debug("Reading data from CSV file: {} using mapping configuration", csvFileName);
            int rowsRead = csvProcessor.readFromCSV(table, csvFileName, csvConfig);
            logger.debug("Read operation completed, processed {} rows", rowsRead);
            logger.info("Successfully read data from CSV file: {}", csvFileName);
            logger.info("Table contains {} rows", table.getRowCount());
            logger.debug("Table column count: {}", table.getColumnCount());

            // Create a database connection for SQLite
            logger.debug("Creating database connection for SQLite");
            DatabaseProperties dbProperties = new DatabaseProperties();
            logger.debug("Using database properties: connection string={}, username={}", 
                dbProperties.getSqliteConnectionString(), 
                dbProperties.getSqliteUsername() != null ? dbProperties.getSqliteUsername() : "<empty>");
            DbConnection dbConnection = dbConnectionManager.createSQLiteConnection(dbProperties);
            logger.debug("SQLite connection created: {}", dbConnection.getConnectionString());

            try {
                // Connect to the database
                logger.debug("Connecting to SQLite database");
                boolean connected = dbConnectionManager.connect(dbConnection);
                logger.debug("Successfully connected to SQLite database: {}", connected);
                logger.debug("Connection details: isConnected={}, connectionString={}", 
                    dbConnection.isConnected(), dbConnection.getConnectionString());

                // Create SQLite column mappings and options
                List<ColumnMapping> sqliteColumnMappings = getDemoSQLiteColumnMappings();
                logger.debug("Created SQLite column mappings with {} mappings", sqliteColumnMappings.size());
                for (int i = 0; i < sqliteColumnMappings.size(); i++) {
                    ColumnMapping mapping = sqliteColumnMappings.get(i);
                    logger.debug("SQLite mapping {}: {} -> {} (type: {}, default: {})", 
                        i, mapping.getSourceColumnName(), mapping.getTargetColumnName(), 
                        mapping.getTargetColumnType(), mapping.getDefaultValue());
                }

                Map<String, Object> sqliteOptions = getDemoSQLiteOptions();
                logger.debug("Created SQLite options with {} options", sqliteOptions.size());
                for (Map.Entry<String, Object> entry : sqliteOptions.entrySet()) {
                    logger.debug("SQLite option: {} = {}", entry.getKey(), entry.getValue());
                }

                // Prepare the mapping configuration
                logger.debug("Creating SQLite mapping configuration for table: {}", DEFAULT_SQLITE_TABLE_NAME);
                MappingConfiguration sqliteConfig = configManager.createSQLiteMappingConfiguration(
                        dbConnection,
                        dbConnection.getUsername(),
                        dbConnection.getPassword(),
                        DEFAULT_SQLITE_TABLE_NAME,
                        sqliteColumnMappings,
                        sqliteOptions,
                        SQLITE_MAPPING_FILE);
                logger.debug("SQLite mapping configuration created successfully");

                // Write to SQLite database
                logger.debug("Writing data to SQLite database: {} rows", table.getRowCount());
                int rowsWritten = sqliteProcessor.writeToSQLiteDatabase(table, dbConnection, sqliteConfig);
                logger.debug("Write operation completed, wrote {} rows", rowsWritten);
                logger.info("Successfully wrote data to SQLite database");

                // Read data from SQLite database (new step)
                logger.debug("Reading data from SQLite database table: {}", DEFAULT_SQLITE_TABLE_NAME);
                ITable queryResult = readFromSQLiteDatabase(dbConnection, DEFAULT_SQLITE_TABLE_NAME);
                logger.debug("Query result table has {} rows and {} columns", queryResult.getRowCount(), queryResult.getColumnCount());
                logger.info("Successfully read data from SQLite database");

                // Generate a SQL SELECT statement from the mapping configuration
                logger.debug("Generating SQL SELECT statement from mapping configuration");
                String sqlQuery = sqliteQueryManager.generateSelectStatement(sqliteConfig);
                logger.debug("Generated SQL query: {}", sqlQuery);

                // Execute the query using SQLiteQueryManager
                logger.debug("Executing generated SQL query");
                ITable queryResultWithMapping = sqliteQueryManager.executeQuery(sqlQuery, dbConnection);
                logger.debug("Query execution completed");
                logger.debug("Query result table has {} rows and {} columns", queryResultWithMapping.getRowCount(), queryResultWithMapping.getColumnCount());
                logger.info("Successfully read data from SQLite database using generated SQL query");

                // Print out the content of the table
                logger.debug("Logging table data for demonstration purposes");
                logTableData(queryResultWithMapping);
                logger.debug("Table data logging complete");

                // Demonstrate the new SQLite mapping configuration with aliases
                logger.info("Demonstrating SQLite mapping configuration with column aliases");

                // Create SQLite column mappings for the alias demonstration
                List<ColumnMapping> aliasColumnMappings = getDemoSQLiteColumnMappings();
                logger.debug("Created SQLite column mappings for alias demonstration with {} mappings", aliasColumnMappings.size());

                // Create a mapping configuration with aliases
                logger.debug("Creating SQLite mapping configuration with aliases for table: {}", DEFAULT_SQLITE_TABLE_NAME);
                String aliasMappingFile = "sqlite_alias_mapping.json";
                MappingConfiguration aliasConfig = configManager.createSQLiteMappingConfigurationWithAliases(
                        dbConnection,
                        dbConnection.getUsername(),
                        dbConnection.getPassword(),
                        DEFAULT_SQLITE_TABLE_NAME,
                        aliasColumnMappings,
                        sqliteOptions,
                        aliasMappingFile);
                logger.debug("SQLite mapping configuration with aliases created successfully");

                // Generate a SQL SELECT statement with aliases from the mapping configuration
                logger.debug("Generating SQL SELECT statement with aliases from mapping configuration");
                String sqlQueryWithAliases = sqliteQueryManager.generateSelectStatement(aliasConfig);
                logger.debug("Generated SQL query with aliases: {}", sqlQueryWithAliases);

                // Execute the query using SQLiteQueryManager
                logger.debug("Executing generated SQL query with aliases");
                ITable queryResultWithAliases = sqliteQueryManager.executeQuery(sqlQueryWithAliases, dbConnection);
                logger.debug("Query execution completed");
                logger.debug("Query result table has {} rows and {} columns", queryResultWithAliases.getRowCount(), queryResultWithAliases.getColumnCount());
                logger.info("Successfully read data from SQLite database using generated SQL query with aliases");

                // Print out the content of the table
                logger.debug("Logging table data for alias demonstration");
                logger.debug("-----------------------------------------------------------------");
                logTableData(queryResultWithAliases);
                logger.debug("-----------------------------------------------------------------");
                logger.debug("Table data logging complete for alias demonstration");

            } finally {
                // Ensure connection is closed even if an exception occurs
                logger.debug("Ensuring database connection is closed");
                dbConnectionManager.ensureConnectionClosed(dbConnection);
                logger.debug("Database connection cleanup complete");
            }

        } catch (IOException e) {
            logger.error("I/O error in demonstration: {}", e.getMessage(), e);
        } catch (SQLException e) {
            logger.error("SQL error in demonstration: {}", e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }


    public static Map<String, Object> getDemoSQLiteOptions() {
        logger.debug("Creating demo SQLite options");
        Map<String, Object> options = new HashMap<>();

        logger.debug("Setting SQLite option: createTable = true");
        options.put("createTable", true);

        logger.debug("Created SQLite options with {} settings", options.size());
        return options;
    }


    /**
     * Creates a demo CSV file if it doesn't exist.
     * This method writes sample data to a CSV file for demonstration purposes.
     *
     * @param csvFilePath the path to the CSV file
     * @param csvContent  the content to write to the CSV file
     * @throws IOException if there is an error writing to the file
     */
    private void createDemoCSVIfNotExists(String csvFilePath, String csvContent) throws IOException {
        logger.debug("Checking if demo CSV file exists: {}", csvFilePath);
        File csvFile = new File(csvFilePath);

        if (!csvFile.exists()) {
            logger.info("Creating demo CSV file: {}", csvFilePath);
            logger.debug("CSV file path: {}", csvFile.getAbsolutePath());
            logger.debug("CSV content length: {} bytes", csvContent.getBytes().length);

            // Write the content to the file
            logger.debug("Writing content to CSV file");
            Files.write(csvFile.toPath(), csvContent.getBytes());
            logger.debug("File write operation completed");

            logger.info("Demo CSV file created successfully");
        } else {
            logger.info("Using existing CSV file: {}", csvFilePath);
            logger.debug("Existing file size: {} bytes", csvFile.length());
            logger.debug("Existing file last modified: {}", new java.util.Date(csvFile.lastModified()));
        }
    }


    public static String getDemoCSVFilePath() {
        logger.debug("Generating demo CSV content");
        StringBuilder csvContent = new StringBuilder();
        csvContent.append("id,name,email,age\n");
        csvContent.append("1,John Doe,john.doe@example.com,30\n");
        csvContent.append("2,Jane Smith,jane.smith@example.com,25\n");
        csvContent.append("3,Bob Johnson,bob.johnson@example.com,40\n");
        logger.debug("Generated CSV content with header and {} data rows", 3);
        return csvContent.toString();
    }


    public static List<ColumnMapping> getDemoCSVColumnMappings() {
        logger.debug("Creating demo CSV column mappings");
        List<ColumnMapping> columnMappings = new ArrayList<>();

        logger.debug("Adding CSV column mapping: id -> personId (type: int, default: 0)");
        columnMappings.add(new ColumnMapping("id", "personId", "int").setDefaultValue("0"));

        logger.debug("Adding CSV column mapping: name -> fullName (type: string, default: Unknown)");
        columnMappings.add(new ColumnMapping("name", "fullName", "string").setDefaultValue("Unknown"));

        logger.debug("Adding CSV column mapping: email -> emailAddress (type: string, default: no-email@example.com)");
        columnMappings.add(new ColumnMapping("email", "emailAddress", "string").setDefaultValue("no-email@example.com"));

        logger.debug("Adding CSV column mapping: age -> personAge (type: int, default: 0)");
        columnMappings.add(new ColumnMapping("age", "personAge", "int").setDefaultValue("0"));

        logger.debug("Adding CSV column mapping: department -> department (type: string, default: General)");
        columnMappings.add(new ColumnMapping("department", "department", "string").setDefaultValue("General"));

        logger.debug("Created {} CSV column mappings", columnMappings.size());
        return columnMappings;
    }

    public static Map<String, Object> getDemoCSVOptions() {
        logger.debug("Creating demo CSV options");
        Map<String, Object> options = new HashMap<>();

        logger.debug("Setting CSV option: hasHeaderRow = true");
        options.put("hasHeaderRow", true);

        logger.debug("Setting CSV option: allowEmptyValues = true");
        options.put("allowEmptyValues", true);

        logger.debug("Created CSV options with {} settings", options.size());
        return options;
    }

    public static List<ColumnMapping> getDemoSQLiteColumnMappings() {
        logger.debug("Creating demo SQLite column mappings");
        List<ColumnMapping> columnMappings = new ArrayList<>();

        logger.debug("Adding SQLite column mapping: personId -> person_id (type: int, default: 0)");
        columnMappings.add(new ColumnMapping("personId", "person_id", "int").setDefaultValue("0"));

        logger.debug("Adding SQLite column mapping: fullName -> full_name (type: string, default: Unknown)");
        columnMappings.add(new ColumnMapping("fullName", "full_name", "string").setDefaultValue("Unknown"));

        logger.debug("Adding SQLite column mapping: emailAddress -> email_address (type: string, default: no-email@example.com)");
        columnMappings.add(new ColumnMapping("emailAddress", "email_address", "string").setDefaultValue("no-email@example.com"));

        logger.debug("Adding SQLite column mapping: personAge -> age (type: int, default: 0)");
        columnMappings.add(new ColumnMapping("personAge", "age", "int").setDefaultValue("0"));

        logger.debug("Adding SQLite column mapping: department -> department (type: string, default: General)");
        columnMappings.add(new ColumnMapping("department", "department", "string").setDefaultValue("General"));

        logger.debug("Created {} SQLite column mappings", columnMappings.size());
        return columnMappings;
    }


    public static List<ColumnMapping> getDemoSQLiteDateTimeColumnMappings() {
        logger.debug("Creating demo SQLite datetime column mappings");
        List<ColumnMapping> columnMappings = new ArrayList<>();

        logger.debug("Adding SQLite datetime column mapping: ID -> id (type: int)");
        columnMappings.add(new ColumnMapping("ID", "id", "int"));

        logger.debug("Adding SQLite datetime column mapping: NAME -> name (type: string)");
        columnMappings.add(new ColumnMapping("NAME", "name", "string"));

        logger.debug("Adding SQLite datetime column mapping: BIRTH_DATE -> birth_date (type: date)");
        columnMappings.add(new ColumnMapping("BIRTH_DATE", "birth_date", "date"));

        logger.debug("Adding SQLite datetime column mapping: START_TIME -> start_time (type: time)");
        columnMappings.add(new ColumnMapping("START_TIME", "start_time", "time"));

        logger.debug("Adding SQLite datetime column mapping: CREATED_AT -> created_at (type: datetime)");
        columnMappings.add(new ColumnMapping("CREATED_AT", "created_at", "datetime"));

        logger.debug("Created {} SQLite datetime column mappings", columnMappings.size());
        return columnMappings;
    }

    /**
     * Reads data from the SQLite database using a SQL query.
     * This method demonstrates how to use SQLiteQueryManager to query the database.
     *
     * @param dbConnection the database connection to use
     * @param tableName the name of the table to query
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    private ITable readFromSQLiteDatabase(DbConnection dbConnection, String tableName) throws SQLException {
        logger.info("Reading data from SQLite database table: {}", tableName);
        logger.debug("Database connection details: isConnected={}, connectionString={}", 
            dbConnection.isConnected(), dbConnection.getConnectionString());

        // Create a SQL query to select all data from the table
        String query = "SELECT * FROM " + tableName;
        logger.debug("Executing SQL query: {}", query);

        // Execute the query using SQLiteQueryManager
        logger.debug("Using SQLiteQueryManager to execute query");
        ITable result = sqliteQueryManager.executeQuery(query, dbConnection);
        logger.debug("Query execution completed");

        // Log result details
        logger.debug("Query result: {} rows, {} columns", result.getRowCount(), result.getColumnCount());
        if (result.getRowCount() > 0) {
            StringBuilder columnTypes = new StringBuilder("Column types: ");
            for (int i = 0; i < result.getColumnCount(); i++) {
                if (i > 0) columnTypes.append(", ");
                String columnName = result.getColumnName(i);
                String value = result.getValueAt(0, columnName);
                String type = value != null ? result.inferType(value) : "null";
                columnTypes.append(columnName).append("=").append(type);
            }
            logger.debug(columnTypes.toString());
        }

        logger.info("Successfully read {} rows from SQLite database table: {}", result.getRowCount(), tableName);

        // Log the data for demonstration purposes
        logger.debug("Logging query result data");
        logTableData(result);
        logger.debug("Query result logging complete");

        return result;
    }

    /**
     * Reads data from the SQLite database using a mapping configuration.
     * This method demonstrates how to use SQLiteQueryManager to generate a SQL SELECT statement
     * from a mapping configuration and execute it.
     *
     * @param dbConnection the database connection to use
     * @param config the mapping configuration to use
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    private ITable readFromSQLiteDatabaseWithMapping(DbConnection dbConnection, MappingConfiguration config) throws SQLException {
        logger.info("Reading data from SQLite database using mapping configuration");
        logger.debug("Database connection details: isConnected={}, connectionString={}", 
            dbConnection.isConnected(), dbConnection.getConnectionString());

        // Generate a SQL SELECT statement from the mapping configuration
        String sqlQuery = sqliteQueryManager.generateSelectStatement(config);
        logger.debug("Generated SQL query from mapping configuration: {}", sqlQuery);

        // Execute the query using the readFromSQLiteDatabaseWithQuery method
        return readFromSQLiteDatabaseWithQuery(dbConnection, sqlQuery);
    }

    /**
     * Reads data from the SQLite database using a custom SQL query.
     * This method demonstrates how to use SQLiteQueryManager to execute a custom SQL query.
     *
     * @param dbConnection the database connection to use
     * @param sqlQuery the SQL query to execute
     * @return a table containing the query results
     * @throws SQLException if there is an error executing the query
     */
    private ITable readFromSQLiteDatabaseWithQuery(DbConnection dbConnection, String sqlQuery) throws SQLException {
        logger.info("Reading data from SQLite database using custom SQL query");
        logger.debug("Database connection details: isConnected={}, connectionString={}", 
            dbConnection.isConnected(), dbConnection.getConnectionString());
        logger.debug("Executing SQL query: {}", sqlQuery);

        // Execute the query using SQLiteQueryManager
        logger.debug("Using SQLiteQueryManager to execute query");
        ITable result = sqliteQueryManager.executeQuery(sqlQuery, dbConnection);
        logger.debug("Query execution completed");

        // Log result details
        logger.debug("Query result: {} rows, {} columns", result.getRowCount(), result.getColumnCount());
        if (result.getRowCount() > 0) {
            StringBuilder columnTypes = new StringBuilder("Column types: ");
            for (int i = 0; i < result.getColumnCount(); i++) {
                if (i > 0) columnTypes.append(", ");
                String columnName = result.getColumnName(i);
                String value = result.getValueAt(0, columnName);
                String type = value != null ? result.inferType(value) : "null";
                columnTypes.append(columnName).append("=").append(type);
            }
            logger.debug(columnTypes.toString());
        }

        logger.info("Successfully read {} rows from SQLite database using custom SQL query", result.getRowCount());

        // Log the data for demonstration purposes
        logger.debug("Logging query result data");
        logTableData(result);
        logger.debug("Query result logging complete");

        return result;
    }

    /**
     * Logs the data in a table for demonstration purposes.
     *
     * @param table the table to log
     */
    private void logTableData(ITable table) {
        logger.debug("Starting to log table data");
        logger.debug("Table structure: {} rows and {} columns", table.getRowCount(), table.getColumnCount());

        // Log column names
        StringBuilder header = new StringBuilder("Columns: ");
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i > 0) header.append(", ");
            header.append(table.getColumnName(i));
        }
        logger.debug("Column names: {}", header.toString());

        // Log column types if table has data
        if (table.getRowCount() > 0) {
            StringBuilder types = new StringBuilder("Column types: ");
            for (int i = 0; i < table.getColumnCount(); i++) {
                if (i > 0) types.append(", ");
                String columnName = table.getColumnName(i);
                String value = table.getValueAt(0, columnName);
                String type = value != null ? table.inferType(value) : "null";
                types.append(columnName).append("=").append(type);
            }
            logger.debug(types.toString());
        }

        // Log row data (limit to first 10 rows for large tables)
        int rowsToLog = Math.min(table.getRowCount(), 10);
        logger.debug("Logging first {} rows of data", rowsToLog);
        for (int row = 0; row < rowsToLog; row++) {
            StringBuilder rowData = new StringBuilder("Row ").append(row).append(": ");
            for (int col = 0; col < table.getColumnCount(); col++) {
                if (col > 0) rowData.append(", ");
                String columnName = table.getColumnName(col);
                String value = table.getValueAt(row, columnName);
                rowData.append(columnName).append("=").append(value);
            }
            logger.debug("Row data: {}", rowData.toString());
        }

        // Indicate if there are more rows not shown
        if (table.getRowCount() > rowsToLog) {
            logger.debug("Not showing {} additional rows", table.getRowCount() - rowsToLog);
            logger.info("... and {} more rows", table.getRowCount() - rowsToLog);
        }

        logger.debug("Finished logging table data");
    }
}
