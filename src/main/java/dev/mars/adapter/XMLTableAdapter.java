package dev.mars.adapter;

import dev.mars.model.ITable;
import dev.mars.xml.IXMLDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects XML data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IXMLDataSource interface.
 */
public class XMLTableAdapter extends BaseTableAdapter implements IXMLDataSource {

    public XMLTableAdapter(ITable table) {
        super(table);
    }
}
