package dev.mars.jtable.core.model;

import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation test for date, time, and datetime data types.
 * This test confirms that the java-table-core module properly supports these types.
 */
public class DateTimeTypeValidationTest {

    @Test
    public void testDateTimeTypesWithSampleData() {
        // Create a table with date, time, and datetime columns
        TableCore table = new TableCore();
        var columnNames = new LinkedHashMap<String, String>();
        columnNames.put("EventName", "string");
        columnNames.put("EventDate", "date");
        columnNames.put("EventTime", "time");
        columnNames.put("EventDateTime", "datetime");
        table.setColumns(columnNames);

        // Add some sample data
        Map<String, String> row1 = new HashMap<>();
        row1.put("EventName", "Conference");
        row1.put("EventDate", "2023-06-15");
        row1.put("EventTime", "09:00:00");
        row1.put("EventDateTime", "2023-06-15T09:00:00");
        table.addRow(row1);

        Map<String, String> row2 = new HashMap<>();
        row2.put("EventName", "Meeting");
        row2.put("EventDate", "2023-06-16");
        row2.put("EventTime", "14:30:00");
        row2.put("EventDateTime", "2023-06-16T14:30:00");
        table.addRow(row2);

        // Verify the data was added correctly
        assertEquals(2, table.getRowCount());
        assertEquals(4, table.getColumnCount());

        // Verify the first row
        assertEquals("Conference", table.getValueAt(0, "EventName"));
        assertEquals("2023-06-15", table.getValueAt(0, "EventDate"));
        assertEquals("09:00:00", table.getValueAt(0, "EventTime"));
        assertEquals("2023-06-15T09:00:00", table.getValueAt(0, "EventDateTime"));

        // Verify the second row
        assertEquals("Meeting", table.getValueAt(1, "EventName"));
        assertEquals("2023-06-16", table.getValueAt(1, "EventDate"));
        assertEquals("14:30:00", table.getValueAt(1, "EventTime"));
        assertEquals("2023-06-16T14:30:00", table.getValueAt(1, "EventDateTime"));

        // Verify the types of the values
        Object eventDateValue = table.getValueObject(0, "EventDate");
        Object eventTimeValue = table.getValueObject(0, "EventTime");
        Object eventDateTimeValue = table.getValueObject(0, "EventDateTime");

        assertTrue(eventDateValue instanceof LocalDate);
        assertTrue(eventTimeValue instanceof LocalTime);
        assertTrue(eventDateTimeValue instanceof LocalDateTime);

        // Verify the actual values
        LocalDate expectedDate = LocalDate.parse("2023-06-15");
        LocalTime expectedTime = LocalTime.parse("09:00:00");
        LocalDateTime expectedDateTime = LocalDateTime.parse("2023-06-15T09:00:00");

        assertEquals(expectedDate, eventDateValue);
        assertEquals(expectedTime, eventTimeValue);
        assertEquals(expectedDateTime, eventDateTimeValue);

        // Test updating values
        table.setValueAt(0, "EventDate", "2023-07-01");
        table.setValueAt(0, "EventTime", "10:15:30");
        table.setValueAt(0, "EventDateTime", "2023-07-01T10:15:30");

        // Verify the updated values
        assertEquals("2023-07-01", table.getValueAt(0, "EventDate"));
        assertEquals("10:15:30", table.getValueAt(0, "EventTime"));
        assertEquals("2023-07-01T10:15:30", table.getValueAt(0, "EventDateTime"));

        // Verify the updated types and values
        eventDateValue = table.getValueObject(0, "EventDate");
        eventTimeValue = table.getValueObject(0, "EventTime");
        eventDateTimeValue = table.getValueObject(0, "EventDateTime");

        assertTrue(eventDateValue instanceof LocalDate);
        assertTrue(eventTimeValue instanceof LocalTime);
        assertTrue(eventDateTimeValue instanceof LocalDateTime);

        expectedDate = LocalDate.parse("2023-07-01");
        expectedTime = LocalTime.parse("10:15:30");
        expectedDateTime = LocalDateTime.parse("2023-07-01T10:15:30");

        assertEquals(expectedDate, eventDateValue);
        assertEquals(expectedTime, eventTimeValue);
        assertEquals(expectedDateTime, eventDateTimeValue);
    }
}