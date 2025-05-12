package dev.mars.jtable.core.model;

import dev.mars.jtable.core.table.Column;
import dev.mars.jtable.core.table.ColumnFactory;
import dev.mars.jtable.core.table.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the date and time data types.
 * This class tests the functionality of the date, time, and datetime data types.
 */
class DateTimeTypeTest {

    private Table table;

    @BeforeEach
    void setUp() {
        table = new Table();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("Name", "string");
        columnNames.put("BirthDate", "date");
        columnNames.put("StartTime", "time");
        columnNames.put("CreatedAt", "datetime");
        table.setColumns(columnNames);
    }

    @Test
    void testCreateDateTimeColumns() {
        // Verify that the columns were created with the correct types
        assertEquals(4, table.getColumnCount());

        IColumn<?> nameColumn = table.getColumn("Name");
        IColumn<?> birthDateColumn = table.getColumn("BirthDate");
        IColumn<?> startTimeColumn = table.getColumn("StartTime");
        IColumn<?> createdAtColumn = table.getColumn("CreatedAt");

        assertEquals(String.class, nameColumn.getType());
        assertEquals(LocalDate.class, birthDateColumn.getType());
        assertEquals(LocalTime.class, startTimeColumn.getType());
        assertEquals(LocalDateTime.class, createdAtColumn.getType());
    }

    @Test
    void testAddRowWithDateTimeValues() {
        // Create a row with date and time values
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("BirthDate", "1990-01-15");
        row.put("StartTime", "09:30:00");
        row.put("CreatedAt", "2023-05-20T14:30:00");

        // Add the row to the table
        table.addRow(row);

        // Verify that the values were added correctly
        assertEquals(1, table.getRowCount());
        assertEquals("Alice", table.getValueAt(0, "Name"));
        assertEquals("1990-01-15", table.getValueAt(0, "BirthDate"));
        assertEquals("09:30:00", table.getValueAt(0, "StartTime"));
        assertEquals("2023-05-20T14:30:00", table.getValueAt(0, "CreatedAt"));

        // Verify that the values were converted to the correct types
        Object birthDateValue = table.getValueObject(0, "BirthDate");
        Object startTimeValue = table.getValueObject(0, "StartTime");
        Object createdAtValue = table.getValueObject(0, "CreatedAt");

        assertTrue(birthDateValue instanceof LocalDate);
        assertTrue(startTimeValue instanceof LocalTime);
        assertTrue(createdAtValue instanceof LocalDateTime);

        // Verify the actual values
        LocalDate expectedBirthDate = LocalDate.parse("1990-01-15");
        LocalTime expectedStartTime = LocalTime.parse("09:30:00");
        LocalDateTime expectedCreatedAt = LocalDateTime.parse("2023-05-20T14:30:00");

        assertEquals(expectedBirthDate, birthDateValue);
        assertEquals(expectedStartTime, startTimeValue);
        assertEquals(expectedCreatedAt, createdAtValue);
    }

    @Test
    void testSetValueAtWithDateTimeValues() {
        // Create a row with initial values
        Map<String, String> row = new HashMap<>();
        row.put("Name", "Alice");
        row.put("BirthDate", "1990-01-15");
        row.put("StartTime", "09:30:00");
        row.put("CreatedAt", "2023-05-20T14:30:00");
        table.addRow(row);

        // Update the values
        table.setValueAt(0, "BirthDate", "1995-03-20");
        table.setValueAt(0, "StartTime", "10:45:30");
        table.setValueAt(0, "CreatedAt", "2023-06-15T08:15:00");

        // Verify that the values were updated correctly
        assertEquals("1995-03-20", table.getValueAt(0, "BirthDate"));
        assertEquals("10:45:30", table.getValueAt(0, "StartTime"));
        assertEquals("2023-06-15T08:15:00", table.getValueAt(0, "CreatedAt"));

        // Verify that the values were converted to the correct types
        Object birthDateValue = table.getValueObject(0, "BirthDate");
        Object startTimeValue = table.getValueObject(0, "StartTime");
        Object createdAtValue = table.getValueObject(0, "CreatedAt");

        assertTrue(birthDateValue instanceof LocalDate);
        assertTrue(startTimeValue instanceof LocalTime);
        assertTrue(createdAtValue instanceof LocalDateTime);

        // Verify the actual values
        LocalDate expectedBirthDate = LocalDate.parse("1995-03-20");
        LocalTime expectedStartTime = LocalTime.parse("10:45:30");
        LocalDateTime expectedCreatedAt = LocalDateTime.parse("2023-06-15T08:15:00");

        assertEquals(expectedBirthDate, birthDateValue);
        assertEquals(expectedStartTime, startTimeValue);
        assertEquals(expectedCreatedAt, createdAtValue);
    }

    @ParameterizedTest
    @CsvSource({
        "2023-01-15, date",
        "1990-12-31, date",
        "2000-02-29, date"  // Leap year
    })
    void testInferTypeDate(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @ParameterizedTest
    @CsvSource({
        "09:30:00, time",
        "23:59:59, time",
        "00:00:00, time"
    })
    void testInferTypeTime(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @ParameterizedTest
    @CsvSource({
        "2023-01-15T09:30:00, datetime",
        "1990-12-31T23:59:59, datetime",
        "2000-02-29T00:00:00, datetime"  // Leap year
    })
    void testInferTypeDateTime(String value, String expectedType) {
        assertEquals(expectedType, table.inferType(value));
    }

    @Test
    void testInferTypeDateTimeEdgeCases() {
        // Invalid date formats
        assertEquals("string", table.inferType("2023/01/15"));
        assertEquals("string", table.inferType("15-01-2023"));
        assertEquals("string", table.inferType("Jan 15, 2023"));

        // Invalid time formats
        assertEquals("string", table.inferType("9:30:00"));  // Missing leading zero
        assertEquals("string", table.inferType("09:30"));    // Missing seconds
        assertEquals("string", table.inferType("09:30:00 AM"));

        // Invalid datetime formats
        assertEquals("string", table.inferType("2023-01-15 09:30:00"));  // Space instead of T
        assertEquals("string", table.inferType("2023-01-15T9:30:00"));   // Missing leading zero in time
        assertEquals("string", table.inferType("2023-01-15T09:30"));     // Missing seconds
    }

    @Test
    void testColumnFactoryDateTimeColumns() {
        // Test creating date column
        IColumn<LocalDate> dateColumn = ColumnFactory.createDateColumn("TestDate");
        assertEquals("TestDate", dateColumn.getName());
        assertEquals(LocalDate.class, dateColumn.getType());
        assertNotNull(dateColumn.createDefaultValue());
        assertTrue(dateColumn.createDefaultValue() instanceof LocalDate);

        // Test creating time column
        IColumn<LocalTime> timeColumn = ColumnFactory.createTimeColumn("TestTime");
        assertEquals("TestTime", timeColumn.getName());
        assertEquals(LocalTime.class, timeColumn.getType());
        assertNotNull(timeColumn.createDefaultValue());
        assertTrue(timeColumn.createDefaultValue() instanceof LocalTime);

        // Test creating datetime column
        IColumn<LocalDateTime> dateTimeColumn = ColumnFactory.createDateTimeColumn("TestDateTime");
        assertEquals("TestDateTime", dateTimeColumn.getName());
        assertEquals(LocalDateTime.class, dateTimeColumn.getType());
        assertNotNull(dateTimeColumn.createDefaultValue());
        assertTrue(dateTimeColumn.createDefaultValue() instanceof LocalDateTime);
    }

    @Test
    void testDateTimeColumnConversion() {
        // Test date column conversion
        Column<LocalDate> dateColumn = new Column<>("TestDate", LocalDate.class, LocalDate.now());
        LocalDate date = dateColumn.convertFromString("2023-01-15");
        assertEquals(LocalDate.of(2023, 1, 15), date);

        // Test time column conversion
        Column<LocalTime> timeColumn = new Column<>("TestTime", LocalTime.class, LocalTime.now());
        LocalTime time = timeColumn.convertFromString("09:30:00");
        assertEquals(LocalTime.of(9, 30, 0), time);

        // Test datetime column conversion
        Column<LocalDateTime> dateTimeColumn = new Column<>("TestDateTime", LocalDateTime.class, LocalDateTime.now());
        LocalDateTime dateTime = dateTimeColumn.convertFromString("2023-01-15T09:30:00");
        assertEquals(LocalDateTime.of(2023, 1, 15, 9, 30, 0), dateTime);
    }

    @Test
    void testDateTimeColumnConversionExceptions() {
        // Test date column conversion with invalid format
        Column<LocalDate> dateColumn = new Column<>("TestDate", LocalDate.class, LocalDate.now());
        assertThrows(IllegalArgumentException.class, () -> dateColumn.convertFromString("2023/01/15"));

        // Test time column conversion with invalid format
        Column<LocalTime> timeColumn = new Column<>("TestTime", LocalTime.class, LocalTime.now());
        assertThrows(IllegalArgumentException.class, () -> timeColumn.convertFromString("9:30:00"));

        // Test datetime column conversion with invalid format
        Column<LocalDateTime> dateTimeColumn = new Column<>("TestDateTime", LocalDateTime.class, LocalDateTime.now());
        assertThrows(IllegalArgumentException.class, () -> dateTimeColumn.convertFromString("2023-01-15 09:30:00"));
    }

    @Test
    void testGetDefaultValueForDateTimeTypes() {
        // Test default value for date type
        String dateDefaultValue = table.getDefaultValue("date");
        assertNotNull(dateDefaultValue);
        assertTrue(dateDefaultValue.matches("\\d{4}-\\d{2}-\\d{2}"));

        // Test default value for time type
        String timeDefaultValue = table.getDefaultValue("time");
        assertNotNull(timeDefaultValue);
        assertTrue(timeDefaultValue.matches("\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"));

        // Test default value for datetime type
        String dateTimeDefaultValue = table.getDefaultValue("datetime");
        assertNotNull(dateTimeDefaultValue);
        assertTrue(dateTimeDefaultValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?"));
    }
}
