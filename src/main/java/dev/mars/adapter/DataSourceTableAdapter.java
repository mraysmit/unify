package dev.mars.adapter;

import dev.mars.model.ITable;
import dev.mars.datasource.IDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects generic data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IDataSource interface.
 */
public class DataSourceTableAdapter extends BaseTableAdapter implements ITableAdapter {

    public DataSourceTableAdapter(ITable table) { super( table ); }

}
