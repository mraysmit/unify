package dev.mars.jtable.integration.db;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.DbConnection;
import dev.mars.jtable.integration.utils.DatabaseProperties;
import dev.mars.jtable.io.files.jdbc.JDBCReader;
import dev.mars.jtable.io.common.mapping.ColumnMapping;
import dev.mars.jtable.io.common.mapping.MappingConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SQLiteQueryManager.
 * This class tests the functionality of the SQLiteQueryManager class.
 */
public class SQLiteQueryManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(SQLiteQueryManagerTest.class);

    private static final String TEST_DB_URL = "jdbc:sqlite:test_query.db";
    private static final String TEST_TABLE_NAME = "test_query_table";

    private SQLiteQueryManager queryManager;
    private DbConnectionManager dbConnectionManager;

    @BeforeEach
    public void setUp() throws Exception {
        logger.info("Setting up test environment");

        // Create the query manager
        dbConnectionManager = new DbConnectionManager();
        queryManager = new SQLiteQueryManager(dbConnectionManager, new JDBCReader());

        // Create a test database and table
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            // Drop the table if it exists
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE_NAME);

            // Create a new table
            stmt.execute("CREATE TABLE " + TEST_TABLE_NAME + " (id INTEGER, name TEXT, age INTEGER)");

            // Insert some test data
            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (1, 'Alice', 30)");
            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (2, 'Bob', 25)");
            stmt.execute("INSERT INTO " + TEST_TABLE_NAME + " VALUES (3, 'Charlie', 35)");

            logger.debug("Created test database and table with sample data");
        } catch (Exception e) {
            logger.error("Error setting up test environment: {}", e.getMessage());
            throw e;
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.info("Tearing down test environment");

        // Delete the test database file
        try {
            Files.deleteIfExists(Paths.get("test_query.db"));
            logger.debug("Deleted test database file");
        } catch (Exception e) {
            logger.warn("Could not delete test database file: {}", e.getMessage());
        }
    }

    @Test
    public void testExecuteQueryWithDefaultProperties() throws Exception {
        logger.info("Testing executeQuery with default properties");

        // Create a database connection for the test database
        DbConnection connection = dbConnectionManager.createConnection(TEST_DB_URL, "", "");

        try {
            // Connect to the database
            dbConnectionManager.connect(connection);

            // Execute a simple query
            String query = "SELECT * FROM " + TEST_TABLE_NAME + " ORDER BY id";
            ITable result = queryManager.executeQuery(query, connection);

            // Verify the results
            assertNotNull(result, "Result should not be null");
            assertEquals(3, result.getRowCount(), "Result should have 3 rows");
            assertEquals(3, result.getColumnCount(), "Result should have 3 columns");

            // Check the column names
            assertEquals("id", result.getColumnName(0), "First column should be 'id'");
            assertEquals("name", result.getColumnName(1), "Second column should be 'name'");
            assertEquals("age", result.getColumnName(2), "Third column should be 'age'");

            // Check the values in the first row
            assertEquals("1", result.getValueAt(0, "id"), "First row, id column should be '1'");
            assertEquals("Alice", result.getValueAt(0, "name"), "First row, name column should be 'Alice'");
            assertEquals("30", result.getValueAt(0, "age"), "First row, age column should be '30'");

            // Check the values in the second row
            assertEquals("2", result.getValueAt(1, "id"), "Second row, id column should be '2'");
            assertEquals("Bob", result.getValueAt(1, "name"), "Second row, name column should be 'Bob'");
            assertEquals("25", result.getValueAt(1, "age"), "Second row, age column should be '25'");

            // Check the values in the third row
            assertEquals("3", result.getValueAt(2, "id"), "Third row, id column should be '3'");
            assertEquals("Charlie", result.getValueAt(2, "name"), "Third row, name column should be 'Charlie'");
            assertEquals("35", result.getValueAt(2, "age"), "Third row, age column should be '35'");

            logger.info("Successfully verified query results");
        } finally {
            // Reset the system property
            System.clearProperty("sqlite.connectionString");
        }
    }

    @Test
    public void testExecuteQueryWithCustomProperties() throws Exception {
        logger.info("Testing executeQuery with custom properties");

        // Create a database connection for the test database
        DbConnection connection = dbConnectionManager.createConnection(TEST_DB_URL, "", "");

        try {
            // Connect to the database
            dbConnectionManager.connect(connection);

            // Execute a simple query
            String query = "SELECT * FROM " + TEST_TABLE_NAME + " WHERE age > 25 ORDER BY age";
            ITable result = queryManager.executeQuery(query, connection);

            // Verify the results
            assertNotNull(result, "Result should not be null");
            assertEquals(2, result.getRowCount(), "Result should have 2 rows");
            assertEquals(3, result.getColumnCount(), "Result should have 3 columns");

            // Check the values in the first row (should be Alice)
            assertEquals("1", result.getValueAt(0, "id"), "First row, id column should be '1'");
            assertEquals("Alice", result.getValueAt(0, "name"), "First row, name column should be 'Alice'");
            assertEquals("30", result.getValueAt(0, "age"), "First row, age column should be '30'");

            // Check the values in the second row (should be Charlie)
            assertEquals("3", result.getValueAt(1, "id"), "Second row, id column should be '3'");
            assertEquals("Charlie", result.getValueAt(1, "name"), "Second row, name column should be 'Charlie'");
            assertEquals("35", result.getValueAt(1, "age"), "Second row, age column should be '35'");

            logger.info("Successfully verified query results with custom properties");
        } finally {
            // Reset the system property
            System.clearProperty("sqlite.connectionString");
        }
    }

    @Test
    public void testExecuteQueryWithConnection() throws Exception {
        logger.info("Testing executeQuery with connection");

        // Create a database connection
        DbConnection connection = dbConnectionManager.createConnection(TEST_DB_URL, "", "");

        try {
            // Connect to the database
            dbConnectionManager.connect(connection);

            // Execute a simple query
            String query = "SELECT * FROM " + TEST_TABLE_NAME + " WHERE name LIKE 'B%'";
            ITable result = queryManager.executeQuery(query, connection);

            // Verify the results
            assertNotNull(result, "Result should not be null");
            assertEquals(1, result.getRowCount(), "Result should have 1 row");
            assertEquals(3, result.getColumnCount(), "Result should have 3 columns");

            // Check the values in the first row (should be Bob)
            assertEquals("2", result.getValueAt(0, "id"), "First row, id column should be '2'");
            assertEquals("Bob", result.getValueAt(0, "name"), "First row, name column should be 'Bob'");
            assertEquals("25", result.getValueAt(0, "age"), "First row, age column should be '25'");

            logger.info("Successfully verified query results with connection");
        } finally {
            // Ensure connection is closed
            dbConnectionManager.ensureConnectionClosed(connection);
        }
    }

    @Test
    public void testExecuteQueryWithInvalidQuery() {
        logger.info("Testing executeQuery with invalid query");

        // Create a database connection
        DbConnection connection = null;

        try {
            connection = dbConnectionManager.createConnection(TEST_DB_URL, "", "");
            dbConnectionManager.connect(connection);

            // Execute an invalid query
            final String query = "SELECT * FROM nonexistent_table";
            final DbConnection finalConnection = connection;

            // This should throw an exception
            assertThrows(SQLException.class, () -> {
                queryManager.executeQuery(query, finalConnection);
            }, "Executing an invalid query should throw SQLException");

            logger.info("Successfully verified that invalid query throws exception");
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            fail("Unexpected error: " + e.getMessage());
        } finally {
            // Ensure connection is closed
            if (connection != null) {
                dbConnectionManager.ensureConnectionClosed(connection);
            }
        }
    }

    @Test
    public void testExecuteQueryWithEmptyQuery() {
        logger.info("Testing executeQuery with empty query");

        // This should throw an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            queryManager.executeQuery("", new DatabaseProperties());
        }, "Executing an empty query should throw IllegalArgumentException");

        assertThrows(IllegalArgumentException.class, () -> {
            queryManager.executeQuery(null, new DatabaseProperties());
        }, "Executing a null query should throw IllegalArgumentException");

        logger.info("Successfully verified that empty query throws exception");
    }

    @Test
    public void testExecuteQueryWithNullConnection() {
        logger.info("Testing executeQuery with null connection");

        // This should throw an IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            queryManager.executeQuery("SELECT * FROM " + TEST_TABLE_NAME, (DbConnection) null);
        }, "Executing a query with null connection should throw IllegalArgumentException");

        logger.info("Successfully verified that null connection throws exception");
    }

    @Test
    public void testGenerateSelectStatement() {
        logger.info("Testing generateSelectStatement with standard configuration");

        // Create a mapping configuration for testing
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("jdbc:sqlite:test.db")
                .setOption("tableName", TEST_TABLE_NAME);

        // Add column mappings
        config.addColumnMapping(new ColumnMapping("source_id", "id", "int"));
        config.addColumnMapping(new ColumnMapping("source_name", "name", "string"));
        config.addColumnMapping(new ColumnMapping("source_age", "age", "int"));

        // Generate the SELECT statement
        String sql = queryManager.generateSelectStatement(config);

        // Verify the SQL statement
        String expectedSql = "SELECT id, name, age FROM " + TEST_TABLE_NAME;
        assertEquals(expectedSql, sql, "Generated SQL should match expected SQL");

        logger.debug("Successfully verified that generateSelectStatement generates the correct SQL");
    }

    @Test
    public void testGenerateSelectStatementWithAliases() {
        logger.info("Testing generateSelectStatement with aliases configuration");

        // Create a mapping configuration for testing with aliases
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("jdbc:sqlite:test.db")
                .setOption("tableName", TEST_TABLE_NAME)
                .setOption("useAliases", true);

        // Add column mappings
        config.addColumnMapping(new ColumnMapping("source_id", "id", "int"));
        config.addColumnMapping(new ColumnMapping("source_name", "name", "string"));
        config.addColumnMapping(new ColumnMapping("source_age", "age", "int"));

        // Generate the SELECT statement
        String sql = queryManager.generateSelectStatement(config);

        // Verify the SQL statement
        String expectedSql = "SELECT id AS source_id, name AS source_name, age AS source_age FROM " + TEST_TABLE_NAME;
        assertEquals(expectedSql, sql, "Generated SQL with aliases should match expected SQL");

        logger.debug("Successfully verified that generateSelectStatement generates the correct SQL with aliases");
    }
}
