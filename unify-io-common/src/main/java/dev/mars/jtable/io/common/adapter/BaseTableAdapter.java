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
package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.IDataSource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects CSV data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the ICSVDataSource interface.
 */
public abstract class BaseTableAdapter implements ITableAdapter, IDataSource {
    protected final ITable table;

    public BaseTableAdapter(ITable table) {
        this.table = table;
    }

    @Override
    public ITable getTable() {
        return table;
    }

    @Override
    public int getRowCount() {
        return table.getRowCount();
    }

    @Override
    public int getColumnCount() {
        return table.getColumnCount();
    }

    @Override
    public String getColumnName(int index) {
        return table.getColumnName(index);
    }

    @Override
    public String getValueAt(int rowIndex, String columnName) {
        return table.getValueAt(rowIndex, columnName);
    }

    @Override
    public String inferType(String value) {
        return table.inferType(value);
    }

    @Override
    public void setColumns(LinkedHashMap<String, String> columns) {
        table.setColumns(columns);
    }

    @Override
    public void addRow(Map<String, String> row) {
        table.addRow(row);
    }
}
