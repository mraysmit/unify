package dev.mars.jtable.io.datasource;

/**
 * Interface for data sources that can be read from or written to various formats.
 * This interface defines the common methods that all data sources must implement.
 */
public interface IDataSource {
    /**
     * Gets the number of rows in the data source.
     *
     * @return the number of rows
     */
    int getRowCount();

    /**
     * Gets the number of columns in the data source.
     *
     * @return the number of columns
     */
    int getColumnCount();

    /**
     * Gets the name of a column at a specific index.
     *
     * @param index the index of the column
     * @return the name of the column
     */
    String getColumnName(int index);

    /**
     * Gets the value at a specific row and column.
     *
     * @param rowIndex the index of the row
     * @param columnName the name of the column
     * @return the value at the specified row and column
     */
    String getValueAt(int rowIndex, String columnName);

    /**
     * Infers the data type of a value.
     *
     * @param value the value to infer the type of
     * @return the inferred data type
     */
    String inferType(String value);

    /**
     * Sets the columns of the data source.
     *
     * @param columns a map of column names to column types
     */
    void setColumns(java.util.LinkedHashMap<String, String> columns);

    /**
     * Adds a row to the data source.
     *
     * @param row a map of column names to values
     */
    void addRow(java.util.Map<String, String> row);
}