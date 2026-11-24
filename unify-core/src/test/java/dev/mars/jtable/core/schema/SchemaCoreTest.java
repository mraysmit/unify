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
package dev.mars.jtable.core.schema;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static org.junit.jupiter.api.Assertions.*;

class SchemaCoreTest {

    @Test
    void schemaKeepsOrderAndLooksUpNamesCaseSensitively() {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("Name", LogicalType.STRING));
        assertEquals(2, schema.size());
        assertEquals("id", schema.column(0).name());
        assertEquals(1, schema.indexOf("Name"));
        assertEquals(-1, schema.indexOf("name"));
        assertThrows(UnsupportedOperationException.class,
                () -> schema.columns().add(nullable("x", LogicalType.STRING)));
    }

    @Test
    void schemaRejectsDuplicateBlankAndEmpty() {
        assertThrows(IllegalArgumentException.class,
                () -> TableSchema.of(nullable("a", LogicalType.STRING), nullable("a", LogicalType.INT32)));
        assertThrows(IllegalArgumentException.class, () -> nullable(" ", LogicalType.STRING));
        assertThrows(IllegalArgumentException.class, TableSchema::of);
    }

    @Test
    void textConversionAcceptsCanonicalForms() {
        assertEquals(-42, ValueNormalizer.fromText("-42", required("n", LogicalType.INT32)));
        assertEquals(1.5e3, ValueNormalizer.fromText("1.5e3", required("d", LogicalType.DOUBLE)));
        assertEquals(0.25, ValueNormalizer.fromText(".25", required("d", LogicalType.DOUBLE)));
        assertEquals(true, ValueNormalizer.fromText("TRUE", required("b", LogicalType.BOOLEAN)));
        assertEquals(LocalDate.of(2026, 11, 24), ValueNormalizer.fromText("2026-11-24", required("t", LogicalType.DATE)));
        assertEquals(LocalTime.of(9, 30), ValueNormalizer.fromText("09:30", required("t", LogicalType.TIME)));
        assertEquals(LocalDateTime.of(2026, 11, 24, 9, 30, 5),
                ValueNormalizer.fromText("2026-11-24T09:30:05", required("t", LogicalType.LOCAL_DATETIME)));
        assertEquals("", ValueNormalizer.fromText("", required("s", LogicalType.STRING)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " 5", "5 ", "1.0", "0x10", "99999999999", "1e3"})
    void int32RejectsAnythingButAnInRangeWholeNumber(String text) {
        assertThrows(ValueConversionException.class, () -> ValueNormalizer.fromText(text, required("n", LogicalType.INT32)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NaN", "Infinity", "-Infinity", "1e999", "1.5d", "0x1p3", "", "1,5"})
    void doubleRejectsNonFiniteAndNonDecimalText(String text) {
        assertThrows(ValueConversionException.class, () -> ValueNormalizer.fromText(text, required("d", LogicalType.DOUBLE)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yes", "no", "1", "0", "t", ""})
    void booleanRejectsPermissiveTokens(String text) {
        assertThrows(ValueConversionException.class, () -> ValueNormalizer.fromText(text, required("b", LogicalType.BOOLEAN)));
    }

    @Test
    void nullIsAllowedOnlyInNullableColumnsAndNeverReplacedByADefault() {
        assertNull(ValueNormalizer.fromText(null, nullable("d", LogicalType.DATE)));
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> ValueNormalizer.fromText(null, required("d", LogicalType.DATE)));
        assertEquals("d", e.column());
    }

    @Test
    void errorsNameTheColumnButNotTheValue() {
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> ValueNormalizer.fromText("secret-token", required("amount", LogicalType.DOUBLE)));
        assertTrue(e.getMessage().contains("amount"));
        assertFalse(e.getMessage().contains("secret-token"));
    }

    @Test
    void canonicalTextRoundTripsWithoutLocaleEffects() {
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            ColumnDefinition d = required("d", LogicalType.DOUBLE);
            for (double value : new double[]{1.5, 1.0e10, 1.0e-7, -0.0, 123456.789}) {
                String text = ValueNormalizer.toText(value);
                assertFalse(text.contains(","), text);
                assertEquals(value, ValueNormalizer.fromText(text, d));
            }
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void batchChecksEachRecordWholeBeforeAddingIt() {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32), nullable("name", LogicalType.STRING));
        RecordBatch.Builder builder = RecordBatch.builder(schema, 2);
        builder.add(1, "a");
        assertThrows(ValueConversionException.class, () -> builder.add(2, 3));
        assertThrows(ValueConversionException.class, () -> builder.add(null, "b"));
        assertThrows(IllegalArgumentException.class, () -> builder.add(4));
        builder.add(5, null);
        RecordBatch batch = builder.build();
        assertEquals(2, batch.rowCount());
        assertEquals(5, batch.value(1, 0));
        assertThrows(IllegalStateException.class, () -> builder.add(6, "c"));
        Object[] copy = batch.row(0);
        copy[0] = 99;
        assertEquals(1, batch.value(0, 0), "row() returns a copy");
    }
}
