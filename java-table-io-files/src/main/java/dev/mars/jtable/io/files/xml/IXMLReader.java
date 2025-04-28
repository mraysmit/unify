package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.IDataReader;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;

/**
 * Interface for reading data from XML files.
 * This interface extends the generic IDataReader interface and adds XML-specific methods.
 */
public interface IXMLReader extends IDataReader {
    /**
     * Reads data from an XML file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param rootElement the name of the root element in the XML file
     * @param rowElement the name of the row elements in the XML file
     */
    void readFromXML(IXMLDataSource dataSource, String fileName, String rootElement, String rowElement);
}
