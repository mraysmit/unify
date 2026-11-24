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
package dev.mars.jtable.core.schema;

/**
 * A value could not be accepted for a column. The message names the column and the rule that
 * failed but never repeats the value itself, which may be sensitive business data.
 */
public class ValueConversionException extends IllegalArgumentException {
    private final String column;

    public ValueConversionException(String column, String reason) {
        super("Column '" + column + "': " + reason);
        this.column = column;
    }

    /** The name of the column whose value was rejected. */
    public String column() {
        return column;
    }
}
