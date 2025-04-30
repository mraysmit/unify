package dev.mars.jtable.io.common.adapter;

import dev.mars.jtable.core.model.ITable;

/**
 * Interface for adapters that connect data sources to Table instances.
 * This interface defines the common methods that all adapters must implement.
 */
public interface ITableAdapter {
    ITable getTable();
}