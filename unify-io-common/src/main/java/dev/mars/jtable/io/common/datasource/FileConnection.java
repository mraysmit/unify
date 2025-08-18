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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of IDataSourceConnection for file-based data sources.
 * Supports local files, network shares, and HTTP/HTTPS URLs.
 */
public class FileConnection implements IDataSourceConnection {
    private static final Logger logger = LoggerFactory.getLogger(FileConnection.class);
    private String location;
    private String fileType;
    private Path filePath;
    private URL fileUrl;
    private boolean isConnected;
    private Map<String, Object> properties;

    /**
     * Creates a new file connection.
     *
     * @param location the file location (path or URL)
     * @param fileType the file type (e.g., "csv", "json", "xml")
     */
    public FileConnection(String location, String fileType) {
        this.location = location;
        this.fileType = fileType.toLowerCase();
        this.properties = new HashMap<>();
    }

    @Override
    public boolean connect() {
        try {
            // Check if it's a URL
            if (location.startsWith("http://") || location.startsWith("https://")) {

                try {
                    URI fileUri = new URI(location);
                    fileUrl = fileUri.toURL();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                // Test connection by opening and closing a stream
                fileUrl.openStream().close();
                isConnected = true;
            } else {
                // Treat as a file path
                filePath = Paths.get(location);
                isConnected = Files.exists(filePath);
            }
            return isConnected;
        } catch (IOException e) {
            logger.error("Error connecting to file: {}", e.getMessage());
            isConnected = false;
            return false;

        }
    }

    @Override
    public void disconnect() {
        isConnected = false;
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public String getConnectionType() {
        return fileType;
    }

    @Override
    public Object getRawConnection() {
        return fileUrl != null ? fileUrl : filePath;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

    // Additional file-specific methods?
    public String getLocation() {
        return location;
    }

    public boolean isRemote() {
        return fileUrl != null;
    }
}
