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
package dev.mars.jtable.core.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface representing a table.
 * A table contains columns and rows.
 */
public interface ITable {

    /**
     * Gets the name of the table.
     *
     * @return the name of the table
     */
    String getName();

    /**
     * Sets the name of the table.
     *
     * @param name the name to set
     */
    void setName(String name);

    void setCreateDefaultValue(boolean createDefaultValue);

    boolean isCreateDefaultValue();

    String getDefaultValue(String type);

    IColumn<?> getColumn(String name);

    IColumn<?> getColumn(int index);

    List<IColumn<?>> getColumns();

    void addColumn(IColumn<?> column);

    String getColumnName(int index);

    IRow getRow(int index);

    List<IRow> getRows();

    void addRow(IRow row);

    void addRow(Map<String, String> row);

    IRow createRow();

    int getRowCount();

    int getColumnCount();

    Object getValueObject(int rowIndex, String columnName);

    void setValue(int rowIndex, String columnName, Object value);

    String getValueAt(int rowIndex, String columnName);

    void setValueAt(int rowIndex, String columnName, String value);

    @SuppressWarnings("unchecked")
    Object convertValue(String value, IColumn<?> column);

    void setColumns(LinkedHashMap<String, String> columns);

    String inferType(String value);

    void printTable();
}
