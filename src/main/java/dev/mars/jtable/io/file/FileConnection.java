package dev.mars.jtable.io.file;

import dev.mars.jtable.io.datasource.IDataSourceConnection;
import java.io.File;
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
            System.err.println("Error connecting to file: " + e.getMessage());
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