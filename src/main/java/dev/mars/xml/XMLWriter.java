package dev.mars.xml;

import dev.mars.datasource.IDataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

/**
 * Implementation of the IXMLWriter interface for writing data to XML files.
 */
public class XMLWriter implements IXMLWriter {
    /**
     * Writes data from a data source to a destination.
     * This method is part of the IDataWriter interface.
     *
     * @param dataSource the data source to write from
     * @param destination the destination to write to (e.g., file name, URL, etc.)
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IDataSource dataSource, String destination, Map<String, Object> options) {
        // Convert the generic dataSource to an XML-specific dataSource
        IXMLDataSource xmlDataSource;
        if (dataSource instanceof IXMLDataSource) {
            xmlDataSource = (IXMLDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IXMLDataSource");
        }

        // Extract options
        String rootElement = options != null && options.containsKey("rootElement") ? (String) options.get("rootElement") : "data";
        String rowElement = options != null && options.containsKey("rowElement") ? (String) options.get("rowElement") : "row";
        boolean indentOutput = options != null && options.containsKey("indentOutput") ? (Boolean) options.get("indentOutput") : false;

        // Call the XML-specific method
        writeToXML(xmlDataSource, destination, rootElement, rowElement, indentOutput);
    }

    /**
     * Writes data from a data source to an XML file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param rootElement the name of the root element in the XML file
     * @param rowElement the name of the row elements in the XML file
     * @param indentOutput whether to format the XML output with indentation for readability
     */
    @Override
    public void writeToXML(IXMLDataSource dataSource, String fileName, String rootElement, String rowElement, boolean indentOutput) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            // Write XML declaration
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            
            // Write root element opening tag
            writer.write("<" + rootElement + ">");
            writer.newLine();
            
            // Write each row
            for (int i = 0; i < dataSource.getRowCount(); i++) {
                // Indent row element if requested
                if (indentOutput) {
                    writer.write("  ");
                }
                
                // Write row element opening tag
                writer.write("<" + rowElement + ">");
                writer.newLine();
                
                // Write each column value
                for (int j = 0; j < dataSource.getColumnCount(); j++) {
                    String columnName = dataSource.getColumnName(j);
                    String value = dataSource.getValueAt(i, columnName);
                    
                    // Indent column element if requested
                    if (indentOutput) {
                        writer.write("    ");
                    }
                    
                    // Write column element with value
                    writer.write("<" + columnName + ">");
                    writer.write(escapeXml(value));
                    writer.write("</" + columnName + ">");
                    writer.newLine();
                }
                
                // Indent row element closing tag if requested
                if (indentOutput) {
                    writer.write("  ");
                }
                
                // Write row element closing tag
                writer.write("</" + rowElement + ">");
                writer.newLine();
            }
            
            // Write root element closing tag
            writer.write("</" + rootElement + ">");
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Error writing XML file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing XML data: " + e.getMessage());
        }
    }
    
    /**
     * Escapes special characters in XML content.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}