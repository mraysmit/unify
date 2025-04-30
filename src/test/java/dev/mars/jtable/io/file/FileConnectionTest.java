package dev.mars.jtable.io.file;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the FileConnection class.
 * These tests validate the functionality of the FileConnection class,
 * which is used to connect to file-based data sources.
 */
class FileConnectionTest {

    private static final String TEST_FILE_NAME = "test_file.csv";
    private static final String TEST_FILE_CONTENT = "Name,Age,Occupation\nAlice,30,Engineer\nBob,25,Designer\n";
    private File testFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary test file
        testFile = tempDir.resolve(TEST_FILE_NAME).toFile();
        Files.write(testFile.toPath(), TEST_FILE_CONTENT.getBytes());
    }

    @AfterEach
    void tearDown() {
        // Clean up the test file
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    void testConstructor() {
        // Test constructor with valid parameters
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        
        assertEquals(testFile.getAbsolutePath(), connection.getLocation());
        assertEquals("csv", connection.getConnectionType());
        assertFalse(connection.isConnected());
        assertFalse(connection.isRemote());
    }

    @Test
    void testConnectToExistingFile() {
        // Test connecting to an existing file
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        
        assertTrue(connection.connect());
        assertTrue(connection.isConnected());
        assertFalse(connection.isRemote());
    }

    @Test
    void testConnectToNonExistentFile() {
        // Test connecting to a non-existent file
        String nonExistentFile = tempDir.resolve("non_existent.csv").toString();
        FileConnection connection = new FileConnection(nonExistentFile, "csv");
        
        assertFalse(connection.connect());
        assertFalse(connection.isConnected());
    }

    @Test
    void testDisconnect() {
        // Test disconnecting from a file
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        connection.connect();
        
        assertTrue(connection.isConnected());
        
        connection.disconnect();
        assertFalse(connection.isConnected());
    }

    @Test
    void testGetConnectionType() {
        // Test getting the connection type
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        assertEquals("csv", connection.getConnectionType());
        
        // Test with different file types
        FileConnection jsonConnection = new FileConnection(testFile.getAbsolutePath(), "JSON");
        assertEquals("json", jsonConnection.getConnectionType()); // Should be lowercase
        
        FileConnection xmlConnection = new FileConnection(testFile.getAbsolutePath(), "XML");
        assertEquals("xml", xmlConnection.getConnectionType()); // Should be lowercase
    }

    @Test
    void testGetRawConnection() {
        // Test getting the raw connection for a local file
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        connection.connect();
        
        Object rawConnection = connection.getRawConnection();
        assertNotNull(rawConnection);
        assertTrue(rawConnection instanceof Path);
    }

    @Test
    void testGetProperties() {
        // Test getting the properties
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        
        Map<String, Object> properties = connection.getProperties();
        assertNotNull(properties);
        assertTrue(properties.isEmpty()); // Initially empty
        
        // Add a property and check if it's there
        properties.put("testKey", "testValue");
        assertEquals("testValue", connection.getProperties().get("testKey"));
    }

    @Test
    void testGetLocation() {
        // Test getting the location
        String location = testFile.getAbsolutePath();
        FileConnection connection = new FileConnection(location, "csv");
        
        assertEquals(location, connection.getLocation());
    }

    @Test
    void testIsRemote() {
        // Test isRemote for a local file
        FileConnection connection = new FileConnection(testFile.getAbsolutePath(), "csv");
        connection.connect();
        
        assertFalse(connection.isRemote());
    }

    // Note: Testing remote URLs would require mocking HTTP responses or using a local HTTP server
    // This is beyond the scope of this simple test class
}