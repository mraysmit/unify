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
package dev.mars.jtable.io.common.exception;

/**
 * Exception thrown when there are issues with column mapping configuration.
 * This includes invalid mapping definitions, missing mappings, and mapping validation errors.
 */
public class MappingException extends DataSourceException {
    
    private final String mappingName;
    private final String sourceColumn;
    private final String targetColumn;
    
    /**
     * Creates a new MappingException with the specified message.
     *
     * @param message the detail message
     */
    public MappingException(String message) {
        super(message);
        this.mappingName = null;
        this.sourceColumn = null;
        this.targetColumn = null;
    }
    
    /**
     * Creates a new MappingException with the specified message and mapping name.
     *
     * @param message the detail message
     * @param mappingName the name of the mapping configuration
     */
    public MappingException(String message, String mappingName) {
        super(message + (mappingName != null ? " (Mapping: " + mappingName + ")" : ""));
        this.mappingName = mappingName;
        this.sourceColumn = null;
        this.targetColumn = null;
    }
    
    /**
     * Creates a new MappingException with the specified message, source column, and target column.
     *
     * @param message the detail message
     * @param sourceColumn the source column name
     * @param targetColumn the target column name
     */
    public MappingException(String message, String sourceColumn, String targetColumn) {
        super(message + " (Mapping: " + sourceColumn + " -> " + targetColumn + ")");
        this.mappingName = null;
        this.sourceColumn = sourceColumn;
        this.targetColumn = targetColumn;
    }
    
    /**
     * Creates a new MappingException with the specified message, source column, target column, and cause.
     *
     * @param message the detail message
     * @param sourceColumn the source column name
     * @param targetColumn the target column name
     * @param cause the cause of this exception
     */
    public MappingException(String message, String sourceColumn, String targetColumn, Throwable cause) {
        super(message + " (Mapping: " + sourceColumn + " -> " + targetColumn + ")", null, cause);
        this.mappingName = null;
        this.sourceColumn = sourceColumn;
        this.targetColumn = targetColumn;
    }
    
    /**
     * Gets the name of the mapping configuration that caused this exception.
     *
     * @return the mapping name, or null if not specified
     */
    public String getMappingName() {
        return mappingName;
    }
    
    /**
     * Gets the source column name that caused this exception.
     *
     * @return the source column name, or null if not specified
     */
    public String getSourceColumn() {
        return sourceColumn;
    }
    
    /**
     * Gets the target column name that caused this exception.
     *
     * @return the target column name, or null if not specified
     */
    public String getTargetColumn() {
        return targetColumn;
    }
}
