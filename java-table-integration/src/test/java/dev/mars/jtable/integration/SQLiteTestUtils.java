package dev.mars.jtable.integration;


import dev.mars.jtable.core.model.ITable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for testing CSV to SQLite integration.
 */
class SQLiteTestUtils {
    private static final Logger logger = LoggerFactory.getLogger(SQLiteTestUtils.class);

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     *
     * @param table       the table to read into
     * @param csvFilePath the path to the CSV file
     * @throws Exception if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath) throws Exception {
        logger.debug("Reading from CSV file: {}", csvFilePath);

        // Create a FileConnection
        dev.mars.jtable.io.common.datasource.FileConnection connection = null;
        try {
            connection = (dev.mars.jtable.io.common.datasource.FileConnection) 
                    dev.mars.jtable.io.common.datasource.DataSourceConnectionFactory.createConnection(csvFilePath);
            if (!connection.connect()) {
                throw new java.io.IOException("Failed to connect to CSV file: " + csvFilePath);
            }
            logger.debug("Successfully connected to CSV file");

            // Create a mapping configuration
            dev.mars.jtable.io.files.mapping.MappingConfiguration csvConfig = 
                    CSVToSQLiteDemo.createCSVMappingConfiguration(connection);

            // Call the method in CSVToSQLiteDemo
            CSVToSQLiteDemo.readFromCSV(table, csvFilePath, csvConfig);
            logger.debug("Successfully read data from CSV file");
        } finally {
            // Ensure connection is closed
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from CSV file");
            }
        }
    }

    /**
     * Writes data from a table to a SQLite database.
     *
     * @param table     the table to write from
     * @param dbUrl     the database URL
     * @param tableName the table name
     * @throws Exception if there is an error writing to the database
     */
    static void writeToSQLiteDatabase(ITable table, String dbUrl, String tableName) throws Exception {
        logger.debug("Writing to SQLite database: {} table: {}", dbUrl, tableName);

        // Create a JDBCConnection
        String username = "";  // SQLite doesn't use username/password
        String password = "";
        dev.mars.jtable.io.common.datasource.JDBCConnection connection = null;
        try {
            connection = new dev.mars.jtable.io.common.datasource.JDBCConnection(dbUrl, username, password);
            if (!connection.connect()) {
                throw new java.sql.SQLException("Failed to connect to SQLite database: " + dbUrl);
            }
            logger.debug("Successfully connected to SQLite database");

            // Create a mapping configuration
            dev.mars.jtable.io.files.mapping.MappingConfiguration sqliteConfig = 
                    CSVToSQLiteDemo.createSQLiteMappingConfiguration(connection, username, password);

            // Override the table name
            sqliteConfig.setOption("tableName", tableName);

            // Call the method in CSVToSQLiteDemo
            CSVToSQLiteDemo.writeToSQLiteDatabase(table, sqliteConfig);
            logger.debug("Successfully wrote data to SQLite database");
        } finally {
            // Ensure connection is closed
            if (connection != null && connection.isConnected()) {
                connection.disconnect();
                logger.debug("Disconnected from SQLite database");
            }
        }
    }
}
