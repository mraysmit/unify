package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;
import dev.mars.jtable.io.files.xml.IXMLReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of the IXMLReader interface for reading data from XML files.
 */
public class XMLReader implements IXMLReader {
    /**
     * Reads data from a source into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
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

        // Connect if not already connected
        if (!fileConnection.isConnected()) {
            fileConnection.connect();
        }

        // Get the file path from the connection
        String source;
        if (fileConnection.isRemote()) {
            source = fileConnection.getLocation();
        } else {
            Object rawConnection = fileConnection.getRawConnection();
            if (rawConnection instanceof Path) {
                source = ((Path) rawConnection).toString();
            } else {
                throw new IllegalArgumentException("Cannot determine file path from connection");
            }
        }

        // Call the XML-specific method
        readFromXML(xmlDataSource, source, rootElement, rowElement);
    }

    /**
     * Reads data from an XML file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param rootElement the name of the root element in the XML file
     * @param rowElement the name of the row elements in the XML file
     */
    @Override
    public void readFromXML(IXMLDataSource dataSource, String fileName, String rootElement, String rowElement) {
        try {
            // Read the entire XML file into a string
            StringBuilder xmlContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    xmlContent.append(line).append("\n");
                }
            }

            // Parse the XML content
            String xml = xmlContent.toString();

            // Check if the root element exists
            Pattern rootPattern = Pattern.compile("<" + rootElement + "[^>]*>(.*?)</" + rootElement + ">", Pattern.DOTALL);
            Matcher rootMatcher = rootPattern.matcher(xml);

            if (!rootMatcher.find()) {
                System.err.println("Root element '" + rootElement + "' not found in XML file");
                return;
            }

            String rootContent = rootMatcher.group(1);

            // Find all row elements
            Pattern rowPattern = Pattern.compile("<" + rowElement + "[^>]*>(.*?)</" + rowElement + ">", Pattern.DOTALL);
            Matcher rowMatcher = rowPattern.matcher(rootContent);

            List<String> rows = new ArrayList<>();
            while (rowMatcher.find()) {
                rows.add(rowMatcher.group(1));
            }

            if (rows.isEmpty()) {
                return; // No rows found
            }

            // Process the first row to determine columns
            String firstRow = rows.get(0);

            // Find all elements in the first row
            Pattern elementPattern = Pattern.compile("<([^/>]+)>(.*?)</\\1>", Pattern.DOTALL);
            Matcher elementMatcher = elementPattern.matcher(firstRow);

            // Create a map to store column names and types
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            Map<String, String> firstRowData = new HashMap<>();

            // Process elements of the first row to determine column names and types
            while (elementMatcher.find()) {
                String columnName = elementMatcher.group(1);
                String columnValue = unescapeXml(elementMatcher.group(2).trim());
                String columnType = dataSource.inferType(columnValue);
                columns.put(columnName, columnType);
                firstRowData.put(columnName, columnValue);
            }

            // Set the columns in the data source
            dataSource.setColumns(columns);

            // Add the first row
            dataSource.addRow(firstRowData);

            // Process remaining rows
            for (int i = 1; i < rows.size(); i++) {
                String rowContent = rows.get(i);
                Matcher rowElementMatcher = elementPattern.matcher(rowContent);

                // Create a map to store the row data
                Map<String, String> rowData = new HashMap<>();

                // Process elements of the row
                while (rowElementMatcher.find()) {
                    String columnName = rowElementMatcher.group(1);
                    String columnValue = unescapeXml(rowElementMatcher.group(2).trim());
                    rowData.put(columnName, columnValue);
                }

                // Add the row to the data source
                dataSource.addRow(rowData);
            }
        } catch (IOException e) {
            System.err.println("Error reading XML file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing XML data: " + e.getMessage());
        }
    }

    /**
     * Unescapes XML entities in a string.
     *
     * @param value the string to unescape
     * @return the unescaped string
     */
    private String unescapeXml(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&amp;", "&")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'");
    }
}
