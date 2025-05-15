package dev.mars.jtable.io.files.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.ColumnFactory;
import dev.mars.jtable.io.common.datasource.DbConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JDBC operations with date, time, and datetime data types.
 * This class tests reading and writing date, time, and datetime values to and from a database.
 */
public class JDBCDateTimeTypeTest {

    private static final String TEST_DB_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";
    private static final String TEST_TABLE = "datetime_test_table";
    private Table table;
    private JDBCTableAdapter adapter;
    private JDBCReader reader;
    private JDBCWriter writer;
    private DbConnection connection;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new Table instance for each test
        table = new Table();

        // Create the adapter with the table
        adapter = new JDBCTableAdapter(table);

        // Create the reader and writer
        reader = new JDBCReader();
        writer = new JDBCWriter();

        // Create the connection
        connection = new DbConnection(TEST_DB_URL, "", "");
        assertTrue(connection.connect(), "Connection should be established");

        // Set up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create a test table with date, time, and datetime columns
            stmt.execute("CREATE TABLE " + TEST_TABLE + " (" +
                    "id INT, " +
                    "name VARCHAR(255), " +
                    "birth_date DATE, " +
                    "start_time TIME, " +
                    "created_at TIMESTAMP" +
                    ")");

            // Insert test data
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES " +
                    "(1, 'Alice', '1990-01-15', '09:30:00', '2023-05-20T14:30:00')");
            stmt.execute("INSERT INTO " + TEST_TABLE + " VALUES " +
                    "(2, 'Bob', '1985-03-22', '10:45:00', '2023-05-21T09:15:00')");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up the test database
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS " + TEST_TABLE);
        }

        // Disconnect
        connection.disconnect();
    }

    @Test
    void testReadDateTimeTypes() {
        // Set up options
        Map<String, Object> options = new HashMap<>();
        options.put("tableName", TEST_TABLE);

        // Read data from the database
        reader.readData(adapter, connection, options);

        // Verify the table content
        assertEquals(2, table.getRowCount(), "Table should have 2 rows");
        assertEquals(5, table.getColumnCount(), "Table should have 5 columns");

        // Check column names (H2 database converts column names to uppercase)
        assertEquals("ID", table.getColumnName(0), "First column should be 'ID'");
        assertEquals("NAME", table.getColumnName(1), "Second column should be 'NAME'");
        assertEquals("BIRTH_DATE", table.getColumnName(2), "Third column should be 'BIRTH_DATE'");
        assertEquals("START_TIME", table.getColumnName(3), "Fourth column should be 'START_TIME'");
        assertEquals("CREATED_AT", table.getColumnName(4), "Fifth column should be 'CREATED_AT'");

        // Check row values for Alice
        assertEquals("1", table.getValueAt(0, "ID"), "First row, ID column should be '1'");
        assertEquals("Alice", table.getValueAt(0, "NAME"), "First row, NAME column should be 'Alice'");
        assertEquals("1990-01-15", table.getValueAt(0, "BIRTH_DATE"), "First row, BIRTH_DATE column should be '1990-01-15'");
        assertEquals("09:30:00", table.getValueAt(0, "START_TIME"), "First row, START_TIME column should be '09:30:00'");
        assertEquals("2023-05-20 14:30:00", table.getValueAt(0, "CREATED_AT"), "First row, CREATED_AT column should be '2023-05-20 14:30:00'");

        // Check row values for Bob
        assertEquals("2", table.getValueAt(1, "ID"), "Second row, ID column should be '2'");
        assertEquals("Bob", table.getValueAt(1, "NAME"), "Second row, NAME column should be 'Bob'");
        assertEquals("1985-03-22", table.getValueAt(1, "BIRTH_DATE"), "Second row, BIRTH_DATE column should be '1985-03-22'");
        assertEquals("10:45:00", table.getValueAt(1, "START_TIME"), "Second row, START_TIME column should be '10:45:00'");
        assertEquals("2023-05-21 09:15:00", table.getValueAt(1, "CREATED_AT"), "Second row, CREATED_AT column should be '2023-05-21 09:15:00'");
    }

    @Test
    void testWriteDateTimeTypes() throws Exception {
        // Create a table with date, time, and datetime columns
        Table sourceTable = new Table();

        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("ID", "int");
        columns.put("NAME", "string");
        columns.put("BIRTH_DATE", "date");
        columns.put("START_TIME", "time");
        columns.put("CREATED_AT", "datetime");
        sourceTable.setColumns(columns);

        // Add rows with date, time, and datetime values
        Map<String, String> row1 = new HashMap<>();
        row1.put("ID", "3");
        row1.put("NAME", "Charlie");
        row1.put("BIRTH_DATE", "1995-07-10");
        row1.put("START_TIME", "08:15:00");
        row1.put("CREATED_AT", "2023-05-22T11:45:00");
        sourceTable.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("ID", "4");
        row2.put("NAME", "Diana");
        row2.put("BIRTH_DATE", "1992-11-30");
        row2.put("START_TIME", "14:20:00");
        row2.put("CREATED_AT", "2023-05-23T16:30:00");
        sourceTable.addRow(row2);

        // Set up options for writing
        Map<String, Object> writeOptions = new HashMap<>();
        writeOptions.put("tableName", TEST_TABLE);
        writeOptions.put("createTable", false); // Table already exists

        // Write data to the database
        JDBCTableAdapter sourceAdapter = new JDBCTableAdapter(sourceTable);
        writer.writeData(sourceAdapter, connection, writeOptions);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " WHERE id IN (3, 4) ORDER BY id")) {

            // Check the first row (Charlie)
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(3, rs.getInt("id"), "First row id should be 3");
            assertEquals("Charlie", rs.getString("name"), "First row name should be Charlie");
            assertEquals("1995-07-10", rs.getDate("birth_date").toString(), "First row birth_date should be 1995-07-10");
            assertEquals("08:15:00", rs.getTime("start_time").toString(), "First row start_time should be 08:15:00");
            assertEquals("2023-05-22 11:45:00.0", rs.getTimestamp("created_at").toString().replace('T', ' '), 
                    "First row created_at should be 2023-05-22T11:45:00");

            // Check the second row (Diana)
            assertTrue(rs.next(), "Result set should have at least two rows");
            assertEquals(4, rs.getInt("id"), "Second row id should be 4");
            assertEquals("Diana", rs.getString("name"), "Second row name should be Diana");
            assertEquals("1992-11-30", rs.getDate("birth_date").toString(), "Second row birth_date should be 1992-11-30");
            assertEquals("14:20:00", rs.getTime("start_time").toString(), "Second row start_time should be 14:20:00");
            assertEquals("2023-05-23 16:30:00.0", rs.getTimestamp("created_at").toString().replace('T', ' '), 
                    "Second row created_at should be 2023-05-23T16:30:00");

            // There should be no more rows with IDs 3 or 4
            assertFalse(rs.next(), "Result set should have exactly two rows with IDs 3 and 4");
        }
    }

    @Test
    void testReadWriteDateTimeTypes() throws Exception {
        // First, read the existing data
        Map<String, Object> readOptions = new HashMap<>();
        readOptions.put("tableName", TEST_TABLE);
        reader.readData(adapter, connection, readOptions);

        // Verify the data was read correctly
        assertEquals(2, table.getRowCount(), "Table should have 2 rows after reading");

        // Add a new row with date, time, and datetime values
        Map<String, String> newRow = new HashMap<>();
        newRow.put("ID", "5");
        newRow.put("NAME", "Eve");
        newRow.put("BIRTH_DATE", "1988-09-05");
        newRow.put("START_TIME", "12:00:00");
        newRow.put("CREATED_AT", "2023-05-24T10:00:00");
        table.addRow(newRow);

        // Now we have 3 rows (2 read from DB + 1 new)
        assertEquals(3, table.getRowCount(), "Table should have 3 rows after adding a new row");

        // Write the data back to the database
        Map<String, Object> writeOptions = new HashMap<>();
        writeOptions.put("tableName", TEST_TABLE);
        writeOptions.put("createTable", false); // Table already exists
        writer.writeData(adapter, connection, writeOptions);

        // Verify the data was written correctly
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + TEST_TABLE + " WHERE id = 5")) {

            // Check the new row (Eve)
            assertTrue(rs.next(), "Result set should have one row");
            assertEquals(5, rs.getInt("id"), "Row id should be 5");
            assertEquals("Eve", rs.getString("name"), "Row name should be Eve");
            assertEquals("1988-09-05", rs.getDate("birth_date").toString(), "Row birth_date should be 1988-09-05");
            assertEquals("12:00:00", rs.getTime("start_time").toString(), "Row start_time should be 12:00:00");
            assertEquals("2023-05-24 10:00:00.0", rs.getTimestamp("created_at").toString().replace('T', ' '), 
                    "Row created_at should be 2023-05-24T10:00:00");

            // There should be no more rows with ID 5
            assertFalse(rs.next(), "Result set should have exactly one row with ID 5");
        }

        // Verify that the new row exists in the table
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + TEST_TABLE + " WHERE id = 5")) {

            assertTrue(rs.next(), "Result set should have one row");
            assertEquals(1, rs.getInt(1), "Table should have exactly one row with ID 5");
        }
    }
}
