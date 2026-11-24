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
import dev.mars.jtable.io.common.stream.RecordSource;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.StringJoiner;

/**
 * SQLite execution engine. Datasets are STRICT tables, so SQLite itself rejects a value of the
 * wrong storage class. Records are loaded with one prepared, typed INSERT executed in batches
 * inside the single load transaction.
 *
 * <p>SQLite has no boolean or temporal storage classes. BOOLEAN is stored as INTEGER 0/1 with a
 * CHECK constraint; DATE, TIME and LOCAL_DATETIME are stored as ISO-8601 text. The dataset's Unify
 * schema restores the logical types when results are read back.</p>
 */
public final class SqliteEngine extends JdbcEngine {
    private static final int STATEMENT_BATCH = 1000;

    private SqliteEngine(Connection connection) throws SQLException {
        super("sqlite", connection);
    }

    /** An engine whose data lives only in memory. */
    public static SqliteEngine inMemory() throws IOException {
        return connect("jdbc:sqlite::memory:");
    }

    /** An engine backed by a database file, created if it does not exist; the caller owns it. */
    public static SqliteEngine open(Path databaseFile) throws IOException {
        return connect("jdbc:sqlite:" + databaseFile.toAbsolutePath());
    }

    private static SqliteEngine connect(String url) throws IOException {
        try {
            return new SqliteEngine(DriverManager.getConnection(url));
        } catch (SQLException e) {
            throw new IOException("Opening SQLite failed: " + e.getMessage(), e);
        }
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
    protected String columnConstraint(ColumnDefinition column) {
        return column.type() == LogicalType.BOOLEAN ? "CHECK (" + quote(column.name()) + " IN (0, 1))" : "";
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
    protected long append(String dataset, RecordSource source) throws SQLException, IOException {
        TableSchema schema = source.schema();
        StringJoiner columns = new StringJoiner(", ");
        StringJoiner markers = new StringJoiner(", ");
        for (ColumnDefinition column : schema.columns()) {
            columns.add(quote(column.name()));
            markers.add("?");
        }
        String insert = "INSERT INTO " + quote(dataset) + " (" + columns + ") VALUES (" + markers + ")";
        long rows = 0;
        int pending = 0;
        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            for (RecordBatch batch = source.nextBatch(); batch != null; batch = source.nextBatch()) {
                for (int r = 0; r < batch.rowCount(); r++) {
                    for (int c = 0; c < schema.size(); c++) {
                        bind(statement, c + 1, batch.value(r, c), schema.column(c).type());
                    }
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
            case TIME:
            case LOCAL_DATETIME:
                // ISO-8601 text; java.time's toString is the ISO local form.
                statement.setString(index, value.toString());
                break;
            default:
                throw new IllegalStateException("Unhandled logical type " + type);
        }
    }
}
