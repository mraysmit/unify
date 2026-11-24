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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * The logical value types of the Unify contract. Each type has exactly one canonical Java
 * representation; a non-null value of the type is always an instance of {@link #javaType()}.
 */
public enum LogicalType {
    STRING(String.class),
    INT32(Integer.class),
    DOUBLE(Double.class),
    BOOLEAN(Boolean.class),
    DATE(LocalDate.class),
    TIME(LocalTime.class),
    LOCAL_DATETIME(LocalDateTime.class);

    private final Class<?> javaType;

    LogicalType(Class<?> javaType) {
        this.javaType = javaType;
    }

    /** The canonical Java class of a non-null value of this type. */
    public Class<?> javaType() {
        return javaType;
    }

    /** Whether {@code value} is a non-null instance of this type's canonical Java class. */
    public boolean isInstance(Object value) {
        return javaType.isInstance(value);
    }
}
