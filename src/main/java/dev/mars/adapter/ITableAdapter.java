package dev.mars.adapter;

import dev.mars.model.ITable;

/**
 * Interface for adapters that connect data sources to Table instances.
 * This interface defines the common methods that all adapters must implement.
 */
public interface ITableAdapter {
    ITable getTable();
}