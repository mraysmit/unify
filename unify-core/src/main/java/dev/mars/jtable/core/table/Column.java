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
package dev.mars.jtable.core.table;

import dev.mars.jtable.core.model.ICell;
import dev.mars.jtable.core.model.IColumn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Column<T> implements IColumn<T> {
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    /**
     * Creates a new column with the given name, type, and default value.
     *
     * @param name the name of the column
     * @param type the type of the column
     * @param defaultValue the default value for the column
     */
    public Column(String name, Class<T> type, T defaultValue) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Column name cannot be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public T createDefaultValue() {
        return defaultValue;
    }

    @Override
    public boolean isValidValue(Object value) {
        if (value == null) {
            return true; // Allow null values
        }
        return type.isInstance(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T convertFromString(String value) {
        if (value == null || value.isEmpty()) {
            if (type != String.class) {
                return null; // Return null for non-string types
            }
        }

        if (type == String.class) {
            return (T) value;
        } else if (type == Integer.class) {
            return (T) Integer.valueOf(value);
        } else if (type == Double.class) {
            return (T) Double.valueOf(value);
        } else if (type == Boolean.class) {
            return (T) Boolean.valueOf(value);
        } else if (type == LocalDate.class) {
            try {
                return (T) LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format. Expected format: yyyy-MM-dd", e);
            }
        } else if (type == LocalTime.class) {
            try {
                return (T) LocalTime.parse(value, DateTimeFormatter.ISO_LOCAL_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid time format. Expected format: HH:mm:ss", e);
            }
        } else if (type == LocalDateTime.class) {
            try {
                return (T) LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date-time format. Expected format: yyyy-MM-ddTHH:mm:ss", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type.getName());
        }
    }

    @Override
    public ICell<T> createCell(T value) {
        return new Cell<>(this, value);
    }
}
