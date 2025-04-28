package dev.mars.jtable.io.common.datasource;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of IDataSourceConnection for RESTful web services.
 * Supports HTTP/HTTPS connections to REST APIs.
 */
public class RESTConnection implements IDataSourceConnection {
    private String endpoint;
    private Map<String, String> headers;
    private String authToken;
    private HttpURLConnection connection;
    private boolean isConnected;
    private Map<String, Object> properties;

    /**
     * Creates a new REST connection.
     *
     * @param endpoint the REST API endpoint URL
     */
    public RESTConnection(String endpoint) {
        this.endpoint = endpoint;
        this.headers = new HashMap<>();
        this.properties = new HashMap<>();
    }

    /**
     * Creates a new REST connection with authentication.
     *
     * @param endpoint the REST API endpoint URL
     * @param authToken the authentication token
     */
    public RESTConnection(String endpoint, String authToken) {
        this(endpoint);
        this.authToken = authToken;
        if (authToken != null && !authToken.isEmpty()) {
            headers.put("Authorization", "Bearer " + authToken);
        }
    }

    @Override
    public boolean connect() {
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            
            // Set headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }
            
            // Test connection
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            isConnected = (responseCode >= 200 && responseCode < 300);
            
            return isConnected;
        } catch (IOException e) {
            System.err.println("Error connecting to REST API: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
        isConnected = false;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public String getConnectionType() {
        return "rest";
    }

    @Override
    public Object getRawConnection() {
        return connection;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Sets a header for the HTTP request.
     *
     * @param name the header name
     * @param value the header value
     */
    public void setHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Gets the endpoint URL.
     *
     * @return the endpoint URL
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Creates a new connection for a specific HTTP method.
     *
     * @param method the HTTP method (GET, POST, PUT, DELETE, etc.)
     * @return the HTTP connection
     * @throws IOException if an I/O error occurs
     */
    public HttpURLConnection createConnection(String method) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        
        // Set headers
        for (Map.Entry<String, String> header : headers.entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }
        
        return conn;
    }
}