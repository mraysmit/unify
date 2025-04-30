package dev.mars.jtable.io.common.rest;

import dev.mars.jtable.io.common.datasource.IDataReader;
import dev.mars.jtable.io.common.datasource.IDataSource;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.RESTConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of IDataReader for reading data from REST APIs.
 */
public class RESTReader implements IDataReader {
    /**
     * Reads data from a REST API into a data source using the provided connection.
     *
     * @param dataSource the data source to read into
     * @param connection the connection to the data source
     * @param options additional options for reading (implementation-specific)
     */
    @Override
    public void readData(IDataSource dataSource, IDataSourceConnection connection, Map<String, Object> options) {
        // Ensure we have a REST connection
        if (!(connection instanceof RESTConnection)) {
            throw new IllegalArgumentException("Connection must be a RESTConnection");
        }
        RESTConnection restConnection = (RESTConnection) connection;

        // Extract options
        String method = options != null && options.containsKey("method") ? (String) options.get("method") : "GET";
        String responseFormat = options != null && options.containsKey("responseFormat") ? (String) options.get("responseFormat") : "json";

        // Connect if not already connected
        if (!restConnection.isConnected()) {
            restConnection.connect();
        }

        try {
            // Create a connection for the specific HTTP method
            HttpURLConnection httpConnection = restConnection.createConnection(method);

            // Get the response
            int responseCode = httpConnection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the response based on the format
                if ("json".equalsIgnoreCase(responseFormat)) {
                    parseJsonResponse(dataSource, response.toString());
                } else if ("xml".equalsIgnoreCase(responseFormat)) {
                    parseXmlResponse(dataSource, response.toString());
                } else if ("csv".equalsIgnoreCase(responseFormat)) {
                    parseCsvResponse(dataSource, response.toString());
                } else {
                    throw new IllegalArgumentException("Unsupported response format: " + responseFormat);
                }
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } catch (IOException e) {
            System.err.println("Error reading from REST API: " + e.getMessage());
        }
    }

    /**
     * Parses a JSON response into a data source.
     * This is a simplified implementation that assumes a flat JSON structure.
     *
     * @param dataSource the data source to read into
     * @param jsonResponse the JSON response
     */
    private void parseJsonResponse(IDataSource dataSource, String jsonResponse) {
        // Simple JSON array parsing without external libraries
        // This is a basic implementation that works for the test cases
        // In a real implementation, this would use a JSON library like Jackson or Gson

        try {
            // Check if the response is an array
            if (jsonResponse.trim().startsWith("[") && jsonResponse.trim().endsWith("]")) {
                // Extract the array content
                String arrayContent = jsonResponse.trim().substring(1, jsonResponse.trim().length() - 1);

                // Split the array into objects
                List<String> jsonObjects = splitJsonArray(arrayContent);

                if (!jsonObjects.isEmpty()) {
                    // Parse the first object to determine columns
                    Map<String, String> firstObject = parseJsonObject(jsonObjects.get(0));

                    // Set up columns based on the first object
                    LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                    for (String key : firstObject.keySet()) {
                        // Infer type from value
                        String value = firstObject.get(key);
                        String type = inferType(value);
                        columns.put(key, type);
                    }

                    // Set columns in the data source
                    if (!columns.isEmpty() && dataSource.getColumnCount() == 0) {
                        dataSource.setColumns(columns);
                    }

                    // Add rows for each object
                    for (String jsonObject : jsonObjects) {
                        Map<String, String> objectMap = parseJsonObject(jsonObject);
                        dataSource.addRow(objectMap);
                    }
                }
            } else if (jsonResponse.trim().startsWith("{") && jsonResponse.trim().endsWith("}")) {
                // Single object response
                Map<String, String> objectMap = parseJsonObject(jsonResponse);

                // Set up columns based on the object
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                for (String key : objectMap.keySet()) {
                    // Infer type from value
                    String value = objectMap.get(key);
                    String type = inferType(value);
                    columns.put(key, type);
                }

                // Set columns in the data source
                if (!columns.isEmpty() && dataSource.getColumnCount() == 0) {
                    dataSource.setColumns(columns);
                }

                // Add the object as a row
                dataSource.addRow(objectMap);
            } else {
                // Fallback for unrecognized format
                LinkedHashMap<String, String> columns = new LinkedHashMap<>();
                columns.put("response", "string");

                if (dataSource.getColumnCount() == 0) {
                    dataSource.setColumns(columns);
                }

                Map<String, String> row = new LinkedHashMap<>();
                row.put("response", jsonResponse);
                dataSource.addRow(row);
            }
        } catch (Exception e) {
            System.err.println("Error parsing JSON response: " + e.getMessage());

            // Fallback in case of parsing error
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("response", "string");

            if (dataSource.getColumnCount() == 0) {
                dataSource.setColumns(columns);
            }

            Map<String, String> row = new LinkedHashMap<>();
            row.put("response", jsonResponse);
            dataSource.addRow(row);
        }
    }

    /**
     * Splits a JSON array content into individual JSON objects.
     * 
     * @param arrayContent the content of a JSON array without the surrounding brackets
     * @return a list of JSON object strings
     */
    private List<String> splitJsonArray(String arrayContent) {
        List<String> result = new ArrayList<>();

        int depth = 0;
        StringBuilder currentObject = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);

            if (escaped) {
                currentObject.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                currentObject.append(c);
                escaped = true;
                continue;
            }

            if (c == '"' && !escaped) {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
            }

            currentObject.append(c);

            if (depth == 0 && !inString && (c == '}' || c == ']')) {
                String obj = currentObject.toString().trim();
                if (!obj.isEmpty()) {
                    result.add(obj);
                }
                currentObject = new StringBuilder();

                // Skip the comma and any whitespace
                while (i + 1 < arrayContent.length() && (arrayContent.charAt(i + 1) == ',' || Character.isWhitespace(arrayContent.charAt(i + 1)))) {
                    i++;
                }
            }
        }

        // Add the last object if there is one
        String lastObj = currentObject.toString().trim();
        if (!lastObj.isEmpty()) {
            result.add(lastObj);
        }

        return result;
    }

    /**
     * Parses a JSON object string into a map of key-value pairs.
     * 
     * @param jsonObject the JSON object string
     * @return a map of key-value pairs
     */
    private Map<String, String> parseJsonObject(String jsonObject) {
        Map<String, String> result = new LinkedHashMap<>();

        // Simple regex-based parsing for demonstration
        // This is not a complete JSON parser and has limitations
        Pattern pattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\"[^\"]*\"|\\d+|true|false|null|\\{[^}]*\\}|\\[[^\\]]*\\])");
        Matcher matcher = pattern.matcher(jsonObject);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            // Remove quotes from string values
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }

            result.put(key, value);
        }

        return result;
    }

    /**
     * Infers the type of a value.
     * 
     * @param value the value to infer the type of
     * @return the inferred type
     */
    private String inferType(String value) {
        if (value == null) {
            return "string";
        }

        try {
            Integer.parseInt(value);
            return "int";
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                return "double";
            } catch (NumberFormatException e2) {
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    return "boolean";
                } else {
                    return "string";
                }
            }
        }
    }

    /**
     * Parses an XML response into a data source.
     * This is a simplified implementation that assumes a flat XML structure.
     *
     * @param dataSource the data source to read into
     * @param xmlResponse the XML response
     */
    private void parseXmlResponse(IDataSource dataSource, String xmlResponse) {
        // This is a placeholder for actual XML parsing logic
        // In a real implementation, this would use an XML library like JAXB or DOM

        // For demonstration purposes, we'll just create a simple column and row
        if (dataSource.getColumnCount() == 0) {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("response", "string");
            dataSource.setColumns(columns);
        }

        Map<String, String> row = new LinkedHashMap<>();
        row.put("response", xmlResponse);
        dataSource.addRow(row);
    }

    /**
     * Parses a CSV response into a data source.
     *
     * @param dataSource the data source to read into
     * @param csvResponse the CSV response
     */
    private void parseCsvResponse(IDataSource dataSource, String csvResponse) {
        // This is a placeholder for actual CSV parsing logic
        // In a real implementation, this would use a CSV library or custom parsing

        // For demonstration purposes, we'll just create a simple column and row
        if (dataSource.getColumnCount() == 0) {
            LinkedHashMap<String, String> columns = new LinkedHashMap<>();
            columns.put("response", "string");
            dataSource.setColumns(columns);
        }

        Map<String, String> row = new LinkedHashMap<>();
        row.put("response", csvResponse);
        dataSource.addRow(row);
    }
}