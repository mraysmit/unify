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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RESTTableAdapterTest_WireMock {
    private static WireMockServer wireMockServer;
    private ITable table;
    private RESTTableAdapter adapter;

    @BeforeAll
    static void setupServer() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();

        // Configure mock responses
        wireMockServer.stubFor(get(urlEqualTo("/posts"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":1,\"title\":\"Test Post\",\"body\":\"Test content\",\"userId\":1}]")));
    }

    @AfterAll
    static void stopServer() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        // Setup table and columns as before
        table = new Table();
        // ...column setup code...

        // Use the mock server URL
        String mockEndpoint = wireMockServer.baseUrl() + "/posts";
        adapter = new RESTTableAdapter(mockEndpoint);
    }

    @Test
    void testReadTable() {
        // Now reads from your controllable mock server
        adapter.readTable();
        assertEquals(1, adapter.getTable().getRowCount());
        assertEquals("Test Post", adapter.getTable().getValue(0, "title"));
    }
}