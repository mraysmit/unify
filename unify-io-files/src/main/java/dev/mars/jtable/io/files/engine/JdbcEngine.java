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
package dev.mars.jtable.io.files.engine;

import dev.mars.jtable.core.schema.ColumnDefinition;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.core.schema.ValueConversionException;
import dev.mars.jtable.core.schema.ValueNormalizer;
import dev.mars.jtable.io.common.engine.DatasetHandle;
import dev.mars.jtable.io.common.engine.ExecutionEngine;
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.StringJoiner;

/**
 * Shared behaviour of the JDBC-based execution engines: dataset creation, all-or-nothing loading,
 * streaming query results, and strict conversion of result values to the declared logical types.
 * Subclasses supply the column type mapping and the bulk loader.
 */
public abstract class JdbcEngine implements ExecutionEngine {
    private final String name;
    private final String version;
    protected final Connection connection;

    protected JdbcEngine(String name, Connection connection) throws SQLException {
        this.name = name;
        this.connection = connection;
        this.version = connection.getMetaData().getDatabaseProductVersion();
    }

    /** The engine column type for a logical type. */
    protected abstract String columnType(LogicalType type);

    /** Extra constraint for a column, for example a CHECK clause; empty by default. */
    protected String columnConstraint(ColumnDefinition column) {
        return "";
    }

    /** Text appended after the closing parenthesis of CREATE TABLE; empty by default. */
    protected String tableOptions() {
        return "";
    }

    /**
     * Appends every record of the source to the new, empty table, inside the transaction opened
     * by {@link #load}. Returns the number of records appended.
     */
    protected abstract long append(String dataset, RecordSource source) throws SQLException, IOException;

    /** Whether the engine represents BOOLEAN as the integers 0 and 1. */
    protected boolean numericBooleans() {
        return false;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String version() {
        return version;
    }

    @Override
    public DatasetHandle load(String dataset, RecordSource source) throws IOException {
        requireName(dataset);
        TableSchema schema = source.schema();
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(createTable(dataset, schema));
                }
                long rows = append(dataset, source);
                connection.commit();
                return new DatasetHandle(name, dataset, schema, rows);
            } catch (SQLException | IOException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    e.addSuppressed(rollbackFailure);
                }
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new IOException("Loading dataset '" + dataset + "' into " + name + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RecordSource query(String sql, TableSchema resultSchema, int batchSize) throws IOException {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be at least 1");
        }
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setFetchSize(batchSize);
            ResultSet results = statement.executeQuery(sql);
            checkResultShape(results.getMetaData(), resultSchema);
            return new ResultSource(statement, results, resultSchema, batchSize);
        } catch (SQLException | IOException e) {
            closeQuietly(statement, e);
            throw e instanceof IOException io ? io
                    : new IOException("Query failed in " + name + ": " + e.getMessage(), e);
        }
    }

    @Override
    public RecordSource scan(DatasetHandle dataset, int batchSize) throws IOException {
        StringJoiner columns = new StringJoiner(", ");
        for (ColumnDefinition column : dataset.schema().columns()) {
            columns.add(quote(column.name()));
        }
        return query("SELECT " + columns + " FROM " + quote(dataset.name()), dataset.schema(), batchSize);
    }

    @Override
    public void drop(String dataset) throws IOException {
        requireName(dataset);
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS " + quote(dataset));
        } catch (SQLException e) {
            throw new IOException("Dropping dataset '" + dataset + "' failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IOException("Closing " + name + " failed: " + e.getMessage(), e);
        }
    }

    /** Quotes an identifier so any name, including reserved words and quotes, is safe. */
    protected static String quote(String identifier) {
        return '"' + identifier.replace("\"", "\"\"") + '"';
    }

    private String createTable(String dataset, TableSchema schema) {
        StringJoiner columns = new StringJoiner(", ", "CREATE TABLE " + quote(dataset) + " (", ")");
        for (ColumnDefinition column : schema.columns()) {
            String constraint = columnConstraint(column);
            columns.add(quote(column.name()) + " " + columnType(column.type())
                    + (column.nullable() ? "" : " NOT NULL")
                    + (constraint.isEmpty() ? "" : " " + constraint));
        }
        return columns + tableOptions();
    }

    private static void requireName(String dataset) {
        if (dataset == null || dataset.isBlank()) {
            throw new IllegalArgumentException("Dataset name cannot be null or blank");
        }
    }

    private static void checkResultShape(ResultSetMetaData metadata, TableSchema schema)
            throws SQLException, IOException {
        if (metadata.getColumnCount() != schema.size()) {
            throw new IOException("The query returns " + metadata.getColumnCount()
                    + " columns but the declared result schema has " + schema.size());
        }
        for (int i = 0; i < schema.size(); i++) {
            String label = metadata.getColumnLabel(i + 1);
            if (!schema.column(i).name().equals(label)) {
                throw new IOException("Result column " + (i + 1) + " is labelled '" + label
                        + "' but the declared result schema names it '" + schema.column(i).name() + "'");
            }
        }
    }

    /**
     * Converts one raw JDBC value to the declared logical type, strictly: integers must fit,
     * text is never coerced from numbers, and booleans are numeric only where the engine stores
     * them that way. Temporal text is parsed with the shared ISO rules.
     */
    protected Object toLogical(Object raw, ColumnDefinition column) {
        if (raw == null) {
            return null;
        }
        String name = column.name();
        switch (column.type()) {
            case STRING:
                if (raw instanceof String) {
                    return raw;
                }
                break;
            case INT32:
                if (raw instanceof Integer) {
                    return raw;
                }
                if (raw instanceof Long || raw instanceof Short || raw instanceof Byte || raw instanceof BigInteger) {
                    long value;
                    try {
                        value = raw instanceof BigInteger big ? big.longValueExact() : ((Number) raw).longValue();
                    } catch (ArithmeticException e) {
                        throw new ValueConversionException(name, "whole number outside the INT32 range");
                    }
                    if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
                        throw new ValueConversionException(name, "whole number outside the INT32 range");
                    }
                    return (int) value;
                }
                break;
            case DOUBLE:
                if (raw instanceof Double || raw instanceof Float || raw instanceof Integer
                        || raw instanceof Long || raw instanceof BigDecimal || raw instanceof Short) {
                    double value = ((Number) raw).doubleValue();
                    if (!Double.isFinite(value)) {
                        throw new ValueConversionException(name, "decimal number outside the finite DOUBLE range");
                    }
                    return value;
                }
                break;
            case BOOLEAN:
                if (raw instanceof Boolean) {
                    return raw;
                }
                if (numericBooleans() && (raw instanceof Integer || raw instanceof Long)) {
                    long value = ((Number) raw).longValue();
                    if (value == 0 || value == 1) {
                        return value == 1;
                    }
                    throw new ValueConversionException(name, "stored boolean is neither 0 nor 1");
                }
                break;
            case DATE:
                if (raw instanceof LocalDate) {
                    return raw;
                }
                if (raw instanceof java.sql.Date date) {
                    return date.toLocalDate();
                }
                if (raw instanceof String text) {
                    return ValueNormalizer.fromText(text, column);
                }
                break;
            case TIME:
                if (raw instanceof LocalTime) {
                    return raw;
                }
                if (raw instanceof java.sql.Time time) {
                    return time.toLocalTime();
                }
                if (raw instanceof String text) {
                    return ValueNormalizer.fromText(text, column);
                }
                break;
            case LOCAL_DATETIME:
                if (raw instanceof LocalDateTime) {
                    return raw;
                }
                if (raw instanceof java.sql.Timestamp timestamp) {
                    return timestamp.toLocalDateTime();
                }
                if (raw instanceof String text) {
                    return ValueNormalizer.fromText(text, column);
                }
                break;
            default:
                throw new IllegalStateException("Unhandled logical type " + column.type());
        }
        throw new ValueConversionException(name, "the engine returned a "
                + raw.getClass().getSimpleName() + " where " + column.type() + " was declared");
    }

    private static void closeQuietly(AutoCloseable resource, Exception primary) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception e) {
            primary.addSuppressed(e);
        }
    }

    /** Streams a JDBC result set as checked record batches. */
    private final class ResultSource implements RecordSource {
        private final Statement statement;
        private final ResultSet results;
        private final TableSchema schema;
        private final int batchSize;
        private long recordNumber = 0;
        private boolean exhausted = false;

        ResultSource(Statement statement, ResultSet results, TableSchema schema, int batchSize) {
            this.statement = statement;
            this.results = results;
            this.schema = schema;
            this.batchSize = batchSize;
        }

        @Override
        public TableSchema schema() {
            return schema;
        }

        @Override
        public RecordBatch nextBatch() throws IOException {
            if (exhausted) {
                return null;
            }
            RecordBatch.Builder builder = RecordBatch.builder(schema, batchSize);
            try {
                while (builder.rowCount() < batchSize && results.next()) {
                    recordNumber++;
                    Object[] values = new Object[schema.size()];
                    for (int i = 0; i < values.length; i++) {
                        values[i] = toLogical(results.getObject(i + 1), schema.column(i));
                    }
                    builder.add(values);
                }
            } catch (SQLException e) {
                throw new IOException("Reading result record " + (recordNumber + 1) + " failed: " + e.getMessage(), e);
            } catch (ValueConversionException e) {
                throw new IOException("Result record " + recordNumber + ": " + e.getMessage(), e);
            }
            if (builder.rowCount() == 0) {
                exhausted = true;
                return null;
            }
            return builder.build();
        }

        @Override
        public void close() throws IOException {
            try {
                results.close();
                statement.close();
            } catch (SQLException e) {
                throw new IOException("Closing the query result failed: " + e.getMessage(), e);
            }
        }
    }
}
