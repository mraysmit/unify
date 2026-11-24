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
import java.util.List;

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

    private final TableSchema schema;
    private final CsvFormat format;
    private final Reader reader;
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
                String text = fields.get(i);
                boolean quoted = quotedFlags.get(i);
                if (!quoted && (text.equals(format.nullToken()) || (format.emptyAsNull() && text.isEmpty()))) {
                    text = null;
                }
                values[i] = ValueNormalizer.fromText(text, schema.column(i));
            }
        } catch (ValueConversionException e) {
            throw new IOException("Line " + recordLine + ": " + e.getMessage(), e);
        }
        return values;
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
