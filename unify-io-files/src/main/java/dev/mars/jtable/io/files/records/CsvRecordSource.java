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
package dev.mars.jtable.io.files.records;

import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.SchemaInference;
import dev.mars.jtable.core.schema.SourceValue;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.schema.ValueConversionException;
import dev.mars.jtable.core.schema.ValueNormalizer;
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streams a CSV file as checked record batches against an explicit schema, holding at most one
 * batch in memory regardless of file size.
 *
 * <p>Parsing follows RFC 4180: quoted fields may contain the delimiter, doubled quotes and line
 * breaks; records end with LF, CRLF or CR. Every record must have exactly one field per schema
 * column. With a header, the header must name the schema's columns in order. Errors give the
 * line on which the failing record starts and never repeat field contents.</p>
 */
public final class CsvRecordSource implements RecordSource {
    private static final int BUFFER = 1 << 16;

    private TableSchema schema;
    private final CsvFormat format;
    private final Reader reader;
    private List<Map<String, SourceValue>> replay = List.of();
    private List<Long> replayLines = List.of();
    private int replayed = 0;
    private final char[] buffer = new char[BUFFER];
    private int position = 0;
    private int limit = 0;
    private long line = 1;
    private long recordLine = 1;
    private boolean finished = false;

    private final List<String> fields = new ArrayList<>();
    private final List<Boolean> quotedFlags = new ArrayList<>();
    private final StringBuilder field = new StringBuilder();

    /** Opens the file (UTF-8) and, when the format has a header, reads and checks it. */
    public CsvRecordSource(Path file, TableSchema schema, CsvFormat format) throws IOException {
        this.schema = schema;
        this.format = format;
        this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        try {
            skipByteOrderMark();
            if (format.header()) {
                checkHeader();
            }
        } catch (IOException | RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    private CsvRecordSource(Path file, CsvFormat format, int sampleSize) throws IOException {
        this.format = format;
        this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        try {
            skipByteOrderMark();
            inferSchema(new SchemaInference(sampleSize));
        } catch (IOException | RuntimeException e) {
            reader.close();
            throw e;
        }
    }

    /**
     * Opens the file and infers its schema from the first {@code sampleSize} records (FR002). The
     * header names the columns and fixes their order; a header with no records gives nullable
     * STRING columns. The sampled records are kept and delivered first, so nothing is read twice.
     * After the sample, a value that does not fit the frozen schema fails its batch with a
     * located error. A headerless file needs an explicit positional schema.
     */
    public static CsvRecordSource inferring(Path file, CsvFormat format, int sampleSize) throws IOException {
        if (!format.header()) {
            throw new IllegalArgumentException("Inference needs a header; a headerless file needs an explicit schema");
        }
        return new CsvRecordSource(file, format, sampleSize);
    }

    /** {@link #inferring(Path, CsvFormat, int)} with the default sample of 1,000 records. */
    public static CsvRecordSource inferring(Path file, CsvFormat format) throws IOException {
        return inferring(file, format, SchemaInference.DEFAULT_SAMPLE_SIZE);
    }

    @Override
    public TableSchema schema() {
        return schema;
    }

    @Override
    public RecordBatch nextBatch() throws IOException {
        if (finished) {
            return null;
        }
        RecordBatch.Builder builder = RecordBatch.builder(schema, format.batchSize());
        while (replayed < replay.size() && builder.rowCount() < format.batchSize()) {
            try {
                builder.add(ValueNormalizer.normalizeRecord(schema, replay.get(replayed), false));
            } catch (ValueConversionException e) {
                throw new IOException("Line " + replayLines.get(replayed) + ": " + e.getMessage(), e);
            }
            replayed++;
        }
        if (replayed == replay.size() && !replay.isEmpty()) {
            replay = List.of(); // release the sample once delivered
            replayLines = List.of();
            replayed = 0;
        }
        while (builder.rowCount() < format.batchSize()) {
            if (!readRecord()) {
                finished = true;
                break;
            }
            builder.add(convert());
        }
        return builder.rowCount() == 0 ? null : builder.build();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private void checkHeader() throws IOException {
        if (!readRecord()) {
            throw new IOException("The file is empty but a header was expected");
        }
        boolean matches = fields.size() == schema.size();
        for (int i = 0; matches && i < fields.size(); i++) {
            matches = fields.get(i).equals(schema.column(i).name());
        }
        if (!matches) {
            List<String> expected = new ArrayList<>();
            schema.columns().forEach(column -> expected.add(column.name()));
            throw new IOException("Line " + recordLine + ": the header does not name the schema columns "
                    + expected + " in order");
        }
    }

    private Object[] convert() throws IOException {
        if (fields.size() != schema.size()) {
            throw new IOException("Line " + recordLine + ": expected " + schema.size()
                    + " fields but found " + fields.size());
        }
        Object[] values = new Object[fields.size()];
        try {
            for (int i = 0; i < values.length; i++) {
                values[i] = ValueNormalizer.normalize(sourceValue(i), schema.column(i));
            }
        } catch (ValueConversionException e) {
            throw new IOException("Line " + recordLine + ": " + e.getMessage(), e);
        }
        return values;
    }

    /**
     * The source value of field {@code i} of the current record. An unquoted null token is an
     * explicit null, and so is an unquoted empty field when the format enables empty-as-null; a
     * quoted field is always literal text. CSV records have a fixed width, so a field is never
     * MISSING.
     */
    private SourceValue sourceValue(int i) {
        String text = fields.get(i);
        if (!quotedFlags.get(i) && (text.equals(format.nullToken()) || (format.emptyAsNull() && text.isEmpty()))) {
            return SourceValue.NULL;
        }
        return SourceValue.text(text);
    }

    private void inferSchema(SchemaInference inference) throws IOException {
        if (!readRecord()) {
            throw new IOException("The file is empty: an explicit schema is required");
        }
        List<String> names = new ArrayList<>(fields);
        for (String name : names) {
            try {
                inference.declareField(name);
            } catch (IllegalArgumentException e) {
                throw new IOException("Line " + recordLine + ": the header has a blank or duplicate column name", e);
            }
        }
        List<Long> lines = new ArrayList<>();
        while (!inference.isFull() && readRecord()) {
            if (fields.size() != names.size()) {
                throw new IOException("Line " + recordLine + ": expected " + names.size()
                        + " fields but found " + fields.size());
            }
            Map<String, SourceValue> sampled = new LinkedHashMap<>();
            for (int i = 0; i < names.size(); i++) {
                sampled.put(names.get(i), sourceValue(i));
            }
            inference.add(sampled);
            lines.add(recordLine);
        }
        schema = inference.freeze();
        replay = inference.sample();
        replayLines = lines;
    }

    /** Reads the next record into {@link #fields}; returns false at the end of the input. */
    private boolean readRecord() throws IOException {
        fields.clear();
        quotedFlags.clear();
        field.setLength(0);
        recordLine = line;
        boolean inQuotes = false;
        boolean wasQuoted = false;
        boolean afterClosingQuote = false;
        boolean started = false;
        char quote = format.quote();
        char delimiter = format.delimiter();
        while (true) {
            int c = next();
            if (c < 0) {
                if (inQuotes) {
                    throw new IOException("Line " + recordLine + ": a quoted field is not closed before the end of the file");
                }
                if (!started) {
                    return false;
                }
                endField(wasQuoted);
                return true;
            }
            started = true;
            if (inQuotes) {
                if (c == quote) {
                    if (peek() == quote) {
                        next();
                        append((char) c);
                    } else {
                        inQuotes = false;
                        afterClosingQuote = true;
                    }
                } else {
                    if (c == '\n' || (c == '\r' && peek() != '\n')) {
                        line++;
                    }
                    append((char) c);
                }
            } else if (c == delimiter) {
                endField(wasQuoted);
                wasQuoted = false;
                afterClosingQuote = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && peek() == '\n') {
                    next();
                }
                line++;
                endField(wasQuoted);
                return true;
            } else if (afterClosingQuote) {
                throw new IOException("Line " + recordLine + ": unexpected character after a closing quote");
            } else if (c == quote) {
                if (field.length() > 0) {
                    throw new IOException("Line " + recordLine + ": a quote appears inside an unquoted field");
                }
                inQuotes = true;
                wasQuoted = true;
            } else {
                append((char) c);
            }
        }
    }

    private void append(char c) throws IOException {
        if (field.length() >= format.maxFieldChars()) {
            throw new IOException("Line " + recordLine + ": a field is longer than " + format.maxFieldChars() + " characters");
        }
        field.append(c);
    }

    private void endField(boolean quoted) {
        fields.add(field.toString());
        quotedFlags.add(quoted);
        field.setLength(0);
    }

    private void skipByteOrderMark() throws IOException {
        if (peek() == '﻿') {
            next();
        }
    }

    private int next() throws IOException {
        if (position == limit && !fill()) {
            return -1;
        }
        return buffer[position++];
    }

    private int peek() throws IOException {
        if (position == limit && !fill()) {
            return -1;
        }
        return buffer[position];
    }

    private boolean fill() throws IOException {
        int read = reader.read(buffer, 0, buffer.length);
        if (read <= 0) {
            return false;
        }
        position = 0;
        limit = read;
        return true;
    }
}
