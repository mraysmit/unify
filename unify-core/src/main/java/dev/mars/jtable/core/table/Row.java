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
import dev.mars.jtable.core.model.IRow;
import dev.mars.jtable.core.model.ITable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Row implements IRow {
    private final ITable table;
    private final Map<String, ICell<?>> cells = new HashMap<>();

    /**
     * Creates a new row for the given table.
     *
     * @param table the table this row belongs to
     */
    public Row(ITable table) {
        this.table = table;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ICell<T> getCell(IColumn<T> column) {
        return (ICell<T>) cells.get(column.getName());
    }

    @Override
    public ICell<?> getCell(String columnName) {
        return cells.get(columnName);
    }

    @Override
    public <T> void setValue(IColumn<T> column, T value) {
        ICell<T> cell = getCell(column);
        if (cell == null) {
            cell = column.createCell(value);
            cells.put(column.getName(), cell);
        } else {
            cell.setValue(value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setValue(String columnName, Object value) {
        IColumn<?> column = table.getColumn(columnName);
        if (column == null) {
            throw new IllegalArgumentException("Column does not exist: " + columnName);
        }

        ICell<?> cell = getCell(columnName);
        if (cell == null) {
            IColumn<Object> objectColumn = (IColumn<Object>) column;
            cell = objectColumn.createCell((Object) value);
            cells.put(columnName, cell);
        } else {
            ((ICell<Object>) cell).setValue(value);
        }
    }

    @Override
    public List<ICell<?>> getCells() {
        return new ArrayList<>(cells.values());
    }

    @Override
    public ITable getTable() {
        return table;
    }
}
