package dev.mars.jtable.io.common.datasource;


/**
 * Interface for data sources that can be read from or written to databases via JDBC.
 * This interface extends the generic IDataSource interface and inherits all its methods.
 * No additional methods are defined here, as all necessary methods are inherited from IDataSource.
 * This is a marker interface to indicate that the data source is JDBC-compatible.
 */
public interface IJDBCDataSource extends IDataSource {
}