package dev.mars.jtable.io.adapter;

import dev.mars.jtable.core.model.ITable;

/**
 * Adapter that connects generic data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IDataSource interface.
 */
public class DataSourceTableAdapter extends BaseTableAdapter implements ITableAdapter {

    public DataSourceTableAdapter(ITable table) { super( table ); }

}
