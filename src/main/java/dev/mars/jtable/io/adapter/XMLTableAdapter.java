package dev.mars.jtable.io.adapter;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.io.xml.IXMLDataSource;

/**
 * Adapter that connects XML data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IXMLDataSource interface.
 */
public class XMLTableAdapter extends BaseTableAdapter implements IXMLDataSource {

    public XMLTableAdapter(ITable table) {
        super(table);
    }
}
