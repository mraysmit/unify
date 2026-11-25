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

import dev.mars.jtable.core.schema.TextConversion.Trim;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.mars.jtable.core.schema.ColumnDefinition.builder;
import static dev.mars.jtable.core.schema.ColumnDefinition.nullable;
import static dev.mars.jtable.core.schema.ColumnDefinition.required;
import static dev.mars.jtable.core.schema.ValueNormalizer.normalize;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The seven normalization stages and their order (TD "Conversion and validation pipeline"), and
 * the AC03 distinctions between missing, null, empty and invalid values.
 */
class NormalizationPipelineTest {

    // ---- AC03: missing, null, empty and invalid each follow their own rule ----

    @Test
    void missingTakesTheDefaultOrNullOrFails() {
        ColumnDefinition withDefault = builder("qty", LogicalType.INT32).required().defaultValue(1).build();
        assertEquals(1, normalize(SourceValue.MISSING, withDefault));
        assertNull(normalize(SourceValue.MISSING, nullable("note", LogicalType.STRING)));
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> normalize(SourceValue.MISSING, required("id", LogicalType.INT32)));
        assertEquals("id", e.column());
    }

    @Test
    void explicitNullIsNeverReplacedByTheDefault() {
        ColumnDefinition nullableWithDefault = builder("qty", LogicalType.INT32).defaultValue(1).build();
        assertNull(normalize(SourceValue.NULL, nullableWithDefault));
        ColumnDefinition requiredWithDefault = builder("qty", LogicalType.INT32).required().defaultValue(1).build();
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.NULL, requiredWithDefault));
    }

    @Test
    void emptyStaysEmptyForStringAndFailsForOtherTypes() {
        assertEquals("", normalize(SourceValue.text(""), required("s", LogicalType.STRING)));
        for (LogicalType type : List.of(LogicalType.INT32, LogicalType.DOUBLE, LogicalType.BOOLEAN,
                LogicalType.DATE, LogicalType.TIME, LogicalType.LOCAL_DATETIME)) {
            assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text(""), nullable("c", type)),
                    type.toString());
        }
    }

    @Test
    void emptyAsNullTurnsEmptyIntoNullWithoutTheDefault() {
        ColumnDefinition column = builder("qty", LogicalType.INT32).emptyAsNull(true).defaultValue(1).build();
        assertNull(normalize(SourceValue.text(""), column));
        ColumnDefinition required = builder("qty", LogicalType.INT32).required().emptyAsNull(true).defaultValue(1).build();
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text(""), required));
    }

    @Test
    void invalidValuesAreNeverReplacedByTheDefault() {
        ColumnDefinition column = builder("qty", LogicalType.INT32).defaultValue(1).build();
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("abc"), column));
        ColumnDefinition flag = builder("flag", LogicalType.BOOLEAN).defaultValue(false).build();
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("yes"), flag));
    }

    // ---- Stage 2: trimming only when configured ----

    @Test
    void whitespaceIsPreservedUnlessTrimmingIsConfigured() {
        assertEquals(" a ", normalize(SourceValue.text(" a "), required("s", LogicalType.STRING)));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text(" 5"), required("n", LogicalType.INT32)));

        assertEquals(5, normalize(SourceValue.text(" 5 "), builder("n", LogicalType.INT32).trim(Trim.BOTH).build()));
        assertEquals("a ", normalize(SourceValue.text(" a "), builder("s", LogicalType.STRING).trim(Trim.LEADING).build()));
        assertEquals(" a", normalize(SourceValue.text(" a "), builder("s", LogicalType.STRING).trim(Trim.TRAILING).build()));
    }

    @Test
    void trimmingRunsBeforeEmptyAsNull() {
        ColumnDefinition column = builder("n", LogicalType.INT32).trim(Trim.BOTH).emptyAsNull(true).build();
        assertNull(normalize(SourceValue.text("   "), column));
        ColumnDefinition noTrim = builder("n", LogicalType.INT32).emptyAsNull(true).build();
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("   "), noTrim));
    }

    // ---- Stage 5: temporal patterns ----

    @Test
    void anExplicitPatternIsResolvedStrictly() {
        ColumnDefinition date = builder("d", LogicalType.DATE).pattern("dd/MM/uuuu").build();
        assertEquals(LocalDate.of(2026, 2, 28), normalize(SourceValue.text("28/02/2026"), date));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("31/02/2026"), date));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("2026-02-28"), date));

        ColumnDefinition stamp = builder("t", LogicalType.LOCAL_DATETIME).pattern("uuuu-MM-dd HH:mm").build();
        assertEquals(LocalDateTime.of(2026, 11, 25, 9, 30), normalize(SourceValue.text("2026-11-25 09:30"), stamp));
    }

    @Test
    void canonicalTextConversionIgnoresColumnTextSettings() {
        // Engines read back canonical text; a column pattern must not apply to it.
        ColumnDefinition date = builder("d", LogicalType.DATE).pattern("dd/MM/uuuu").trim(Trim.BOTH).build();
        assertEquals(LocalDate.of(2026, 2, 28), ValueNormalizer.fromText("2026-02-28", date));
    }

    // ---- Stage 7: rules ----

    @Test
    void numericBoundsAreInclusive() {
        ColumnDefinition column = builder("pct", LogicalType.INT32).bounds(0, 100).build();
        assertEquals(0, normalize(SourceValue.text("0"), column));
        assertEquals(100, normalize(SourceValue.text("100"), column));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("-1"), column));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("101"), column));

        ColumnDefinition ratio = builder("r", LogicalType.DOUBLE).bounds(null, 1.0).build();
        assertEquals(-5.0, normalize(SourceValue.text("-5"), ratio));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("1.0001"), ratio));
    }

    @Test
    void lengthIsMeasuredInCodePoints() {
        ColumnDefinition column = builder("code", LogicalType.STRING).length(2, 3).build();
        String twoEmoji = "😀😁"; // 2 code points, 4 UTF-16 units
        assertEquals(twoEmoji, normalize(SourceValue.text(twoEmoji), column));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("a"), column));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("abcd"), column));
    }

    @Test
    void permittedValuesAreTypedAndExact() {
        ColumnDefinition column = builder("status", LogicalType.STRING).permitted(List.of("OPEN", "CLOSED")).build();
        assertEquals("OPEN", normalize(SourceValue.text("OPEN"), column));
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> normalize(SourceValue.text("open"), column));
        assertFalse(e.getMessage().contains("open"), "errors must not repeat the value");

        ColumnDefinition code = builder("code", LogicalType.INT32).permitted(List.of(1, 2, 3)).build();
        assertEquals(2, normalize(SourceValue.text("2"), code));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.text("4"), code));
    }

    @Test
    void rulesApplyToProgrammaticValuesToo() {
        ColumnDefinition column = builder("pct", LogicalType.INT32).bounds(0, 100).build();
        assertEquals(50, ValueNormalizer.checkTyped(50, column));
        assertThrows(ValueConversionException.class, () -> ValueNormalizer.checkTyped(150, column));
    }

    @Test
    void nullIsDecidedByNullabilityNotByRules() {
        ColumnDefinition column = builder("pct", LogicalType.INT32).bounds(1, 100).permitted(List.of(5)).build();
        assertNull(normalize(SourceValue.NULL, column));
    }

    // ---- Configuration is checked when the column is defined ----

    @Test
    void inconsistentRulesAreConfigurationErrors() {
        assertThrows(IllegalArgumentException.class, () -> builder("n", LogicalType.INT32).bounds(10, 1).build());
        assertThrows(IllegalArgumentException.class, () -> builder("n", LogicalType.INT32).bounds(1.5, null).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).bounds(1, 2).build());
        assertThrows(IllegalArgumentException.class, () -> builder("n", LogicalType.INT32).length(1, 2).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).length(3, 2).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).length(-1, 2).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).permitted(List.of(1)).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).permitted(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> builder("d", LogicalType.DOUBLE).bounds(Double.NaN, null).build());
        assertThrows(IllegalArgumentException.class, () -> builder("s", LogicalType.STRING).pattern("dd/MM/uuuu").build());
        assertThrows(IllegalArgumentException.class, () -> builder("d", LogicalType.DATE).pattern("ppp{").build());
    }

    @Test
    void defaultsAreValidatedAgainstTypeAndRules() {
        assertThrows(IllegalArgumentException.class, () -> builder("n", LogicalType.INT32).defaultValue("1").build());
        assertThrows(IllegalArgumentException.class, () -> builder("n", LogicalType.INT32).bounds(0, 10).defaultValue(11).build());
        assertThrows(IllegalArgumentException.class, () -> builder("d", LogicalType.DOUBLE).defaultValue(Double.POSITIVE_INFINITY).build());
        assertThrows(IllegalArgumentException.class,
                () -> builder("s", LogicalType.STRING).permitted(List.of("A")).defaultValue("B").build());
        assertEquals("A", builder("s", LogicalType.STRING).permitted(List.of("A")).defaultValue("A").build().defaultValue());
    }

    // ---- Typed scalars from structured sources ----

    @Test
    void typedScalarsConvertStrictly() {
        assertEquals(7, normalize(SourceValue.of(7L), required("n", LogicalType.INT32)));
        assertEquals(7, normalize(SourceValue.of(new BigDecimal("7")), required("n", LogicalType.INT32)));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.of(7.5), required("n", LogicalType.INT32)));
        assertThrows(ValueConversionException.class,
                () -> normalize(SourceValue.of(new BigDecimal("7.5")), required("n", LogicalType.INT32)));
        assertThrows(ValueConversionException.class,
                () -> normalize(SourceValue.of(3_000_000_000L), required("n", LogicalType.INT32)));
        assertEquals(2.0, normalize(SourceValue.of(2), required("d", LogicalType.DOUBLE)));
        assertEquals(true, normalize(SourceValue.of(Boolean.TRUE), required("b", LogicalType.BOOLEAN)));
        assertThrows(ValueConversionException.class, () -> normalize(SourceValue.of(1), required("b", LogicalType.BOOLEAN)));
    }

    @Test
    void aStringColumnKeepsTheLexicalTokenOfATypedValue() {
        BigInteger huge = new BigInteger("123456789012345678901234567890");
        assertEquals("123456789012345678901234567890",
                normalize(SourceValue.of(huge, "123456789012345678901234567890"), required("s", LogicalType.STRING)));
        assertEquals("1.50", normalize(SourceValue.of(1.5, "1.50"), required("s", LogicalType.STRING)));
        assertEquals("1.5", normalize(SourceValue.of(1.5), required("s", LogicalType.STRING)));
    }

    // ---- Whole records ----

    @Test
    void namedRecordsTakeMissingForAbsentFieldsAndRejectUnknownOnes() {
        TableSchema schema = TableSchema.of(required("id", LogicalType.INT32),
                builder("qty", LogicalType.INT32).required().defaultValue(1).build());
        Map<String, SourceValue> record = new LinkedHashMap<>();
        record.put("id", SourceValue.text("5"));
        assertArrayEquals(new Object[]{5, 1}, ValueNormalizer.normalizeRecord(schema, record, false));

        record.put("extra", SourceValue.text("x"));
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> ValueNormalizer.normalizeRecord(schema, record, false));
        assertEquals("extra", e.column());
        assertArrayEquals(new Object[]{5, 1}, ValueNormalizer.normalizeRecord(schema, record, true));
    }

    @Test
    void positionalRecordsNeedOneValuePerColumn() {
        TableSchema schema = TableSchema.of(required("a", LogicalType.INT32), nullable("b", LogicalType.STRING));
        assertArrayEquals(new Object[]{1, null},
                ValueNormalizer.normalizeRecord(schema, List.of(SourceValue.text("1"), SourceValue.NULL)));
        assertThrows(IllegalArgumentException.class,
                () -> ValueNormalizer.normalizeRecord(schema, List.of(SourceValue.text("1"))));
    }

    @Test
    void sourceValuesNeverPrintTheirContent() {
        assertFalse(SourceValue.text("secret").toString().contains("secret"));
        assertSame(SourceValue.NULL, SourceValue.text(null));
    }
}
