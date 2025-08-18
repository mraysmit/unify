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

import java.util.List;

/**
 * Interface representing a row in a table.
 * A row contains cells.
 */
public interface IRow {
    /**
     * Gets the cell at the given column.
     *
     * @param column the column
     * @return the cell at the given column
     */
    <T> ICell<T> getCell(IColumn<T> column);

    /**
     * Gets the cell at the given column name.
     *
     * @param columnName the name of the column
     * @return the cell at the given column name
     */
    ICell<?> getCell(String columnName);

    /**
     * Sets the value at the given column.
     *
     * @param column the column
     * @param value the value to set
     */
    <T> void setValue(IColumn<T> column, T value);

    /**
     * Sets the value at the given column name.
     *
     * @param columnName the name of the column
     * @param value the value to set
     */
    void setValue(String columnName, Object value);

    /**
     * Gets all cells in this row.
     *
     * @return all cells in this row
     */
    List<ICell<?>> getCells();

    /**
     * Gets the table this row belongs to.
     *
     * @return the table this row belongs to
     */
    ITable getTable();
}
