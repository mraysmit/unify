package dev.mars.jtable.io.rest;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.RESTTableAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import dev.mars.jtable.core.model.ITable;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RESTTableAdapterTest_WireMock {
    private static WireMockServer wireMockServer;
    private ITable table;
    private RESTTableAdapter adapter;
    private String mockEndpoint;

    @BeforeAll
    static void setupServer() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Configure mock responses for basic GET request
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"title\":\"Test Post\",\"body\":\"Test content\",\"userId\":1}]")));

        // Configure mock response for authenticated request
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
                .withHeader("Authorization", containing("Bearer test-token"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":2,\"title\":\"Authenticated Post\",\"body\":\"Secure content\",\"userId\":1}]")));

        // Configure mock response for error case
        wireMockServer.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        // Configure mock response for XML format
        wireMockServer.stubFor(get(urlEqualTo("/xml"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/xml")
                        .withBody("<posts><post><id>3</id><title>XML Post</title><body>XML content</body><userId>1</userId></post></posts>")));

        // Configure mock response for CSV format
        wireMockServer.stubFor(get(urlEqualTo("/csv"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/csv")
                        .withBody("id,title,body,userId\n4,CSV Post,CSV content,1")));
    }

    @AfterAll
    static void stopServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // Setup table and columns
        table = new Table();

        // Set up columns for the table
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("title", "string");
        columns.put("body", "string");
        columns.put("userId", "int");
        table.setColumns(columns);

        // Use the mock server URL
        mockEndpoint = wireMockServer.baseUrl() + "/posts";
        adapter = new RESTTableAdapter(mockEndpoint);
    }

    @Test
    void testReadTable() {
        // Now reads from your controllable mock server
        adapter.readTable();
        assertEquals(1, adapter.getTable().getRowCount());
        assertEquals("Test Post", adapter.getTable().getValueObject(0, "title"));
    }

    @Test
    void testConstructors() {
        // Test the constructors
        RESTTableAdapter adapter1 = new RESTTableAdapter(table);
        assertEquals(table, adapter1.getTable());

        RESTTableAdapter adapter2 = new RESTTableAdapter(mockEndpoint);
        assertNotNull(adapter2.getTable());

        RESTTableAdapter adapter3 = new RESTTableAdapter(mockEndpoint, "test-token");
        assertNotNull(adapter3.getTable());
    }

    @Test
    void testWithMethods() {
        // Test the with methods
        RESTTableAdapter adapter1 = new RESTTableAdapter(mockEndpoint)
                .withMethod("GET")
                .withResponseFormat("json");

        assertNotNull(adapter1);
    }

    @Test
    void testAuthenticatedRequest() {
        // Create a new adapter with authentication
        RESTTableAdapter authAdapter = new RESTTableAdapter(mockEndpoint, "test-token");

        // Read the table from the REST API
        authAdapter.readTable();

        // Verify that authenticated data was read
        assertEquals(1, authAdapter.getTable().getRowCount());
        assertEquals("Authenticated Post", authAdapter.getTable().getValueObject(0, "title"));
    }

    @Test
    void testErrorHandling() {
        // Create a new adapter with an endpoint that will return an error
        String errorEndpoint = wireMockServer.baseUrl() + "/error";
        RESTTableAdapter errorAdapter = new RESTTableAdapter(errorEndpoint);

        // Read the table from the REST API - this should handle the error gracefully
        errorAdapter.readTable();

        // Verify that no data was read (or appropriate error handling occurred)
        assertEquals(0, errorAdapter.getTable().getRowCount());
    }

    @Test
    void testImplementsInterfaces() {
        // Test that the adapter implements the correct interfaces
        assertTrue(adapter instanceof dev.mars.jtable.io.adapter.ITableAdapter);
    }

    @Test
    void testGetTable() {
        // Test that getTable returns the correct table instance
        assertSame(table, new RESTTableAdapter(table).getTable());
    }

    @Test
    void testXmlResponseFormat() {
        // Create a new adapter with XML endpoint
        String xmlEndpoint = wireMockServer.baseUrl() + "/xml";
        RESTTableAdapter xmlAdapter = new RESTTableAdapter(xmlEndpoint)
                .withResponseFormat("xml");

        // Read the table from the REST API
        xmlAdapter.readTable();

        // Verify that XML data was processed
        // Note: The current implementation just stores the raw XML in a single column
        // In a real implementation, this would parse the XML properly
        assertTrue(xmlAdapter.getTable().getRowCount() > 0);
    }

    @Test
    void testCsvResponseFormat() {
        // Create a new adapter with CSV endpoint
        String csvEndpoint = wireMockServer.baseUrl() + "/csv";
        RESTTableAdapter csvAdapter = new RESTTableAdapter(csvEndpoint)
                .withResponseFormat("csv");

        // Read the table from the REST API
        csvAdapter.readTable();

        // Verify that CSV data was processed
        // Note: The current implementation just stores the raw CSV in a single column
        // In a real implementation, this would parse the CSV properly
        assertTrue(csvAdapter.getTable().getRowCount() > 0);
    }
}
