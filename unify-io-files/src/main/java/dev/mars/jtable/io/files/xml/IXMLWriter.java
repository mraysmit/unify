package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.IDataWriter;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;

/**
 * Interface for writing data to XML files.
 * This interface extends the generic IDataWriter interface and adds XML-specific methods.
 */
public interface IXMLWriter extends IDataWriter {
    /**
     * Writes data from a data source to an XML file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param rootElement the name of the root element in the XML file
     * @param rowElement the name of the row elements in the XML file
     * @param indentOutput whether to format the XML output with indentation for readability
     */
    void writeToXML(IXMLDataSource dataSource, String fileName, String rootElement, String rowElement, boolean indentOutput);
}
