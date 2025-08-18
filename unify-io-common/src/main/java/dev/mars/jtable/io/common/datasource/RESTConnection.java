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
package dev.mars.jtable.io.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(RESTConnection.class);
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
            logger.error("Error connecting to REST API: {}", e.getMessage());
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
