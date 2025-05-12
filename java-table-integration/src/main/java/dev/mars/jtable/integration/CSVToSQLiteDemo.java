package dev.mars.jtable.integration;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.TableCore;
import dev.mars.jtable.io.files.csv.CSVMappingReader;
import dev.mars.jtable.io.files.jdbc.JDBCMappingWriter;
import dev.mars.jtable.io.files.mapping.ColumnMapping;
import dev.mars.jtable.io.files.mapping.MappingConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Demonstration of CSV to SQLite database integration using MappingConfiguration.
 * This class shows how to:
 * 1. Read data from a CSV file using MappingConfiguration
 * 2. Write the data to a SQLite database using MappingConfiguration
 */
public class CSVToSQLiteDemo {

    private static final Logger logger = LoggerFactory.getLogger(CSVToSQLiteDemo.class);

    /**
     * Main method to run the demonstration.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            logger.info("Starting CSV to SQLite demonstration");

            // Create a demo CSV file if it doesn't exist
            String csvFilePath = "demo_data.csv";
            createDemoCSVIfNotExists(csvFilePath);

            // Create a table to hold the data
            ITable table = new TableCore();
            logger.debug("Created empty table");

            // Read from CSV
            readFromCSV(table, csvFilePath);
            logger.info("Successfully read data from CSV file: {}", csvFilePath);
            logger.info("Table contains {} rows", table.getRowCount());

            // Write to SQLite database
            writeToSQLiteDatabase(table);
            logger.info("Successfully wrote data to SQLite database");

        } catch (Exception e) {
            logger.error("Error in demonstration: {}", e.getMessage(), e);
        }
    }

    /**
     * Reads data from a CSV file into a table using MappingConfiguration.
     *
     * @param table the table to read into
     * @param csvFilePath the path to the CSV file
     * @throws IOException if there is an error reading the file
     */
    static void readFromCSV(ITable table, String csvFilePath) throws IOException {
        // Create a mapping configuration for reading from CSV
        MappingConfiguration csvConfig = new MappingConfiguration()
                .setSourceLocation(csvFilePath)
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", true);

        // Add column mappings
        csvConfig.addColumnMapping(new ColumnMapping("id", "id", "int"))
                .addColumnMapping(new ColumnMapping("name", "name", "string"))
                .addColumnMapping(new ColumnMapping("email", "email", "string"))
                .addColumnMapping(new ColumnMapping("age", "age", "int"));

        // Read from CSV
        CSVMappingReader csvReader = new CSVMappingReader();
        csvReader.readFromCSV(table, csvConfig);
    }

    /**
     * Writes data from a table to a SQLite database using MappingConfiguration.
     *
     * @param table the table to write from
     * @throws SQLException if there is an error writing to the database
     */
    static void writeToSQLiteDatabase(ITable table) throws SQLException {
        // Create a mapping configuration for writing to SQLite database
        MappingConfiguration sqliteConfig = new MappingConfiguration()
                .setSourceLocation("jdbc:sqlite:demo_sqlite.db")
                .setOption("tableName", "person")
                .setOption("username", "")
                .setOption("password", "")
                .setOption("createTable", true);

        // Add column mappings - SQLite preserves case sensitivity, so we'll use lowercase
        sqliteConfig.addColumnMapping(new ColumnMapping("id", "id", "int"))
                .addColumnMapping(new ColumnMapping("name", "name", "string"))
                .addColumnMapping(new ColumnMapping("email", "email", "string"))
                .addColumnMapping(new ColumnMapping("age", "age", "int"));

        // Write to SQLite database
        JDBCMappingWriter sqliteWriter = new JDBCMappingWriter();
        sqliteWriter.writeToDatabase(table, sqliteConfig);
    }

    /**
     * Creates a demo CSV file if it doesn't exist.
     *
     * @param csvFilePath the path to the CSV file
     * @throws IOException if there is an error creating the file
     */
    private static void createDemoCSVIfNotExists(String csvFilePath) throws IOException {
        File csvFile = new File(csvFilePath);
        if (!csvFile.exists()) {
            logger.info("Creating demo CSV file: {}", csvFilePath);

            // Create a simple CSV file with header and some data
            StringBuilder csvContent = new StringBuilder();
            csvContent.append("id,name,email,age\n");
            csvContent.append("1,John Doe,john.doe@example.com,30\n");
            csvContent.append("2,Jane Smith,jane.smith@example.com,25\n");
            csvContent.append("3,Bob Johnson,bob.johnson@example.com,40\n");

            // Write the content to the file
            java.nio.file.Files.write(csvFile.toPath(), csvContent.toString().getBytes());

            logger.info("Demo CSV file created successfully");
        } else {
            logger.info("Using existing CSV file: {}", csvFilePath);
        }
    }
}