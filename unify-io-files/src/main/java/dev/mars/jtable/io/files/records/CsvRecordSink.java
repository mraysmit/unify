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
import dev.mars.jtable.core.schema.ValueNormalizer;
import dev.mars.jtable.io.common.stream.RecordSink;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes checked records to a new CSV file, streaming batch by batch.
 *
 * <p>Output goes to a temporary file in the target's directory and is moved to the target name
 * only by {@link #complete()}; closing without completing deletes it, so a failed operation never
 * leaves a partial file under the target name. The target must not exist. (Verified no-overwrite
 * and atomic-replace publication modes are specified separately as TD04.)</p>
 *
 * <p>Values use the locale-independent canonical text of {@link ValueNormalizer#toText}. Null is
 * the unquoted null token. A field is quoted when it is empty, equals the null token, or contains
 * the delimiter, the quote character or a line break.</p>
 */
public final class CsvRecordSink implements RecordSink {
    private final Path target;
    private final Path temporary;
    private final TableSchema schema;
    private final CsvFormat format;
    private final BufferedWriter writer;
    private boolean completed = false;
    private boolean closed = false;

    public CsvRecordSink(Path target, TableSchema schema, CsvFormat format) throws IOException {
        this.target = target.toAbsolutePath();
        this.schema = schema;
        this.format = format;
        if (Files.exists(this.target)) {
            throw new FileAlreadyExistsException(this.target.toString(), null, "the target file already exists");
        }
        this.temporary = Files.createTempFile(this.target.getParent(), this.target.getFileName() + ".", ".partial");
        this.writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(temporary), StandardCharsets.UTF_8), 1 << 16);
        try {
            if (format.header()) {
                for (int i = 0; i < schema.size(); i++) {
                    if (i > 0) {
                        writer.write(format.delimiter());
                    }
                    writeText(schema.column(i).name());
                }
                writer.write(format.lineSeparator());
            }
        } catch (IOException e) {
            close();
            throw e;
        }
    }

    @Override
    public void write(RecordBatch batch) throws IOException {
        if (completed || closed) {
            throw new IllegalStateException("The sink no longer accepts records");
        }
        if (!batch.schema().equals(schema)) {
            throw new IllegalArgumentException("The batch schema does not match the sink's schema");
        }
        for (int r = 0; r < batch.rowCount(); r++) {
            for (int c = 0; c < schema.size(); c++) {
                if (c > 0) {
                    writer.write(format.delimiter());
                }
                Object value = batch.value(r, c);
                if (value == null) {
                    writer.write(format.nullToken());
                } else {
                    writeText(ValueNormalizer.toText(value));
                }
            }
            writer.write(format.lineSeparator());
        }
    }

    @Override
    public void complete() throws IOException {
        if (completed || closed) {
            throw new IllegalStateException("The sink has already been completed or closed");
        }
        writer.close();
        // Plain move: fails rather than overwriting if the target appeared meanwhile.
        Files.move(temporary, target);
        completed = true;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        if (!completed) {
            try {
                writer.close();
            } finally {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private void writeText(String text) throws IOException {
        if (needsQuotes(text)) {
            char quote = format.quote();
            writer.write(quote);
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == quote) {
                    writer.write(quote);
                }
                writer.write(c);
            }
            writer.write(quote);
        } else {
            writer.write(text);
        }
    }

    private boolean needsQuotes(String text) {
        if (text.isEmpty() || text.equals(format.nullToken())) {
            return true;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == format.delimiter() || c == format.quote() || c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }
}
