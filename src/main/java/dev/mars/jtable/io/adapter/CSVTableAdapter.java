package dev.mars.jtable.io.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.csv.ICSVDataSource;

/**
 * Adapter that connects CSV data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the ICSVDataSource interface.
 */
public class CSVTableAdapter extends BaseTableAdapter implements ICSVDataSource {

    public CSVTableAdapter(ITable table) {
        super(table);
    }

}
