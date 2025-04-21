package dev.mars.adapter;

import dev.mars.model.ITable;
import dev.mars.json.IJSONDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects JSON data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IJSONDataSource interface.
 */
public class JSONTableAdapter extends BaseTableAdapter implements IJSONDataSource {

    public JSONTableAdapter(ITable table) {
        super(table);
    }

}

