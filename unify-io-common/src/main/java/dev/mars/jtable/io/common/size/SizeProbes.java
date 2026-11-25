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
package dev.mars.jtable.io.common.size;

import dev.mars.jtable.core.model.ITable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The size probes of the supported source kinds (TD "Size probes"). Each runs before any record
 * is read and costs at most one metadata call or one bounded count. HTTP staging belongs to the
 * HTTP connector, which measures its staged copy with {@link SourceSize#stagedBytes(long)}.
 */
public final class SizeProbes {

    private SizeProbes() {
    }

    /** The exact byte size of a local file, from file-system metadata. */
    public static SourceSize file(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("Not a readable file: " + file.getFileName());
        }
        return SourceSize.fileBytes(Files.size(file));
    }

    /**
     * Counts the records of a JDBC query, stopping at {@code limit}: one more than the selection
     * profile's largest record boundary, so the cost is bounded by {@code limit} rows. A count
     * below the limit is exact; a count at the limit means "above the largest boundary".
     *
     * <p>The query is the source's own read query, embedded as a derived table. The caller owns
     * the connection; where the product supports it, the probe and the read should share one
     * read-only transaction so they see the same data.</p>
     */
    public static SourceSize jdbc(Connection connection, String sourceQuery, long limit) throws IOException {
        if (limit < 1) {
            throw new IllegalArgumentException("The count limit must be at least 1");
        }
        String probe = "SELECT COUNT(*) FROM (SELECT 1 AS one FROM (" + sourceQuery + ") source_query LIMIT "
                + limit + ") probe";
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(probe)) {
            if (!result.next()) {
                throw new IOException("The size probe returned no count");
            }
            return SourceSize.boundedCount(result.getLong(1), limit);
        } catch (SQLException e) {
            throw new IOException("The size probe failed: " + e.getMessage(), e);
        }
    }

    /** The exact record count of a supplied in-memory table. */
    public static SourceSize supplied(ITable table) {
        return SourceSize.suppliedCount(table.getRowCount());
    }
}
