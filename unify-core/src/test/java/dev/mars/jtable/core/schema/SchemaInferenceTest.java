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
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** FR002 inference and AC04: a bounded sample, combination rules, freezing and replay. */
class SchemaInferenceTest {

    private static Map<String, SourceValue> record(Object... nameValuePairs) {
        Map<String, SourceValue> record = new LinkedHashMap<>();
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            Object value = nameValuePairs[i + 1];
            record.put((String) nameValuePairs[i], value instanceof SourceValue sv ? sv : SourceValue.text((String) value));
        }
        return record;
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "42|INT32", "-7|INT32", "+3|INT32", "0|INT32", "2147483647|INT32",
            "2147483648|STRING", "99999999999|STRING", "007|STRING", "-01|STRING",
            "1.5|DOUBLE", "1e3|DOUBLE", ".5|DOUBLE", "1e999|STRING",
            "true|BOOLEAN", "FALSE|BOOLEAN", "yes|STRING",
            "2026-11-25|DATE", "09:30|TIME", "09:30:05.123|TIME", "2026-11-25T09:30|LOCAL_DATETIME",
            "2026-02-30|STRING", "25/11/2026|STRING", "' 5'|STRING", "abc|STRING", "NaN|STRING"
    })
    void textClassifiesStrictly(String token, LogicalType expected) {
        assertEquals(expected, SchemaInference.classify(new SourceValue.Scalar(token, token)));
    }

    @Test
    void typedScalarsClassifyByValue() {
        assertEquals(LogicalType.INT32, SchemaInference.classify(new SourceValue.Scalar(5L, "5")));
        assertEquals(LogicalType.STRING, SchemaInference.classify(new SourceValue.Scalar(5_000_000_000L, "5000000000")));
        assertEquals(LogicalType.STRING, SchemaInference.classify(
                new SourceValue.Scalar(new BigInteger("123456789012345678901"), "123456789012345678901")));
        assertEquals(LogicalType.DOUBLE, SchemaInference.classify(new SourceValue.Scalar(new BigDecimal("1.0"), "1.0")));
        assertEquals(LogicalType.INT32, SchemaInference.classify(new SourceValue.Scalar(new BigDecimal("12"), "12")));
        assertEquals(LogicalType.BOOLEAN, SchemaInference.classify(new SourceValue.Scalar(true, "true")));
    }

    @Test
    void combinationRules() {
        assertEquals(LogicalType.INT32, SchemaInference.combine(LogicalType.INT32, LogicalType.INT32));
        assertEquals(LogicalType.DOUBLE, SchemaInference.combine(LogicalType.INT32, LogicalType.DOUBLE));
        assertEquals(LogicalType.DOUBLE, SchemaInference.combine(LogicalType.DOUBLE, LogicalType.INT32));
        assertEquals(LogicalType.STRING, SchemaInference.combine(LogicalType.INT32, LogicalType.BOOLEAN));
        assertEquals(LogicalType.STRING, SchemaInference.combine(LogicalType.DATE, LogicalType.LOCAL_DATETIME));
        assertEquals(LogicalType.STRING, SchemaInference.combine(LogicalType.STRING, LogicalType.INT32));
        assertEquals(LogicalType.DATE, SchemaInference.combine(null, LogicalType.DATE));
    }

    /** AC04 end to end. */
    @Test
    void infersFromABoundedSampleFreezesAndRejectsALaterIncompatibleValue() {
        SchemaInference inference = new SchemaInference(3);
        inference.add(record("id", "001", "amount", "10", "flag", SourceValue.NULL, "qty", "1"));
        inference.add(record("id", "002", "amount", "2.5", "flag", SourceValue.NULL, "qty", "2"));
        inference.add(record("id", "003", "amount", "7", "flag", SourceValue.NULL, "qty", "3", "late", "x"));
        assertTrue(inference.isFull());
        assertThrows(IllegalStateException.class, () -> inference.add(record("id", "004")));

        TableSchema schema = inference.freeze();
        assertEquals(TableSchema.of(
                ColumnDefinition.required("id", LogicalType.STRING),       // leading-zero identifiers
                ColumnDefinition.required("amount", LogicalType.DOUBLE),   // mixed integer and fractional
                ColumnDefinition.nullable("flag", LogicalType.STRING),     // null only
                ColumnDefinition.required("qty", LogicalType.INT32),
                ColumnDefinition.nullable("late", LogicalType.STRING)      // appears only later, so missing earlier
        ), schema);

        // Replay the sample through normalization: no second read.
        List<Map<String, SourceValue>> sample = inference.sample();
        assertEquals(3, sample.size());
        assertArrayEquals(new Object[]{"001", 10.0, null, 1, null},
                ValueNormalizer.normalizeRecord(schema, sample.get(0), false));
        assertArrayEquals(new Object[]{"003", 7.0, null, 3, "x"},
                ValueNormalizer.normalizeRecord(schema, sample.get(2), false));

        // After freezing, a later incompatible value is a located error, not a schema change.
        ValueConversionException e = assertThrows(ValueConversionException.class,
                () -> ValueNormalizer.normalizeRecord(schema, record("id", "005", "amount", "x", "qty", "1"), false));
        assertEquals("amount", e.column());
        // A later new field is an error unless an explicit projection excludes it.
        assertThrows(ValueConversionException.class,
                () -> ValueNormalizer.normalizeRecord(schema, record("id", "6", "amount", "1", "qty", "1", "new", "y"), false));
        assertThrows(IllegalStateException.class, () -> inference.add(record("id", "7")));
        assertSame(schema, inference.freeze());
    }

    @Test
    void aFieldMissingFromSomeRecordsIsNullable() {
        SchemaInference inference = SchemaInference.withDefaultSample();
        inference.add(record("a", "1", "b", "2"));
        inference.add(record("a", "3"));
        TableSchema schema = inference.freeze();
        assertFalse(schema.column("a").nullable());
        assertTrue(schema.column("b").nullable());
    }

    @Test
    void typesCombineAcrossTheSample() {
        SchemaInference inference = SchemaInference.withDefaultSample();
        inference.add(record("x", "1", "y", "true", "z", "2026-11-25"));
        inference.add(record("x", "abc", "y", "1", "z", "2026-11-25T10:00"));
        TableSchema schema = inference.freeze();
        assertEquals(LogicalType.STRING, schema.column("x").type());
        assertEquals(LogicalType.STRING, schema.column("y").type());
        assertEquals(LogicalType.STRING, schema.column("z").type());
    }

    @Test
    void stringFallbackKeepsTheOriginalToken() {
        SchemaInference inference = SchemaInference.withDefaultSample();
        inference.add(record("v", SourceValue.of(new BigInteger("123456789012345678901"), "123456789012345678901")));
        inference.add(record("v", SourceValue.of(5L, "5")));
        TableSchema schema = inference.freeze();
        assertEquals(LogicalType.STRING, schema.column("v").type());
        assertEquals("123456789012345678901",
                ValueNormalizer.normalizeRecord(schema, inference.sample().get(0), false)[0]);
    }

    @Test
    void declaredHeaderAloneGivesNullableStrings() {
        SchemaInference inference = SchemaInference.withDefaultSample();
        inference.declareField("first");
        inference.declareField("second");
        assertEquals(TableSchema.of(ColumnDefinition.nullable("first", LogicalType.STRING),
                ColumnDefinition.nullable("second", LogicalType.STRING)), inference.freeze());
    }

    @Test
    void declaredFieldsKeepTheirPositionAndRejectDuplicates() {
        SchemaInference inference = SchemaInference.withDefaultSample();
        inference.declareField("b");
        inference.declareField("a");
        assertThrows(IllegalArgumentException.class, () -> inference.declareField("a"));
        inference.add(record("a", "1", "b", "x"));
        assertThrows(IllegalStateException.class, () -> inference.declareField("c"));
        TableSchema schema = inference.freeze();
        assertEquals("b", schema.column(0).name());
        assertEquals(LogicalType.INT32, schema.column("a").type());
    }

    @Test
    void anEmptySourceNeedsAnExplicitSchema() {
        assertThrows(IllegalStateException.class, () -> SchemaInference.withDefaultSample().freeze());
        assertThrows(IllegalArgumentException.class, () -> new SchemaInference(0));
    }
}
