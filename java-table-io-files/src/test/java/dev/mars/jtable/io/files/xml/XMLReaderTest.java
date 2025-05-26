package dev.mars.jtable.io.files.xml;

import dev.mars.jtable.io.common.datasource.FileConnection;
import dev.mars.jtable.io.common.datasource.IDataSourceConnection;
import dev.mars.jtable.io.common.datasource.IXMLDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class XMLReaderTest {

    private XMLReader xmlReader;
    private MockXMLDataSource dataSource;
    private MockFileConnection fileConnection;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        xmlReader = new XMLReader();
        dataSource = new MockXMLDataSource();
        fileConnection = new MockFileConnection();
    }

    @Test
    void testReadFromXML() throws Exception {
        // Create a test XML file
        File testFile = tempDir.resolve("test.xml").toFile();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<data>");
            writer.newLine();
            writer.write("  <row>");
            writer.newLine();
            writer.write("    <id>1</id>");
            writer.newLine();
            writer.write("    <name>Test Name</name>");
            writer.newLine();
            writer.write("    <active>true</active>");
            writer.newLine();
            writer.write("    <score>95.5</score>");
            writer.newLine();
            writer.write("  </row>");
            writer.newLine();
            writer.write("  <row>");
            writer.newLine();
            writer.write("    <id>2</id>");
            writer.newLine();
            writer.write("    <name>Another Name</name>");
            writer.newLine();
            writer.write("    <active>false</active>");
            writer.newLine();
            writer.write("    <score>82.3</score>");
            writer.newLine();
            writer.write("  </row>");
            writer.newLine();
            writer.write("</data>");
            writer.newLine();
        }
        
        // Test reading from the file
        xmlReader.readFromXML(dataSource, testFile.getAbsolutePath(), "data", "row");
        
        // Verify the data was read correctly
        assertEquals(2, dataSource.getRowCount());
        assertEquals(4, dataSource.getColumnCount());
        
        // Verify column types
        Map<String, String> columns = dataSource.getColumns();
        assertEquals("int", columns.get("id"));
        assertEquals("string", columns.get("name"));
        assertEquals("boolean", columns.get("active"));
        assertEquals("double", columns.get("score"));
        
        // Verify row values
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test Name", dataSource.getValueAt(0, "name"));
        assertEquals("true", dataSource.getValueAt(0, "active"));
        assertEquals("95.5", dataSource.getValueAt(0, "score"));
        
        assertEquals("2", dataSource.getValueAt(1, "id"));
        assertEquals("Another Name", dataSource.getValueAt(1, "name"));
        assertEquals("false", dataSource.getValueAt(1, "active"));
        assertEquals("82.3", dataSource.getValueAt(1, "score"));
    }

    @Test
    void testReadData() throws Exception {
        // Create a test XML file
        File testFile = tempDir.resolve("test_data.xml").toFile();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<data>");
            writer.newLine();
            writer.write("  <row>");
            writer.newLine();
            writer.write("    <id>1</id>");
            writer.newLine();
            writer.write("    <name>Test Name</name>");
            writer.newLine();
            writer.write("  </row>");
            writer.newLine();
            writer.write("</data>");
            writer.newLine();
        }
        
        // Set up the file connection
        fileConnection.setRawConnection(testFile.toPath());
        fileConnection.setConnected(true);
        
        // Test reading from the file using readData
        Map<String, Object> options = new HashMap<>();
        options.put("rootElement", "data");
        options.put("rowElement", "row");
        xmlReader.readData(dataSource, fileConnection, options);
        
        // Verify the data was read correctly
        assertEquals(1, dataSource.getRowCount());
        assertEquals(2, dataSource.getColumnCount());
        
        // Verify row values
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test Name", dataSource.getValueAt(0, "name"));
    }

    @Test
    void testReadXMLWithEscapedCharacters() throws Exception {
        // Create a test XML file with escaped characters
        File testFile = tempDir.resolve("test_escaped.xml").toFile();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.newLine();
            writer.write("<data>");
            writer.newLine();
            writer.write("  <row>");
            writer.newLine();
            writer.write("    <id>1</id>");
            writer.newLine();
            writer.write("    <name>Test &amp; Name</name>");
            writer.newLine();
            writer.write("    <description>This is a &lt;test&gt; with &quot;quotes&quot;</description>");
            writer.newLine();
            writer.write("  </row>");
            writer.newLine();
            writer.write("</data>");
            writer.newLine();
        }
        
        // Test reading from the file
        xmlReader.readFromXML(dataSource, testFile.getAbsolutePath(), "data", "row");
        
        // Verify the data was read correctly
        assertEquals(1, dataSource.getRowCount());
        assertEquals(3, dataSource.getColumnCount());
        
        // Verify row values with unescaped characters
        assertEquals("1", dataSource.getValueAt(0, "id"));
        assertEquals("Test & Name", dataSource.getValueAt(0, "name"));
        assertEquals("This is a <test> with \"quotes\"", dataSource.getValueAt(0, "description"));
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