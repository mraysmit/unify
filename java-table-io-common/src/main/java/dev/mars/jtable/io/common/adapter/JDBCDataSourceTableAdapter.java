package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.IJDBCDataSource;

/**
 * Adapter that connects JDBC data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IJDBCDataSource interface.
 */
public class JDBCDataSourceTableAdapter extends BaseTableAdapter implements ITableAdapter, IJDBCDataSource {

    /**
     * Creates a new JDBCDataSourceTableAdapter with the specified table.
     *
     * @param table the table to adapt
     */
    public JDBCDataSourceTableAdapter(ITable table) {
        super(table);
    }
}