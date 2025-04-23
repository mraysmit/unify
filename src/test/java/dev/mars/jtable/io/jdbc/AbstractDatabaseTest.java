package dev.mars.jtable.io.jdbc;

import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.JDBCTableAdapter;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractDatabaseTest {

    protected Table table;
    protected JDBCTableAdapter adapter;
    protected JDBCReader reader;
    protected JDBCWriter writer;
    protected final String testTableName = "test_table";

    // Connection details for H2 in-memory database
    protected final String connectionString = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    protected final String username = "";
    protected final String password = "";

    @BeforeEach
    void setUp() throws Exception {
        table = new Table();
        adapter = new JDBCTableAdapter(table);
        reader = new JDBCReader();
        writer = new JDBCWriter();
    }
}
