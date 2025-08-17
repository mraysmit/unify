package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class XMLWriterTest {

    private XMLWriter xmlWriter;
    private MockXMLDataSource dataSource;
    private MockFileConnection fileConnection;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        xmlWriter = new XMLWriter();
        dataSource = new MockXMLDataSource();
        fileConnection = new MockFileConnection();
        
        // Set up test data
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("name", "string");
        columns.put("active", "boolean");
        columns.put("score", "double");
        dataSource.setColumns(columns);
        
        Map<String, String> row1 = new HashMap<>();
        row1.put("id", "1");
        row1.put("name", "Test Name");
        row1.put("active", "true");
        row1.put("score", "95.5");
        dataSource.addRow(row1);
        
        Map<String, String> row2 = new HashMap<>();
        row2.put("id", "2");
        row2.put("name", "Another Name");
        row2.put("active", "false");
        row2.put("score", "82.3");
        dataSource.addRow(row2);
    }

    @Test
    void testWriteToXML() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_write.xml").toFile();
        
        // Write to the file
        xmlWriter.writeToXML(dataSource, testFile.getAbsolutePath(), "data", "row", false);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        String content = readFile(testFile);
        
        // Check XML declaration
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        
        // Check root element
        assertTrue(content.contains("<data>"));
        assertTrue(content.contains("</data>"));
        
        // Check row elements
        assertTrue(content.contains("<row>"));
        assertTrue(content.contains("</row>"));
        
        // Check data values
        assertTrue(content.contains("<id>1</id>"));
        assertTrue(content.contains("<name>Test Name</name>"));
        assertTrue(content.contains("<active>true</active>"));
        assertTrue(content.contains("<score>95.5</score>"));
        
        assertTrue(content.contains("<id>2</id>"));
        assertTrue(content.contains("<name>Another Name</name>"));
        assertTrue(content.contains("<active>false</active>"));
        assertTrue(content.contains("<score>82.3</score>"));
    }

    @Test
    void testWriteToXMLWithIndentation() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_indented.xml").toFile();
        
        // Write to the file with indentation
        xmlWriter.writeToXML(dataSource, testFile.getAbsolutePath(), "data", "row", true);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        String content = readFile(testFile);
        
        // Check XML declaration
        assertTrue(content.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        
        // Check indentation
        assertTrue(content.contains("  <row>"));
        assertTrue(content.contains("    <id>"));
        
        // Check data values
        assertTrue(content.contains("<id>1</id>"));
        assertTrue(content.contains("<name>Test Name</name>"));
    }

    @Test
    void testWriteData() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_write_data.xml").toFile();
        
        // Set up the file connection
        fileConnection.setRawConnection(testFile.toPath());
        fileConnection.setConnected(true);
        
        // Write to the file using writeData
        Map<String, Object> options = new HashMap<>();
        options.put("rootElement", "customData");
        options.put("rowElement", "item");
        options.put("indentOutput", true);
        xmlWriter.writeData(dataSource, fileConnection, options);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        String content = readFile(testFile);
        
        // Check custom element names
        assertTrue(content.contains("<customData>"));
        assertTrue(content.contains("<item>"));
        
        // Check indentation
        assertTrue(content.contains("  <item>"));
        
        // Check data values
        assertTrue(content.contains("<id>1</id>"));
        assertTrue(content.contains("<name>Test Name</name>"));
    }

    @Test
    void testWriteXMLWithSpecialCharacters() throws Exception {
        // Create a test file
        File testFile = tempDir.resolve("test_special_chars.xml").toFile();
        
        // Set up data with special characters
        dataSource = new MockXMLDataSource();
        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        columns.put("id", "int");
        columns.put("name", "string");
        columns.put("description", "string");
        dataSource.setColumns(columns);
        
        Map<String, String> row = new HashMap<>();
        row.put("id", "1");
        row.put("name", "Test & Name");
        row.put("description", "This is a <test> with \"quotes\" and 'apostrophes'");
        dataSource.addRow(row);
        
        // Write to the file
        xmlWriter.writeToXML(dataSource, testFile.getAbsolutePath(), "data", "row", false);
        
        // Verify the file was created
        assertTrue(testFile.exists());
        
        // Read the file and verify its contents
        String content = readFile(testFile);
        
        // Check that special characters are escaped
        assertTrue(content.contains("<name>Test &amp; Name</name>"));
        assertTrue(content.contains("<description>This is a &lt;test&gt; with &quot;quotes&quot; and &apos;apostrophes&apos;</description>"));
        
        // Verify that the XML is well-formed by checking if we can extract values
        String nameValue = extractXmlValue(content, "name");
        String descValue = extractXmlValue(content, "description");
        
        assertEquals("Test &amp; Name", nameValue);
        assertEquals("This is a &lt;test&gt; with &quot;quotes&quot; and &apos;apostrophes&apos;", descValue);
    }
    
    private String readFile(File file) throws Exception {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
    
    private String extractXmlValue(String xml, String tagName) {
        Pattern pattern = Pattern.compile("<" + tagName + ">(.*?)</" + tagName + ">");
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    // Mock classes for testing
    private static class MockXMLDataSource implements IXMLDataSource {
        private LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        private Map<Integer, Map<String, String>> rows = new HashMap<>();
        private int rowCount = 0;

        @Override
        public void setColumns(LinkedHashMap<String, String> columns) {
            this.columns = columns;
        }

        public LinkedHashMap<String, String> getColumns() {
            return columns;
        }

        @Override
        public void addRow(Map<String, String> row) {
            rows.put(rowCount++, row);
        }

        @Override
        public int getRowCount() {
            return rowCount;
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return (String) columns.keySet().toArray()[columnIndex];
        }

        @Override
        public String getValueAt(int rowIndex, String columnName) {
            return rows.get(rowIndex).get(columnName);
        }
        
        @Override
        public String inferType(String value) {
            if (value == null || value.isEmpty()) {
                return "string";
            }
            
            if (value.matches("-?\\d+")) {
                return "int";
            } else if (value.matches("-?\\d*\\.\\d+")) {
                return "double";
            } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                return "boolean";
            } else {
                return "string";
            }
        }
    }

    private static class MockFileConnection extends FileConnection {
        private boolean connected = false;
        private Path rawConnection;
        private Map<String, Object> properties = new HashMap<>();

        public MockFileConnection() {
            super("", "xml");
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void setRawConnection(Path rawConnection) {
            this.rawConnection = rawConnection;
        }

        @Override
        public boolean connect() {
            connected = true;
            return true;
        }

        @Override
        public void disconnect() {
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public String getConnectionType() {
            return "xml";
        }

        @Override
        public Object getRawConnection() {
            return rawConnection;
        }

        @Override
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getLocation() {
            return rawConnection != null ? rawConnection.toString() : "";
        }
    }
}