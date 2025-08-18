/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Implementation of the IXMLWriter interface for writing data to XML files.
 */
public class XMLWriter implements IXMLWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to an XML-specific dataSource
        IXMLDataSource xmlDataSource;
        if (dataSource instanceof IXMLDataSource) {
            xmlDataSource = (IXMLDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IXMLDataSource");
        }

        // Ensure we have a file connection
        if (!(connection instanceof FileConnection)) {
            throw new IllegalArgumentException("Connection must be a FileConnection");
        }
        FileConnection fileConnection = (FileConnection) connection;

        // Extract options
        String rootElement = options != null && options.containsKey("rootElement") ? (String) options.get("rootElement") : "data";
        String rowElement = options != null && options.containsKey("rowElement") ? (String) options.get("rowElement") : "row";
        boolean indentOutput = options != null && options.containsKey("indentOutput") ? (Boolean) options.get("indentOutput") : false;

        // Connect if not already connected
        if (!fileConnection.isConnected()) {
            fileConnection.connect();
        }

        // Get the file path from the connection
        String destination;
        if (fileConnection.isRemote()) {
            destination = fileConnection.getLocation();
        } else {
            Object rawConnection = fileConnection.getRawConnection();
            if (rawConnection instanceof Path) {
                destination = ((Path) rawConnection).toString();
            } else {
                throw new IllegalArgumentException("Cannot determine file path from connection");
            }
        }

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
