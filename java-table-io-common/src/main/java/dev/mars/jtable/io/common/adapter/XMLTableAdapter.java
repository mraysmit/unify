package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;


/**
 * Adapter that connects XML data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IXMLDataSource interface.
 */
public class XMLTableAdapter extends BaseTableAdapter implements IXMLDataSource {

    public XMLTableAdapter(ITable table) {
        super(table);
    }
}
