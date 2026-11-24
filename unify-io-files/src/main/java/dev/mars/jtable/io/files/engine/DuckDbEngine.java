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

import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.stream.RecordSource;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;

/**
 * DuckDB execution engine. Records are loaded through DuckDB's native appender, and query results
 * are streamed from DuckDB chunk by chunk rather than materialised first. With a database file,
 * DuckDB can spill intermediate work to disk, so datasets larger than memory are supported.
 */
public final class DuckDbEngine extends JdbcEngine {

    private DuckDbEngine(Connection connection) throws SQLException {
        super("duckdb", connection);
    }

    /** An engine whose data lives only in memory (native memory, not the Java heap). */
    public static DuckDbEngine inMemory() throws IOException {
        return connect("jdbc:duckdb:");
    }

    /**
     * An engine backed by a database file, which lets DuckDB spill to disk. The file is created
     * if it does not exist; the caller owns it and removes it when it is no longer needed.
     */
    public static DuckDbEngine open(Path databaseFile) throws IOException {
        return connect("jdbc:duckdb:" + databaseFile.toAbsolutePath());
    }

    private static DuckDbEngine connect(String url) throws IOException {
        Properties properties = new Properties();
        // Stream query results instead of materialising the whole result before the first row.
        properties.setProperty("jdbc_stream_results", "true");
        try {
            return new DuckDbEngine(DriverManager.getConnection(url, properties));
        } catch (SQLException e) {
            throw new IOException("Opening DuckDB failed: " + e.getMessage(), e);
        }
    }

    @Override
    protected String columnType(LogicalType type) {
        switch (type) {
            case STRING:
                return "VARCHAR";
            case INT32:
                return "INTEGER";
            case DOUBLE:
                return "DOUBLE";
            case BOOLEAN:
                return "BOOLEAN";
            case DATE:
                return "DATE";
            case TIME:
                return "TIME";
            case LOCAL_DATETIME:
                return "TIMESTAMP";
            default:
                throw new IllegalStateException("Unhandled logical type " + type);
        }
    }

    @Override
    protected long append(String table, List<String> columns, RecordSource source) throws SQLException, IOException {
        TableSchema schema = source.schema();
        long rows = 0;
        // The appender fills columns in table order: the generated columns, then the ordinal.
        try (DuckDBAppender appender = connection.unwrap(DuckDBConnection.class).createAppender("main", table)) {
            for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
                for (int r = 0; r < batch.rowCount(); r++) {
                    appender.beginRow();
                    for (int c = 0; c < schema.size(); c++) {
                        appendValue(appender, batch.value(r, c));
                    }
                    appender.append(rows + r + 1);
                    appender.endRow();
                }
                rows += batch.rowCount();
            }
        }
        return rows;
    }

    private static void appendValue(DuckDBAppender appender, Object value) throws SQLException {
        // Values are already checked against the schema, so each has its column's canonical type.
        if (value == null) {
            appender.appendNull();
        } else if (value instanceof String text) {
            appender.append(text);
        } else if (value instanceof Integer number) {
            appender.append(number.intValue());
        } else if (value instanceof Double number) {
            appender.append(number.doubleValue());
        } else if (value instanceof Boolean flag) {
            appender.append(flag.booleanValue());
        } else if (value instanceof LocalDate date) {
            appender.append(date);
        } else if (value instanceof LocalTime time) {
            appender.append(time);
        } else if (value instanceof LocalDateTime dateTime) {
            appender.append(dateTime);
        } else {
            throw new IllegalStateException("Unexpected value type " + value.getClass().getName());
        }
    }
}
