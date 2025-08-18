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
package dev.mars.jtable.io.common.mapping;

/**
 * Mapping between a source column and a target column.
 * This class defines how a column in the source data maps to a column in the target Table.
 */
public class ColumnMapping {
    private String sourceColumnName;
    private int sourceColumnIndex;
    private String targetColumnName;
    private String targetColumnType;
    private String defaultValue;

    /**
     * Default constructor for Jackson deserialization.
     */
    public ColumnMapping() {
        this.sourceColumnIndex = -1;
    }

    /**
     * Creates a new ColumnMapping with a source column name.
     *
     * @param sourceColumnName the name of the source column
     * @param targetColumnName the name of the target column
     * @param targetColumnType the type of the target column
     */
    public ColumnMapping(String sourceColumnName, String targetColumnName, String targetColumnType) {
        this.sourceColumnName = sourceColumnName;
        this.sourceColumnIndex = -1; // Not specified
        this.targetColumnName = targetColumnName;
        this.targetColumnType = targetColumnType;
    }

    /**
     * Creates a new ColumnMapping with a source column index.
     *
     * @param sourceColumnIndex the index of the source column
     * @param targetColumnName the name of the target column
     * @param targetColumnType the type of the target column
     */
    public ColumnMapping(int sourceColumnIndex, String targetColumnName, String targetColumnType) {
        this.sourceColumnName = null;
        this.sourceColumnIndex = sourceColumnIndex;
        this.targetColumnName = targetColumnName;
        this.targetColumnType = targetColumnType;
    }

    /**
     * Gets the name of the source column.
     *
     * @return the name of the source column, or null if not specified
     */
    public String getSourceColumnName() {
        return sourceColumnName;
    }

    /**
     * Gets the index of the source column.
     *
     * @return the index of the source column, or -1 if not specified
     */
    public int getSourceColumnIndex() {
        return sourceColumnIndex;
    }

    /**
     * Gets the name of the target column.
     *
     * @return the name of the target column
     */
    public String getTargetColumnName() {
        return targetColumnName;
    }

    /**
     * Gets the type of the target column.
     *
     * @return the type of the target column
     */
    public String getTargetColumnType() {
        return targetColumnType;
    }

    /**
     * Gets the default value for the column.
     *
     * @return the default value, or null if not specified
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    public ColumnMapping setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * Determines if this mapping uses a source column name.
     *
     * @return true if this mapping uses a source column name, false otherwise
     */
    public boolean usesSourceColumnName() {
        return sourceColumnName != null;
    }

    /**
     * Determines if this mapping uses a source column index.
     *
     * @return true if this mapping uses a source column index, false otherwise
     */
    public boolean usesSourceColumnIndex() {
        return sourceColumnIndex >= 0;
    }
}
