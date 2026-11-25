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
package dev.mars.jtable.engine.sqlite;

import dev.mars.jtable.core.schema.ColumnDefinition;
import dev.mars.jtable.core.schema.LogicalType;
import dev.mars.jtable.core.schema.RecordBatch;
import dev.mars.jtable.core.schema.TableSchema;
import dev.mars.jtable.io.common.engine.JdbcEngine;
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SQLite execution engine. Datasets are STRICT tables, so SQLite itself rejects a value of the
 * wrong storage class. Records are loaded with one prepared, typed INSERT executed in batches
 * inside the single load transaction.
 *
 * <p>SQLite has no boolean or temporal storage classes. BOOLEAN is stored as INTEGER 0/1 with a
 * CHECK constraint. DATE, TIME and LOCAL_DATETIME are stored as fixed-width ISO-8601 text
 * ({@code yyyy-MM-dd}, {@code HH:mm:ss.nnnnnnnnn}, {@code yyyy-MM-ddTHH:mm:ss.nnnnnnnnn}), so
 * equal values always have equal text and text order is time order for years 0000 to 9999.
 * The dataset's Unify schema restores the logical types when results are read back.</p>
 */
public final class SqliteEngine extends JdbcEngine {
    /**
     * The oldest supported SQLite: RIGHT and FULL OUTER JOIN arrived in 3.39 (STRICT tables in
     * 3.37). Opening an older library fails.
     */
    public static final String MINIMUM_VERSION = "3.39.0";

    /**
     * The default memory cap of an in-memory engine: 256 MiB, four times the default 64 MiB
     * materialised-payload limit, to allow for page and index overhead. Hosts derive their own
     * from the operation's limits.
     */
    public static final long DEFAULT_MEMORY_CAP = 256L * 1024 * 1024;

    private static final int SQLITE_FULL = 13;
    private static final int STATEMENT_BATCH = 1000;
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter DATE_TIME_TEXT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSSSS");

    private final long memoryCap;

    private SqliteEngine(Connection connection, long memoryCap) throws SQLException {
        super("sqlite", connection);
        this.memoryCap = memoryCap;
    }

    /** An engine whose data lives only in memory, capped at {@link #DEFAULT_MEMORY_CAP}. */
    public static SqliteEngine inMemory() throws IOException {
        return inMemory(DEFAULT_MEMORY_CAP);
    }

    /**
     * An engine whose data lives only in memory, capped at {@code memoryCapBytes}. An in-memory
     * SQLite database otherwise grows until the process runs out of memory. The cap limits the
     * database's pages ({@code PRAGMA max_page_count}) on this connection only;
     * {@code PRAGMA hard_heap_limit} is not used because it is process-wide and would throttle
     * every other SQLite connection in the JVM. A load or query that reaches the cap fails with a
     * message saying so; such an operation needs a database file or the DuckDB engine.
     */
    public static SqliteEngine inMemory(long memoryCapBytes) throws IOException {
        if (memoryCapBytes < 1) {
            throw new IllegalArgumentException("The memory cap must be positive");
        }
        return connect("jdbc:sqlite::memory:", memoryCapBytes);
    }

    /**
     * An engine backed by a database file, created if it does not exist; the caller owns it. The
     * file grows on disk, so no memory cap applies.
     */
    public static SqliteEngine open(Path databaseFile) throws IOException {
        return connect("jdbc:sqlite:" + databaseFile.toAbsolutePath(), 0);
    }

    /** The memory cap in bytes, or 0 for a file-backed engine. */
    public long memoryCap() {
        return memoryCap;
    }

    /**
     * Whether a SQLite library version meets {@link #MINIMUM_VERSION}. Versions compare by their
     * numeric parts, so 3.100.0 is newer than 3.39.0.
     */
    public static boolean isSupported(String version) {
        int[] actual = parts(version);
        int[] minimum = parts(MINIMUM_VERSION);
        for (int i = 0; i < minimum.length; i++) {
            int part = i < actual.length ? actual[i] : 0;
            if (part != minimum[i]) {
                return part > minimum[i];
            }
        }
        return true;
    }

    private static int[] parts(String version) {
        if (version == null || !version.matches("\\d++(\\.\\d++)*+")) {
            throw new IllegalArgumentException("Not a SQLite version: " + version);
        }
        String[] text = version.split("\\.");
        int[] parts = new int[text.length];
        for (int i = 0; i < text.length; i++) {
            parts[i] = Integer.parseInt(text[i]);
        }
        return parts;
    }

    private static SqliteEngine connect(String url, long memoryCap) throws IOException {
        Connection connection;
        try {
            connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            throw new IOException("Opening SQLite failed: " + e.getMessage(), e);
        }
        try {
            String version = libraryVersion(connection);
            if (!isSupported(version)) {
                throw new IOException("SQLite " + version + " is too old: Unify needs " + MINIMUM_VERSION + " or later");
            }
            if (memoryCap > 0) {
                applyMemoryCap(connection, memoryCap);
            }
            return new SqliteEngine(connection, memoryCap);
        } catch (SQLException e) {
            closeAfterFailure(connection, e);
            throw new IOException("Opening SQLite failed: " + e.getMessage(), e);
        } catch (IOException | RuntimeException e) {
            closeAfterFailure(connection, e);
            throw e;
        }
    }

    private static void closeAfterFailure(Connection connection, Exception primary) {
        try {
            connection.close();
        } catch (SQLException closeFailure) {
            primary.addSuppressed(closeFailure);
        }
    }

    private static String libraryVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT sqlite_version()")) {
            result.next();
            return result.getString(1);
        }
    }

    private static void applyMemoryCap(Connection connection, long memoryCap) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            long pageSize;
            try (ResultSet result = statement.executeQuery("PRAGMA page_size")) {
                result.next();
                pageSize = result.getLong(1);
            }
            long pages = Math.max(1, memoryCap / pageSize);
            try (ResultSet result = statement.executeQuery("PRAGMA max_page_count = " + pages)) {
                result.next();
                if (result.getLong(1) != pages) {
                    throw new SQLException("SQLite did not accept the memory cap");
                }
            }
        }
    }

    @Override
    protected String explain(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (memoryCap > 0 && (e.getErrorCode() == SQLITE_FULL || message.contains("SQLITE_FULL"))) {
            return "the working data reached the SQLite memory cap of " + memoryCap
                    + " bytes; use a database file or the DuckDB engine for an operation this large";
        }
        return message;
    }

    @Override
    protected String columnType(LogicalType type) {
        switch (type) {
            case STRING:
            case DATE:
            case TIME:
            case LOCAL_DATETIME:
                return "TEXT";
            case INT32:
            case BOOLEAN:
                return "INTEGER";
            case DOUBLE:
                return "REAL";
            default:
                throw new IllegalStateException("Unhandled logical type " + type);
        }
    }

    @Override
    protected String ordinalType() {
        // STRICT tables accept only INT, INTEGER, REAL, TEXT, BLOB and ANY; INTEGER is 64-bit.
        return "INTEGER";
    }

    @Override
    protected String columnConstraint(ColumnDefinition column, String physicalName) {
        return column.type() == LogicalType.BOOLEAN ? "CHECK (" + physicalName + " IN (0, 1))" : "";
    }

    @Override
    protected String tableOptions() {
        return " STRICT";
    }

    @Override
    protected boolean numericBooleans() {
        return true;
    }

    @Override
    protected long append(String table, List<String> columns, RecordSource source) throws SQLException, IOException {
        TableSchema schema = source.schema();
        String insert = "INSERT INTO " + table + " (" + String.join(", ", columns) + ", " + ORDINAL
                + ") VALUES (" + "?, ".repeat(columns.size()) + "?)";
        long rows = 0;
        int pending = 0;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
                for (int r = 0; r < batch.rowCount(); r++) {
                    for (int c = 0; c < schema.size(); c++) {
                        bind(statement, c + 1, batch.value(r, c), schema.column(c).type());
                    }
                    statement.setLong(schema.size() + 1, rows + r + 1);
                    statement.addBatch();
                    if (++pending == STATEMENT_BATCH) {
                        statement.executeBatch();
                        pending = 0;
                    }
                }
                rows += batch.rowCount();
            }
            if (pending > 0) {
                statement.executeBatch();
            }
        }
        return rows;
    }

    private static void bind(PreparedStatement statement, int index, Object value, LogicalType type)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, type == LogicalType.DOUBLE ? Types.REAL
                    : type == LogicalType.INT32 || type == LogicalType.BOOLEAN ? Types.INTEGER : Types.VARCHAR);
            return;
        }
        switch (type) {
            case STRING:
                statement.setString(index, (String) value);
                break;
            case INT32:
                statement.setInt(index, (Integer) value);
                break;
            case DOUBLE:
                statement.setDouble(index, (Double) value);
                break;
            case BOOLEAN:
                statement.setInt(index, ((Boolean) value) ? 1 : 0);
                break;
            case DATE:
                // ISO yyyy-MM-dd, which LocalDate always prints at fixed width for years 0000-9999.
                statement.setString(index, value.toString());
                break;
            case TIME:
                // Fixed width: toString would drop zero seconds ("10:15") and vary the fraction.
                statement.setString(index, TIME_TEXT.format((LocalTime) value));
                break;
            case LOCAL_DATETIME:
                statement.setString(index, DATE_TIME_TEXT.format((LocalDateTime) value));
                break;
            default:
                throw new IllegalStateException("Unhandled logical type " + type);
        }
    }
}
