/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.io.common.mapping;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for date, time, and datetime data types in mapping configurations.
 * This class tests that MappingConfiguration properly handles date, time, and datetime types.
 */
public class DateTimeTypeMappingTest {

    @Test
    void testDateTimeColumnMappings() {
        // Create a mapping configuration with date, time, and datetime columns
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("test-source")
                .addColumnMapping(new ColumnMapping("source_date", "targetDate", "date"))
                .addColumnMapping(new ColumnMapping("source_time", "targetTime", "time"))
                .addColumnMapping(new ColumnMapping("source_datetime", "targetDateTime", "datetime"));

        // Verify the column mappings
        assertEquals(3, config.getColumnMappings().size(), "Configuration should have 3 column mappings");

        // Verify the date column mapping
        ColumnMapping dateMapping = config.getColumnMappings().get(0);
        assertEquals("source_date", dateMapping.getSourceColumnName(), "Source column name should be 'source_date'");
        assertEquals("targetDate", dateMapping.getTargetColumnName(), "Target column name should be 'targetDate'");
        assertEquals("date", dateMapping.getTargetColumnType(), "Target column type should be 'date'");

        // Verify the time column mapping
        ColumnMapping timeMapping = config.getColumnMappings().get(1);
        assertEquals("source_time", timeMapping.getSourceColumnName(), "Source column name should be 'source_time'");
        assertEquals("targetTime", timeMapping.getTargetColumnName(), "Target column name should be 'targetTime'");
        assertEquals("time", timeMapping.getTargetColumnType(), "Target column type should be 'time'");

        // Verify the datetime column mapping
        ColumnMapping datetimeMapping = config.getColumnMappings().get(2);
        assertEquals("source_datetime", datetimeMapping.getSourceColumnName(), "Source column name should be 'source_datetime'");
        assertEquals("targetDateTime", datetimeMapping.getTargetColumnName(), "Target column name should be 'targetDateTime'");
        assertEquals("datetime", datetimeMapping.getTargetColumnType(), "Target column type should be 'datetime'");
    }

    @Test
    void testCreateColumnDefinitions() {
        // Create a mapping configuration with date, time, and datetime columns
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("test-source")
                .addColumnMapping(new ColumnMapping("source_date", "targetDate", "date"))
                .addColumnMapping(new ColumnMapping("source_time", "targetTime", "time"))
                .addColumnMapping(new ColumnMapping("source_datetime", "targetDateTime", "datetime"));

        // Create column definitions
        LinkedHashMap<String, String> columnDefs = config.createColumnDefinitions();

        // Verify the column definitions
        assertEquals(3, columnDefs.size(), "Column definitions should have 3 entries");
        assertEquals("date", columnDefs.get("targetDate"), "targetDate column type should be 'date'");
        assertEquals("time", columnDefs.get("targetTime"), "targetTime column type should be 'time'");
        assertEquals("datetime", columnDefs.get("targetDateTime"), "targetDateTime column type should be 'datetime'");
    }

    @Test
    void testDateTimeColumnMappingWithDefaultValues() {
        // Create a mapping configuration with date, time, and datetime columns with default values
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("test-source")
                .addColumnMapping(new ColumnMapping("source_date", "targetDate", "date").setDefaultValue("2023-01-01"))
                .addColumnMapping(new ColumnMapping("source_time", "targetTime", "time").setDefaultValue("12:00:00"))
                .addColumnMapping(new ColumnMapping("source_datetime", "targetDateTime", "datetime").setDefaultValue("2023-01-01T12:00:00"));

        // Verify the column mappings
        assertEquals(3, config.getColumnMappings().size(), "Configuration should have 3 column mappings");

        // Verify the date column mapping default value
        ColumnMapping dateMapping = config.getColumnMappings().get(0);
        assertEquals("2023-01-01", dateMapping.getDefaultValue(), "Date default value should be '2023-01-01'");

        // Verify the time column mapping default value
        ColumnMapping timeMapping = config.getColumnMappings().get(1);
        assertEquals("12:00:00", timeMapping.getDefaultValue(), "Time default value should be '12:00:00'");

        // Verify the datetime column mapping default value
        ColumnMapping datetimeMapping = config.getColumnMappings().get(2);
        assertEquals("2023-01-01T12:00:00", datetimeMapping.getDefaultValue(), "Datetime default value should be '2023-01-01T12:00:00'");
    }

    @Test
    void testSerializationWithDateTimeTypes() throws Exception {
        // Create a mapping configuration with date, time, and datetime columns
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("test-source")
                .addColumnMapping(new ColumnMapping("source_date", "targetDate", "date"))
                .addColumnMapping(new ColumnMapping("source_time", "targetTime", "time"))
                .addColumnMapping(new ColumnMapping("source_datetime", "targetDateTime", "datetime"));

        // Serialize to JSON
        IMappingSerializer jsonSerializer = MappingSerializerFactory.createSerializer("json");
        String json = jsonSerializer.serialize(config);

        // Print the JSON string for debugging
        System.out.println("[DEBUG_LOG] JSON: " + json);

        // Verify that the JSON contains the date, time, and datetime types
        // The JSON string has whitespace and formatting, so we need to be careful with the contains check
        assertTrue(json.indexOf("\"targetColumnType\" : \"date\"") > 0, "JSON should contain date type");
        assertTrue(json.indexOf("\"targetColumnType\" : \"time\"") > 0, "JSON should contain time type");
        assertTrue(json.indexOf("\"targetColumnType\" : \"datetime\"") > 0, "JSON should contain datetime type");

        // Deserialize from JSON
        MappingConfiguration deserializedConfig = jsonSerializer.deserialize(json);

        // Verify the deserialized configuration
        assertEquals(3, deserializedConfig.getColumnMappings().size(), "Deserialized configuration should have 3 column mappings");

        // Verify the date column mapping
        ColumnMapping dateMapping = deserializedConfig.getColumnMappings().get(0);
        assertEquals("source_date", dateMapping.getSourceColumnName(), "Source column name should be 'source_date'");
        assertEquals("targetDate", dateMapping.getTargetColumnName(), "Target column name should be 'targetDate'");
        assertEquals("date", dateMapping.getTargetColumnType(), "Target column type should be 'date'");

        // Verify the time column mapping
        ColumnMapping timeMapping = deserializedConfig.getColumnMappings().get(1);
        assertEquals("source_time", timeMapping.getSourceColumnName(), "Source column name should be 'source_time'");
        assertEquals("targetTime", timeMapping.getTargetColumnName(), "Target column name should be 'targetTime'");
        assertEquals("time", timeMapping.getTargetColumnType(), "Target column type should be 'time'");

        // Verify the datetime column mapping
        ColumnMapping datetimeMapping = deserializedConfig.getColumnMappings().get(2);
        assertEquals("source_datetime", datetimeMapping.getSourceColumnName(), "Source column name should be 'source_datetime'");
        assertEquals("targetDateTime", datetimeMapping.getTargetColumnName(), "Target column name should be 'targetDateTime'");
        assertEquals("datetime", datetimeMapping.getTargetColumnType(), "Target column type should be 'datetime'");
    }
}
