package dev.mars.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ColumnFactoryTest {

    private ColumnFactory columnFactory;

    @BeforeEach
    public void setUp() {
        columnFactory = new ColumnFactory();
    }

    @Test
    public void testCreateStringColumn() {
        var column = columnFactory.createStringColumn("name");
        assertNotNull(column);
        assertEquals("name", column.getName());
        assertEquals(String.class, column.getType());
    }

    @Test
    public void testCreateIntegerColumn() {
        var column = columnFactory.createIntegerColumn("age");
        assertNotNull(column);
        assertEquals("age", column.getName());
        assertEquals(Integer.class, column.getType());
    }

    @Test
    public void testCreateDoubleColumn() {
        var column = columnFactory.createDoubleColumn("price");
        assertNotNull(column);
        assertEquals("price", column.getName());
        assertEquals(Double.class, column.getType());
    }

    @Test
    public void testCreateBooleanColumn() {
        var column = columnFactory.createBooleanColumn("active");
        assertNotNull(column);
        assertEquals("active", column.getName());
        assertEquals(Boolean.class, column.getType());
    }

    @Test
    public void testCreateColumnWithNullName() {
        assertThrows(IllegalArgumentException.class, () ->
                columnFactory.createStringColumn(null)
        );
    }

    @Test
    public void testCreateColumnWithEmptyName() {
        assertThrows(IllegalArgumentException.class, () ->
                columnFactory.createStringColumn("")
        );
    }

    @Test
    public void testCreateCustomColumnWithUnsupportedType() {

        assertThrows(IllegalArgumentException.class, () -> columnFactory.createColumn("custom", "unsupportedType"));
    }
}