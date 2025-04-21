package dev.mars.adapter;

import dev.mars.model.ITable;
import dev.mars.table.Table;
import dev.mars.csv.ICSVDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects CSV data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the ICSVDataSource interface.
 */
public class CSVTableAdapter extends BaseTableAdapter implements ICSVDataSource {

    public CSVTableAdapter(ITable table) {
        super(table);
    }

}
