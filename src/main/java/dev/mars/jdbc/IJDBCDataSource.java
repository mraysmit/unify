package dev.mars.jdbc;

import dev.mars.datasource.IDataSource;

/**
 * Interface for data sources that can be read from or written to databases via JDBC.
 * This interface extends the generic IDataSource interface and inherits all its methods.
 * No additional methods are defined here, as all necessary methods are inherited from IDataSource.
 */
public interface IJDBCDataSource extends IDataSource {
}