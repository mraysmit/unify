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
package dev.mars.jtable.core.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the table exception hierarchy.
 */
public class ExceptionHierarchyTest {
    
    @Test
    public void testTableExceptionHierarchy() {
        // Test that all exceptions extend TableException
        assertTrue(TableException.class.isAssignableFrom(ColumnException.class));
        assertTrue(TableException.class.isAssignableFrom(RowException.class));
        assertTrue(TableException.class.isAssignableFrom(CellException.class));
        assertTrue(ColumnException.class.isAssignableFrom(DataTypeException.class));
    }
    
    @Test
    public void testTableExceptionCreation() {
        TableException ex1 = new TableException("Test message");
        assertEquals("Test message", ex1.getMessage());
        
        RuntimeException cause = new RuntimeException("Cause");
        TableException ex2 = new TableException("Test message", cause);
        assertEquals("Test message", ex2.getMessage());
        assertEquals(cause, ex2.getCause());
        
        TableException ex3 = new TableException(cause);
        assertEquals(cause, ex3.getCause());
    }
    
    @Test
    public void testColumnExceptionCreation() {
        ColumnException ex1 = new ColumnException("Column error");
        assertEquals("Column error", ex1.getMessage());
        assertNull(ex1.getColumnName());
        
        ColumnException ex2 = new ColumnException("Column error", "TestColumn");
        assertEquals("Column error (Column: TestColumn)", ex2.getMessage());
        assertEquals("TestColumn", ex2.getColumnName());
        
        RuntimeException cause = new RuntimeException("Cause");
        ColumnException ex3 = new ColumnException("Column error", "TestColumn", cause);
        assertEquals("Column error (Column: TestColumn)", ex3.getMessage());
        assertEquals("TestColumn", ex3.getColumnName());
        assertEquals(cause, ex3.getCause());
    }
    
    @Test
    public void testRowExceptionCreation() {
        RowException ex1 = new RowException("Row error");
        assertEquals("Row error", ex1.getMessage());
        assertEquals(-1, ex1.getRowIndex());
        
        RowException ex2 = new RowException("Row error", 5);
        assertEquals("Row error (Row: 5)", ex2.getMessage());
        assertEquals(5, ex2.getRowIndex());
        
        RuntimeException cause = new RuntimeException("Cause");
        RowException ex3 = new RowException("Row error", 10, cause);
        assertEquals("Row error (Row: 10)", ex3.getMessage());
        assertEquals(10, ex3.getRowIndex());
        assertEquals(cause, ex3.getCause());
    }
    
    @Test
    public void testCellExceptionCreation() {
        CellException ex1 = new CellException("Cell error");
        assertEquals("Cell error", ex1.getMessage());
        assertNull(ex1.getColumnName());
        assertEquals(-1, ex1.getRowIndex());
        
        CellException ex2 = new CellException("Cell error", "TestColumn", 5);
        assertEquals("Cell error (Cell: TestColumn[5])", ex2.getMessage());
        assertEquals("TestColumn", ex2.getColumnName());
        assertEquals(5, ex2.getRowIndex());
        
        RuntimeException cause = new RuntimeException("Cause");
        CellException ex3 = new CellException("Cell error", "TestColumn", 10, cause);
        assertEquals("Cell error (Cell: TestColumn[10])", ex3.getMessage());
        assertEquals("TestColumn", ex3.getColumnName());
        assertEquals(10, ex3.getRowIndex());
        assertEquals(cause, ex3.getCause());
    }
    
    @Test
    public void testDataTypeExceptionCreation() {
        DataTypeException ex1 = new DataTypeException("Type error");
        assertEquals("Type error", ex1.getMessage());
        assertNull(ex1.getSourceValue());
        assertNull(ex1.getTargetType());
        
        DataTypeException ex2 = new DataTypeException("Type error", "invalid_value");
        assertEquals("Type error (Value: 'invalid_value')", ex2.getMessage());
        assertEquals("invalid_value", ex2.getSourceValue());
        assertNull(ex2.getTargetType());
        
        DataTypeException ex3 = new DataTypeException("Type error", "123.abc", Integer.class);
        assertEquals("Type error (Value: '123.abc' -> Integer)", ex3.getMessage());
        assertEquals("123.abc", ex3.getSourceValue());
        assertEquals(Integer.class, ex3.getTargetType());
        
        RuntimeException cause = new RuntimeException("Cause");
        DataTypeException ex4 = new DataTypeException("Type error", "123.abc", Integer.class, cause);
        assertEquals("Type error (Value: '123.abc' -> Integer)", ex4.getMessage());
        assertEquals("123.abc", ex4.getSourceValue());
        assertEquals(Integer.class, ex4.getTargetType());
        assertEquals(cause, ex4.getCause());
    }
    
    @Test
    public void testExceptionInheritance() {
        // Test that exceptions can be caught by their parent types
        try {
            throw new ColumnException("Test");
        } catch (TableException e) {
            assertTrue(e instanceof ColumnException);
        }
        
        try {
            throw new DataTypeException("Test");
        } catch (ColumnException e) {
            assertTrue(e instanceof DataTypeException);
        } catch (TableException e) {
            fail("Should have been caught as ColumnException");
        }
    }
}
