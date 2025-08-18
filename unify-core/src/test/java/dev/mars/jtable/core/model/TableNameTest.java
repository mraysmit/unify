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

import dev.mars.jtable.core.table.OptimizedTableCore;
import dev.mars.jtable.core.table.TableCore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for the Name attribute of ITable implementations.
 * This class tests that the Name attribute is correctly set and retrieved.
 */
public class TableNameTest {

    @Test
    public void testDefaultNames() {
        // Test that the default names are set correctly
        TableCore tableCore = new TableCore();
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore();

        assertNotNull(tableCore.getName(), "TableCore name should not be null");
        assertNotNull(optimizedTableCore.getName(), "OptimizedTableCore name should not be null");

        assertEquals("TableCore", tableCore.getName(), "TableCore default name should be 'TableCore'");
        assertEquals("OptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore default name should be 'OptimizedTableCore'");
    }

    @Test
    public void testCustomNames() {
        // Test that custom names are set correctly
        TableCore tableCore = new TableCore("CustomTableCore");
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore("CustomOptimizedTableCore");

        assertEquals("CustomTableCore", tableCore.getName(), "TableCore custom name should be 'CustomTableCore'");
        assertEquals("CustomOptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore custom name should be 'CustomOptimizedTableCore'");
    }

    @Test
    public void testSetName() {
        // Test that setName works correctly
        TableCore tableCore = new TableCore();
        OptimizedTableCore optimizedTableCore = new OptimizedTableCore();

        tableCore.setName("NewTableCore");
        optimizedTableCore.setName("NewOptimizedTableCore");

        assertEquals("NewTableCore", tableCore.getName(), "TableCore name should be 'NewTableCore'");
        assertEquals("NewOptimizedTableCore", optimizedTableCore.getName(), "OptimizedTableCore name should be 'NewOptimizedTableCore'");
    }

    // Note: testNamePropagation test removed since Table wrapper class was eliminated
    // This was testing the delegation from Table to TableCore, which is no longer needed
}
