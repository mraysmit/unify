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
