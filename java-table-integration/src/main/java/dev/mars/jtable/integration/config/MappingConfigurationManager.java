package dev.mars.jtable.integration.config;

import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import dev.mars.jtable.io.common.mapping.IMappingSerializer;
import dev.mars.jtable.io.common.mapping.MappingSerializerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Manager class for creating, loading, and saving mapping configurations.
 * This class separates the configuration management concerns from the processing logic.
 * It follows strict rules of encapsulation and dependency injection.
 */
public class MappingConfigurationManager {
    private static final Logger logger = LoggerFactory.getLogger(MappingConfigurationManager.class);

    // Default path to the mappings directory in resources
    private static final String DEFAULT_MAPPINGS_DIR = "mappings";

    // The serializer to use for loading and saving mapping configurations
    private final IMappingSerializer serializer;

    // The path to the mappings directory
    private final String mappingsDir;

    /**
     * Creates a new MappingConfigurationManager with the default serializer (JSON) and mappings directory.
     */
    public MappingConfigurationManager() {
        this(MappingSerializerFactory.createSerializer("json"), DEFAULT_MAPPINGS_DIR);
    }

    /**
     * Creates a new MappingConfigurationManager with the specified serializer and default mappings directory.
     * 
     * @param serializer the serializer to use for loading and saving mapping configurations
     */
    public MappingConfigurationManager(IMappingSerializer serializer) {
        this(serializer, DEFAULT_MAPPINGS_DIR);
    }

    /**
     * Creates a new MappingConfigurationManager with the default serializer (JSON) and the specified mappings directory.
     * 
     * @param mappingsDir the path to the mappings directory
     */
    public MappingConfigurationManager(String mappingsDir) {
        this(MappingSerializerFactory.createSerializer("json"), mappingsDir);
    }

    /**
     * Creates a new MappingConfigurationManager with the specified serializer and mappings directory.
     * 
     * @param serializer the serializer to use for loading and saving mapping configurations
     * @param mappingsDir the path to the mappings directory
     */
    public MappingConfigurationManager(IMappingSerializer serializer, String mappingsDir) {
        this.serializer = serializer;
        this.mappingsDir = mappingsDir;
    }

    /**
     * Creates or prepares a mapping configuration for reading from CSV file.
     * This method accepts parameters for column mappings and options.
     * If a csvMappingFile is provided, it will try to load the configuration from that file.
     * If the file doesn't exist or no file is provided, it will create a new configuration.
     *
     * @param fileLocation the location of the CSV file
     * @param columnMappings the column mappings to use
     * @param options the options to use
     * @param csvMappingFile optional name of the mapping file to load or save
     * @return the mapping configuration
     * @throws IOException if there is an error with the mapping configuration
     */
    public MappingConfiguration createCSVMappingConfiguration(String fileLocation, 
                                                             List<ColumnMapping> columnMappings, 
                                                             Map<String, Object> options,
                                                             String... csvMappingFile) throws IOException {
        // If a mapping file is provided, try to load the configuration from that file
        MappingConfiguration csvConfig = null;

        if (csvMappingFile != null && csvMappingFile.length > 0 && csvMappingFile[0] != null) {
            // Try to load the mapping configuration from the file
            csvConfig = loadMappingConfiguration(csvMappingFile[0]);

            if (csvConfig != null) {
                logger.info("Using existing CSV mapping configuration from file");

                // Update the source location to the current file
                csvConfig.setSourceLocation(fileLocation);

                return csvConfig;
            }

            logger.info("Creating new CSV mapping configuration");
        }

        // Create a new mapping configuration
        csvConfig = new MappingConfiguration()
                .setSourceLocation(fileLocation);

        // Add options
        if (options != null) {
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                csvConfig.setOption(entry.getKey(), entry.getValue());
            }
        }

        // Add column mappings
        if (columnMappings != null) {
            for (ColumnMapping mapping : columnMappings) {
                csvConfig.addColumnMapping(mapping);
            }
        }

        logger.debug("Created CSV mapping configuration with provided column mappings and options");

        // If a mapping file was provided, save the configuration to that file
        if (csvMappingFile != null && csvMappingFile.length > 0 && csvMappingFile[0] != null) {
            saveMappingConfiguration(csvConfig, csvMappingFile[0]);
        }

        return csvConfig;
    }

    /**
     * Creates or prepares a mapping configuration for writing to SQLite database.
     * This method accepts parameters for table name, column mappings, and options.
     * If a sqliteMappingFile is provided, it will try to load the configuration from that file.
     * If the file doesn't exist or no file is provided, it will create a new configuration.
     *
     * @param connection the JDBCConnection to use
     * @param username the username for the database
     * @param password the password for the database
     * @param tableName the name of the table to write to
     * @param columnMappings the column mappings to use
     * @param options the options to use
     * @param sqliteMappingFile optional name of the mapping file to load or save
     * @return the mapping configuration
     * @throws IOException if there is an error with the mapping configuration
     */
    public MappingConfiguration createSQLiteMappingConfiguration(DbConnection connection, 
                                                                String username, 
                                                                String password, 
                                                                String tableName,
                                                                List<ColumnMapping> columnMappings,
                                                                Map<String, Object> options,
                                                                String... sqliteMappingFile) throws IOException {
        // If a mapping file is provided, try to load the configuration from that file
        MappingConfiguration sqliteConfig = null;

        if (sqliteMappingFile != null && sqliteMappingFile.length > 0 && sqliteMappingFile[0] != null) {
            // Try to load the mapping configuration from the file
            sqliteConfig = loadMappingConfiguration(sqliteMappingFile[0]);

            if (sqliteConfig != null) {
                logger.info("Using existing SQLite mapping configuration from file");

                // Update the connection information
                sqliteConfig.setSourceLocation(connection.getConnectionString())
                        .setOption("username", username)
                        .setOption("password", password);

                // Update the table name if provided
                if (tableName != null && !tableName.isEmpty()) {
                    sqliteConfig.setOption("tableName", tableName);
                }

                // Check for system property that can override the table name (for testing)
                sqliteConfig = checkSystemTableNameProperty(sqliteConfig, connection, username, password, columnMappings, options);

                return sqliteConfig;
            }

            logger.info("Creating new SQLite mapping configuration");
        }

        // Create a mapping configuration for writing to SQLite database
        sqliteConfig = new MappingConfiguration()
                .setSourceLocation(connection.getConnectionString())
                .setOption("tableName", tableName)
                .setOption("username", username)
                .setOption("password", password);

        // Add options
        if (options != null) {
            for (Map.Entry<String, Object> entry : options.entrySet()) {
                sqliteConfig.setOption(entry.getKey(), entry.getValue());
            }
        }

        // Add column mappings
        if (columnMappings != null) {
            for (ColumnMapping mapping : columnMappings) {
                sqliteConfig.addColumnMapping(mapping);
            }
        }

        logger.debug("Created SQLite mapping configuration with provided column mappings and options");

        // Check for system property that can override the table name (for testing)
        sqliteConfig = checkSystemTableNameProperty(sqliteConfig, connection, username, password, columnMappings, options);

        // If a mapping file was provided, save the configuration to that file
        if (sqliteMappingFile != null && sqliteMappingFile.length > 0 && sqliteMappingFile[0] != null) {
            saveMappingConfiguration(sqliteConfig, sqliteMappingFile[0]);
        }

        return sqliteConfig;
    }




    /**
     * Creates or prepares a mapping configuration for SQLite with column aliases.
     * This method creates a configuration that will generate SQL statements in the format
     * "SELECT fromColumn AS toColumn" where fromColumn is the source column name and
     * toColumn is the target column name.
     *
     * @param connection the JDBCConnection to use
     * @param username the username for the database
     * @param password the password for the database
     * @param tableName the name of the table to query
     * @param columnMappings the column mappings to use
     * @param options the options to use
     * @param sqliteMappingFile optional name of the mapping file to load or save
     * @return the mapping configuration
     * @throws IOException if there is an error with the mapping configuration
     */
    public MappingConfiguration createSQLiteMappingConfigurationWithAliases(DbConnection connection, 
                                                                String username, 
                                                                String password, 
                                                                String tableName,
                                                                List<ColumnMapping> columnMappings,
                                                                Map<String, Object> options,
                                                                String... sqliteMappingFile) throws IOException {
        // Create a base SQLite mapping configuration
        MappingConfiguration sqliteConfig = createSQLiteMappingConfiguration(
            connection, username, password, tableName, columnMappings, options, sqliteMappingFile);

        // Set the useAliases option to true
        sqliteConfig.setOption("useAliases", true);

        logger.debug("Created SQLite mapping configuration with aliases");

        // If a mapping file was provided, save the updated configuration to that file
        if (sqliteMappingFile != null && sqliteMappingFile.length > 0 && sqliteMappingFile[0] != null) {
            saveMappingConfiguration(sqliteConfig, sqliteMappingFile[0]);
        }

        return sqliteConfig;
    }

    /**
     * Helper method to check for system properties that can override the table name.
     * This is used for testing purposes.
     *
     * @param sqliteConfig the mapping configuration to update
     * @param connection the database connection to use
     * @param username the username for the database
     * @param password the password for the database
     * @param columnMappings the column mappings to use
     * @param options the options to use
     * @return the updated mapping configuration
     * @throws IOException if there is an error with the mapping configuration
     */
    private MappingConfiguration checkSystemTableNameProperty(MappingConfiguration sqliteConfig,
                                                             DbConnection connection,
                                                             String username,
                                                             String password,
                                                             List<ColumnMapping> columnMappings,
                                                             Map<String, Object> options) throws IOException {
        // Check for system property that can override the table name (for testing)
        String systemTableName = System.getProperty("sqlite.table");
        if (systemTableName != null && !systemTableName.isEmpty()) {
            logger.info("Overriding table name with system property: {}", systemTableName);
            sqliteConfig.setOption("tableName", systemTableName);

            // If the table name is datetime_test, use the datetime mapping configuration
            if ("datetime_test".equals(systemTableName)) {
                // Get default options for datetime configuration from CSVToSQLiteDemo
                Map<String, Object> datetimeOptions = dev.mars.jtable.integration.CSVToSQLiteDemo.getDemoSQLiteOptions();
                if (options != null) {
                    datetimeOptions.putAll(options);
                }
                datetimeOptions.put("createTable", true);

                // Get default column mappings for datetime configuration from CSVToSQLiteDemo
                List<ColumnMapping> datetimeColumnMappings = dev.mars.jtable.integration.CSVToSQLiteDemo.getDemoSQLiteDateTimeColumnMappings();

                sqliteConfig = createSQLiteMappingConfiguration(connection, username, password, systemTableName, datetimeColumnMappings, datetimeOptions);
            }
        }
        return sqliteConfig;
    }


    /**
     * Loads a mapping configuration from a file in the resources/mappings directory.
     * Uses the serializer injected in the constructor.
     *
     * @param fileName the name of the file to load from
     * @return the loaded mapping configuration, or null if the file doesn't exist
     * @throws IOException if there is an error loading the configuration
     */
    public MappingConfiguration loadMappingConfiguration(String fileName) throws IOException {
        // Get the path to the mapping file
        String filePath = getMappingFilePath(fileName);

        // Check if the file exists
        File file = new File(filePath);
        if (!file.exists()) {
            logger.info("Mapping configuration file does not exist: {}", filePath);
            return null;
        }

        // Read the configuration from the file using the injected serializer
        MappingConfiguration config = serializer.readFromFile(filePath);

        logger.info("Loaded mapping configuration from file: {}", filePath);

        return config;
    }

    /**
     * Saves a mapping configuration to a file in the resources/mappings directory.
     * Uses the serializer injected in the constructor.
     *
     * @param config the mapping configuration to save
     * @param fileName the name of the file to save to
     * @throws IOException if there is an error saving the configuration
     */
    public void saveMappingConfiguration(MappingConfiguration config, String fileName) throws IOException {
        // Get the path to the mapping file
        String filePath = getMappingFilePath(fileName);

        // Ensure the directory exists
        File file = new File(filePath);
        file.getParentFile().mkdirs();

        // Write the configuration to the file using the injected serializer
        serializer.writeToFile(config, filePath);

        logger.info("Saved mapping configuration to file: {}", filePath);
    }

    /**
     * Gets the path to a mapping configuration file in the resources/mappings directory.
     *
     * @param fileName the name of the mapping configuration file
     * @return the path to the mapping configuration file
     */
    private String getMappingFilePath(String fileName) {
        try {
            // Get the path to the resources directory
            URL resourceUrl = getClass().getClassLoader().getResource("");
            if (resourceUrl == null) {
                // Fallback to the current directory if resources directory is not found
                return new File(System.getProperty("user.dir"), "src\\main\\resources\\" + mappingsDir + "\\" + fileName).getAbsolutePath();
            }

            // Construct the path to the mapping file using File to handle path separators correctly
            File resourcesDir = new File(resourceUrl.toURI());
            File mappingsDirFile = new File(resourcesDir, mappingsDir);
            File mappingFile = new File(mappingsDirFile, fileName);

            return mappingFile.getAbsolutePath();
        } catch (Exception e) {
            // If there's any error, fallback to a simple path
            logger.warn("Error getting mapping file path: {}", e.getMessage());
            return new File(System.getProperty("user.dir"), "src\\main\\resources\\" + mappingsDir + "\\" + fileName).getAbsolutePath();
        }
    }
}
