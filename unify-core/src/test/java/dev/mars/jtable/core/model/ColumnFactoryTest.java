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

import dev.mars.jtable.core.table.ColumnFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ColumnFactoryTest {

    @Test
    public void testCreateStringColumn() {
        var column = ColumnFactory.createStringColumn("name");
        assertNotNull(column);
        assertEquals("name", column.getName());
        assertEquals(String.class, column.getType());
    }

    @Test
    public void testCreateIntegerColumn() {
        var column = ColumnFactory.createIntegerColumn("age");
        assertNotNull(column);
        assertEquals("age", column.getName());
        assertEquals(Integer.class, column.getType());
    }

    @Test
    public void testCreateDoubleColumn() {
        var column = ColumnFactory.createDoubleColumn("price");
        assertNotNull(column);
        assertEquals("price", column.getName());
        assertEquals(Double.class, column.getType());
    }

    @Test
    public void testCreateBooleanColumn() {
        var column = ColumnFactory.createBooleanColumn("active");
        assertNotNull(column);
        assertEquals("active", column.getName());
        assertEquals(Boolean.class, column.getType());
    }

    @Test
    public void testCreateColumnWithNullName() {
        assertThrows(IllegalArgumentException.class, () ->
                ColumnFactory.createStringColumn(null)
        );
    }

    @Test
    public void testCreateColumnWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                ColumnFactory.createStringColumn("")
        );
    }

    @Test
    public void testCreateCustomColumnWithUnsupportedType() {
        assertThrows(IllegalArgumentException.class, () -> 
                ColumnFactory.createColumn("custom", "unsupportedType")
        );
    }
}
