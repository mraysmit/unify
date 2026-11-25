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
package dev.mars.jtable.pipeline.selection;

/**
 * A source delivered more, or other, data than the size it reported before selection. The
 * operation fails before publication; the engine is never switched mid-operation.
 */
public class SourceChangeException extends RuntimeException {
    private final String source;

    public SourceChangeException(String source, String detail) {
        super("Source '" + source + "' changed after its size was reported: " + detail);
        this.source = source;
    }

    public String source() {
        return source;
    }
}
