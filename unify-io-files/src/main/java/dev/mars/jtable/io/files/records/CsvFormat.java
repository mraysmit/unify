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

/**
 * Immutable CSV settings shared by {@link CsvRecordSource} and {@link CsvRecordSink}.
 *
 * <p>Null is written as the unquoted null token (default {@code \N}); a quoted field is always a
 * literal value, so {@code "\N"} in quotes is the two-character text. An unquoted empty field is
 * the empty string unless {@code emptyAsNull} is set. The null token cannot contain the delimiter,
 * the quote character or a line break.</p>
 */
public final class CsvFormat {
    public static final CsvFormat DEFAULT = new CsvFormat(',', '"', true, "\\N", false, 1024, 1_000_000, "\n");

    private final char delimiter;
    private final char quote;
    private final boolean header;
    private final String nullToken;
    private final boolean emptyAsNull;
    private final int batchSize;
    private final int maxFieldChars;
    private final String lineSeparator;

    private CsvFormat(char delimiter, char quote, boolean header, String nullToken, boolean emptyAsNull,
                      int batchSize, int maxFieldChars, String lineSeparator) {
        if (delimiter == quote) {
            throw new IllegalArgumentException("The delimiter and quote character must differ");
        }
        if (isLineBreak(delimiter) || isLineBreak(quote)) {
            throw new IllegalArgumentException("The delimiter and quote character cannot be line breaks");
        }
        if (nullToken == null || nullToken.indexOf(delimiter) >= 0 || nullToken.indexOf(quote) >= 0
                || nullToken.indexOf('\n') >= 0 || nullToken.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "The null token cannot be null or contain the delimiter, the quote character or a line break");
        }
        if (batchSize < 1 || maxFieldChars < 1) {
            throw new IllegalArgumentException("batchSize and maxFieldChars must be at least 1");
        }
        if (!lineSeparator.equals("\n") && !lineSeparator.equals("\r\n")) {
            throw new IllegalArgumentException("The line separator must be \\n or \\r\\n");
        }
        this.delimiter = delimiter;
        this.quote = quote;
        this.header = header;
        this.nullToken = nullToken;
        this.emptyAsNull = emptyAsNull;
        this.batchSize = batchSize;
        this.maxFieldChars = maxFieldChars;
        this.lineSeparator = lineSeparator;
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    public char delimiter() { return delimiter; }
    public char quote() { return quote; }
    public boolean header() { return header; }
    public String nullToken() { return nullToken; }
    public boolean emptyAsNull() { return emptyAsNull; }
    public int batchSize() { return batchSize; }
    public int maxFieldChars() { return maxFieldChars; }
    public String lineSeparator() { return lineSeparator; }

    public CsvFormat withDelimiter(char value) {
        return new CsvFormat(value, quote, header, nullToken, emptyAsNull, batchSize, maxFieldChars, lineSeparator);
    }

    public CsvFormat withQuote(char value) {
        return new CsvFormat(delimiter, value, header, nullToken, emptyAsNull, batchSize, maxFieldChars, lineSeparator);
    }

    public CsvFormat withHeader(boolean value) {
        return new CsvFormat(delimiter, quote, value, nullToken, emptyAsNull, batchSize, maxFieldChars, lineSeparator);
    }

    public CsvFormat withNullToken(String value) {
        return new CsvFormat(delimiter, quote, header, value, emptyAsNull, batchSize, maxFieldChars, lineSeparator);
    }

    public CsvFormat withEmptyAsNull(boolean value) {
        return new CsvFormat(delimiter, quote, header, nullToken, value, batchSize, maxFieldChars, lineSeparator);
    }

    public CsvFormat withBatchSize(int value) {
        return new CsvFormat(delimiter, quote, header, nullToken, emptyAsNull, value, maxFieldChars, lineSeparator);
    }

    public CsvFormat withMaxFieldChars(int value) {
        return new CsvFormat(delimiter, quote, header, nullToken, emptyAsNull, batchSize, value, lineSeparator);
    }

    public CsvFormat withLineSeparator(String value) {
        return new CsvFormat(delimiter, quote, header, nullToken, emptyAsNull, batchSize, maxFieldChars, value);
    }
}
