package dev.mars.jtable.io.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.mars.jtable.io.datasource.IDataSource;
import dev.mars.jtable.io.datasource.IDataSourceConnection;
import dev.mars.jtable.io.file.FileConnection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Implementation of the IJSONWriter interface for writing data to JSON files.
 */
public class JSONWriter implements IJSONWriter {
    /**
     * Writes data from a data source to a destination using the provided connection.
     *
     * @param dataSource the data source to write from
     * @param connection the connection to the destination
     * @param options additional options for writing (implementation-specific)
     */
    @Override
    public void writeData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Convert the generic dataSource to a JSON-specific dataSource
        IJSONDataSource jsonDataSource;
        if (dataSource instanceof IJSONDataSource) {
            jsonDataSource = (IJSONDataSource) dataSource;
        } else {
            throw new IllegalArgumentException("Data source must implement IJSONDataSource");
        }

        // Ensure we have a file connection
        if (!(connection instanceof FileConnection)) {
            throw new IllegalArgumentException("Connection must be a FileConnection");
        }
        FileConnection fileConnection = (FileConnection) connection;

        // Extract options
        boolean prettyPrint = options != null && options.containsKey("prettyPrint") ? (Boolean) options.get("prettyPrint") : false;

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

        // Call the JSON-specific method
        writeToJSON(jsonDataSource, destination, prettyPrint);
    }

    /**
     * Writes data from a data source to a JSON file.
     *
     * @param dataSource the data source to write from
     * @param fileName the name of the file to write to
     * @param prettyPrint whether to format the JSON output for readability
     */
    @Override
    public void writeToJSON(IJSONDataSource dataSource, String fileName, boolean prettyPrint) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (prettyPrint) {
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
            }

            // Create an array to hold all rows
            ArrayNode rootArray = mapper.createArrayNode();

            // Add each row as an object in the array
            for (int i = 0; i < dataSource.getRowCount(); i++) {
                ObjectNode rowObject = mapper.createObjectNode();
                for (int j = 0; j < dataSource.getColumnCount(); j++) {
                    String columnName = dataSource.getColumnName(j);
                    String value = dataSource.getValueAt(i, columnName);
                    addValueToObjectNode(rowObject, columnName, value);
                }
                rootArray.add(rowObject);
            }

            // Write the array to the file
            mapper.writeValue(new File(fileName), rootArray);
        } catch (IOException e) {
            System.err.println("Error writing JSON file: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Error processing JSON data: " + e.getMessage());
        }
    }

    /**
     * Adds a value to an object node, converting it to the appropriate JSON type if possible.
     *
     * @param objectNode the object node to add the value to
     * @param fieldName the name of the field
     * @param value the value to add
     */
    private void addValueToObjectNode(ObjectNode objectNode, String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            objectNode.putNull(fieldName);
            return;
        }

        // Try to convert the value to the appropriate type
        if (value.matches("-?\\d+")) {
            try {
                int intValue = Integer.parseInt(value);
                objectNode.put(fieldName, intValue);
                return;
            } catch (NumberFormatException e) {
                // If parsing as int fails, try as long
                try {
                    long longValue = Long.parseLong(value);
                    objectNode.put(fieldName, longValue);
                    return;
                } catch (NumberFormatException e2) {
                    // Fall back to string
                }
            }
        } else if (value.matches("-?\\d*\\.\\d+")) {
            try {
                double doubleValue = Double.parseDouble(value);
                objectNode.put(fieldName, doubleValue);
                return;
            } catch (NumberFormatException e) {
                // Fall back to string
            }
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            boolean boolValue = Boolean.parseBoolean(value);
            objectNode.put(fieldName, boolValue);
            return;
        }

        // Default to string
        objectNode.put(fieldName, value);
    }
}
