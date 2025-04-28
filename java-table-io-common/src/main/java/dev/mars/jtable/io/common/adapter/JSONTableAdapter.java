package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.IJSONDataSource;


/**
 * Adapter that connects JSON data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IJSONDataSource interface.
 */
public class JSONTableAdapter extends BaseTableAdapter implements IJSONDataSource {

    public JSONTableAdapter(ITable table) {
        super(table);
    }

}

