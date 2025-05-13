package dev.mars.jtable.integration;


import dev.mars.jtable.core.model.ITable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for testing CSV to H2 integration.
 */
class H2TestUtils {
    private static final Logger logger = LoggerFactory.getLogger(H2TestUtils.class);

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @throws Exception if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath) throws Exception {
        logger.debug("Reading from CSV file: {}", csvFilePath);
        // Directly call the method in CSVToH2Demo
        CSVToH2Demo.readFromCSV(table, csvFilePath);
        logger.debug("Successfully read data from CSV file");
    }

    /**
     * Writes data from a table to an H2 database.
     *
     * @param table the table to write from
     * @param dbUrl the database URL
     * @param tableName the table name
     * @throws Exception if there is an error writing to the database
     */
    static void writeToH2Database(ITable table, String dbUrl, String tableName) throws Exception {
        logger.debug("Writing to H2 database: {} table: {}", dbUrl, tableName);

        // Create a custom method for testing that uses the provided database URL and table name
        dev.mars.jtable.io.files.mapping.MappingConfiguration h2Config = new dev.mars.jtable.io.files.mapping.MappingConfiguration()
                .setSourceLocation(dbUrl)
                .setOption("tableName", tableName)
                .setOption("username", "sa")
                .setOption("password", "")
                .setOption("createTable", true);

        logger.debug("Created H2 mapping configuration");

        // Add column mappings - using the transformed column names from CSVToH2Demo.readFromCSV
        h2Config.addColumnMapping(new dev.mars.jtable.io.files.mapping.ColumnMapping("personId", "ID", "int"))
                .addColumnMapping(new dev.mars.jtable.io.files.mapping.ColumnMapping("fullName", "NAME", "string"))
                .addColumnMapping(new dev.mars.jtable.io.files.mapping.ColumnMapping("emailAddress", "EMAIL", "string"))
                .addColumnMapping(new dev.mars.jtable.io.files.mapping.ColumnMapping("personAge", "AGE", "int"))
                .addColumnMapping(new dev.mars.jtable.io.files.mapping.ColumnMapping("department", "DEPARTMENT", "string"));

        logger.debug("Added column mappings to H2 configuration");

        // Write to H2 database
        dev.mars.jtable.io.files.jdbc.JDBCMappingWriter h2Writer = new dev.mars.jtable.io.files.jdbc.JDBCMappingWriter();
        h2Writer.writeToDatabase(table, h2Config);

        logger.debug("Successfully wrote data to H2 database");
    }
}
