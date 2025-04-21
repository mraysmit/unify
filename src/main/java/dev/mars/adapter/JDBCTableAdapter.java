package dev.mars.adapter;

import com.fasterxml.jackson.databind.ser.Serializers;
import dev.mars.model.ITable;
import dev.mars.jdbc.IJDBCDataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adapter that connects JDBC data sources to Table instances.
 * This adapter implements both the ITableAdapter interface and the IJDBCDataSource interface.
 */
public class JDBCTableAdapter extends BaseTableAdapter implements IJDBCDataSource {

    public JDBCTableAdapter(ITable table) {super (table); }

}
