package dev.mars.jtable.io.datasource;

import dev.mars.jtable.io.file.FileConnection;
import dev.mars.jtable.io.jdbc.JDBCConnection;
import dev.mars.jtable.io.rest.RESTConnection;
import dev.mars.jtable.io.nosql.NoSQLConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DataSourceConnectionFactory class.
 * These tests validate the functionality of the DataSourceConnectionFactory class,
 * which is used to create connections to various data sources.
 */
class DataSourceConnectionFactoryTest {

    private static final String TEST_FILE_CONTENT = "Name,Age,Occupation\nAlice,30,Engineer\nBob,25,Designer\n";

    @TempDir
    Path tempDir;

    @Test
    void testCreateFileConnection() {
        // Test creating a file connection with explicit file type
        String location = "test.csv";
        IDataSourceConnection connection = DataSourceConnectionFactory.createFileConnection(location, "csv");
        
        assertNotNull(connection);
        assertTrue(connection instanceof FileConnection);
        assertEquals("csv", connection.getConnectionType());
        assertEquals(location, ((FileConnection) connection).getLocation());
    }

    @Test
    void testCreateConnectionWithCSVFile() throws IOException {
        // Test creating a connection with a CSV file
        File csvFile = tempDir.resolve("test.csv").toFile();
        Files.write(csvFile.toPath(), TEST_FILE_CONTENT.getBytes());
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(csvFile.getAbsolutePath());
        
        assertNotNull(connection);
        assertTrue(connection instanceof FileConnection);
        assertEquals("csv", connection.getConnectionType());
        assertEquals(csvFile.getAbsolutePath(), ((FileConnection) connection).getLocation());
    }

    @Test
    void testCreateConnectionWithJSONFile() throws IOException {
        // Test creating a connection with a JSON file
        File jsonFile = tempDir.resolve("test.json").toFile();
        Files.write(jsonFile.toPath(), "{\"name\":\"Alice\",\"age\":30}".getBytes());
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(jsonFile.getAbsolutePath());
        
        assertNotNull(connection);
        assertTrue(connection instanceof FileConnection);
        assertEquals("json", connection.getConnectionType());
        assertEquals(jsonFile.getAbsolutePath(), ((FileConnection) connection).getLocation());
    }

    @Test
    void testCreateConnectionWithXMLFile() throws IOException {
        // Test creating a connection with an XML file
        File xmlFile = tempDir.resolve("test.xml").toFile();
        Files.write(xmlFile.toPath(), "<root><name>Alice</name><age>30</age></root>".getBytes());
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(xmlFile.getAbsolutePath());
        
        assertNotNull(connection);
        assertTrue(connection instanceof FileConnection);
        assertEquals("xml", connection.getConnectionType());
        assertEquals(xmlFile.getAbsolutePath(), ((FileConnection) connection).getLocation());
    }

    @Test
    void testCreateConnectionWithJDBCUrl() {
        // Test creating a connection with a JDBC URL
        String jdbcUrl = "jdbc:mysql://localhost:3306/testdb";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(jdbcUrl);
        
        assertNotNull(connection);
        assertTrue(connection instanceof JDBCConnection);
    }

    @Test
    void testCreateConnectionWithRESTUrl() {
        // Test creating a connection with a REST API URL
        String restUrl = "https://api.example.com/rest/data";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(restUrl);
        
        assertNotNull(connection);
        assertTrue(connection instanceof RESTConnection);
    }

    @Test
    void testCreateConnectionWithMongoDBUrl() {
        // Test creating a connection with a MongoDB URL
        String mongoUrl = "mongodb://localhost:27017";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(mongoUrl);
        
        assertNotNull(connection);
        assertTrue(connection instanceof NoSQLConnection);
    }

    @Test
    void testCreateConnectionWithUnsupportedFormat() {
        // Test creating a connection with an unsupported format
        String unsupportedUrl = "unsupported://example.com";
        
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            DataSourceConnectionFactory.createConnection(unsupportedUrl);
        });
        
        assertTrue(exception.getMessage().contains("Unsupported location format"));
    }

    @Test
    void testCreateConnectionWithHttpUrl() {
        // Test creating a connection with an HTTP URL that is not a REST API
        String httpUrl = "https://example.com/data.csv";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createConnection(httpUrl);
        
        assertNotNull(connection);
        assertTrue(connection instanceof FileConnection);
        assertEquals("csv", connection.getConnectionType());
    }

    @Test
    void testCreateDatabaseConnection() {
        // Test creating a database connection
        String jdbcUrl = "jdbc:mysql://localhost:3306/testdb";
        String username = "user";
        String password = "password";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createDatabaseConnection(jdbcUrl, username, password);
        
        assertNotNull(connection);
        assertTrue(connection instanceof JDBCConnection);
    }

    @Test
    void testCreateRESTConnection() {
        // Test creating a REST connection
        String endpoint = "https://api.example.com/data";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createRESTConnection(endpoint);
        
        assertNotNull(connection);
        assertTrue(connection instanceof RESTConnection);
    }

    @Test
    void testCreateRESTConnectionWithAuth() {
        // Test creating a REST connection with authentication
        String endpoint = "https://api.example.com/data";
        String authToken = "token123";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createRESTConnection(endpoint, authToken);
        
        assertNotNull(connection);
        assertTrue(connection instanceof RESTConnection);
    }

    @Test
    void testCreateNoSQLConnection() {
        // Test creating a NoSQL connection
        String connectionString = "mongodb://localhost:27017";
        String database = "testdb";
        String collection = "testcollection";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createNoSQLConnection(connectionString, database, collection);
        
        assertNotNull(connection);
        assertTrue(connection instanceof NoSQLConnection);
    }

    @Test
    void testCreateNoSQLConnectionWithAuth() {
        // Test creating a NoSQL connection with authentication
        String connectionString = "mongodb://localhost:27017";
        String database = "testdb";
        String collection = "testcollection";
        String username = "user";
        String password = "password";
        
        IDataSourceConnection connection = DataSourceConnectionFactory.createNoSQLConnection(connectionString, database, collection, username, password);
        
        assertNotNull(connection);
        assertTrue(connection instanceof NoSQLConnection);
    }
}