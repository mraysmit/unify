package dev.mars.jtable.io.rest;

import dev.mars.jtable.core.model.ITable;
import dev.mars.jtable.core.table.Table;
import dev.mars.jtable.io.adapter.RESTTableAdapter;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

class RESTTableAdapterTest_OkHttp {
    private static MockWebServer mockServer;
    private ITable table;
    private RESTTableAdapter adapter;

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        // Setup table and columns as before
        table = new Table();
        // ...column setup code...

        // Queue a response for the next request
        mockServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("[{\"id\":1,\"title\":\"Test Post\",\"body\":\"Test content\",\"userId\":1}]"));

        // Use the mock server URL
        String mockEndpoint = mockServer.url("/posts").toString();
        adapter = new RESTTableAdapter(mockEndpoint);
    }

    @Test
    void testReadTable() {
        adapter.readTable();
        assertEquals(1, adapter.getTable().getRowCount());
    }
}