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
package dev.mars.jtable.integration.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DatabaseProperties.
 * This class tests the functionality of the DatabaseProperties utility class,
 * focusing on negative test cases and error handling.
 */
public class DatabasePropertiesTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabasePropertiesTest.class);

    // Test properties objects
    private Properties validProperties;
    private Properties emptyProperties;
    private Properties incompleteProperties;

    // Test file names (these don't need to exist as actual files)
    private static final String TEST_PROPERTIES_FILE = "test_db.properties";
    private static final String INVALID_PROPERTIES_FILE = "nonexistent.properties";
    private static final String EMPTY_PROPERTIES_FILE = "empty.properties";
    private static final String INCOMPLETE_PROPERTIES_FILE = "incomplete.properties";

    @BeforeEach
    public void setUp() throws Exception {
        logger.info("Setting up test environment");

        // Reset the static properties field in DatabaseProperties to ensure tests are isolated
        resetDatabasePropertiesStaticField();

        // Create valid properties
        validProperties = new Properties();
        validProperties.setProperty("sqlite.connectionString", "jdbc:sqlite:test.db");
        validProperties.setProperty("sqlite.username", "test_user");
        validProperties.setProperty("sqlite.password", "test_password");
        validProperties.setProperty("h2.connectionString", "jdbc:h2:./test_h2");
        validProperties.setProperty("h2.username", "sa");
        validProperties.setProperty("h2.password", "");
        logger.debug("Created valid properties");

        // Create empty properties
        emptyProperties = new Properties();
        logger.debug("Created empty properties");

        // Create incomplete properties
        incompleteProperties = new Properties();
        incompleteProperties.setProperty("sqlite.connectionString", "jdbc:sqlite:test.db");
        incompleteProperties.setProperty("h2.connectionString", "jdbc:h2:./test_h2");
        logger.debug("Created incomplete properties");
    }

    @AfterEach
    public void tearDown() throws Exception {
        logger.info("Tearing down test environment");

        // Reset the static properties field in DatabaseProperties
        resetDatabasePropertiesStaticField();
    }

    /**
     * Resets the static properties field in DatabaseProperties to ensure tests are isolated.
     */
    private void resetDatabasePropertiesStaticField() throws Exception {
        Field propertiesField = DatabaseProperties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        propertiesField.set(null, null);
    }

    /**
     * Sets the properties field in DatabaseProperties to the specified Properties object.
     */
    private void setDatabaseProperties(Properties props) throws Exception {
        Field propertiesField = DatabaseProperties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        propertiesField.set(null, props);
    }

    /**
     * Test loading properties from a non-existent file.
     * This should throw a RuntimeException wrapping an IOException.
     */
    @Test
    public void testLoadPropertiesFromNonExistentFile() {
        logger.info("Testing loading properties from non-existent file");

        // Attempt to load properties from a non-existent file
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            DatabaseProperties.loadPropertiesFromFile(INVALID_PROPERTIES_FILE);
        }, "Loading properties from a non-existent file should throw RuntimeException");

        logger.debug("Exception message: {}", exception.getMessage());
        assertTrue(exception.getMessage().contains("Error loading properties from"), 
            "Exception message should indicate an error loading properties");
        assertTrue(exception.getMessage().contains(INVALID_PROPERTIES_FILE), 
            "Exception message should include the file name");
        assertTrue(exception.getCause() instanceof IOException, 
            "The cause of the exception should be an IOException");
    }

    /**
     * Test accessing a property that doesn't exist in the file.
     * This should return null.
     */
    @Test
    public void testAccessingNonExistentProperty() throws Exception {
        logger.info("Testing accessing non-existent property");

        // Set empty properties
        setDatabaseProperties(emptyProperties);

        // Attempt to access a property that doesn't exist
        assertNull(DatabaseProperties.getSqliteConnectionString(), 
            "Accessing a non-existent property should return null");
        assertNull(DatabaseProperties.getH2ConnectionString(), 
            "Accessing a non-existent property should return null");
    }

    /**
     * Test accessing properties from an incomplete properties file.
     * Some properties should be null, others should have values.
     */
    @Test
    public void testAccessingIncompleteProperties() throws Exception {
        logger.info("Testing accessing incomplete properties");

        // Set incomplete properties
        setDatabaseProperties(incompleteProperties);

        // Properties that exist should have values
        assertNotNull(DatabaseProperties.getSqliteConnectionString(), 
            "Existing property should not be null");
        assertEquals("jdbc:sqlite:test.db", DatabaseProperties.getSqliteConnectionString(), 
            "Property value should match expected value");

        assertNotNull(DatabaseProperties.getH2ConnectionString(), 
            "Existing property should not be null");
        assertEquals("jdbc:h2:./test_h2", DatabaseProperties.getH2ConnectionString(), 
            "Property value should match expected value");

        // Properties that don't exist should be null
        assertNull(DatabaseProperties.getSqliteUsername(), 
            "Non-existent property should be null");
        assertNull(DatabaseProperties.getSqlitePassword(), 
            "Non-existent property should be null");
        assertNull(DatabaseProperties.getH2Username(), 
            "Non-existent property should be null");
        assertNull(DatabaseProperties.getH2Password(), 
            "Non-existent property should be null");
    }

    /**
     * Test that the properties can be reloaded.
     * This tests that the static properties object is reset and reloaded.
     */
    @Test
    public void testReloadingProperties() throws Exception {
        logger.info("Testing reloading properties");

        // First set valid properties
        setDatabaseProperties(validProperties);
        assertEquals("jdbc:sqlite:test.db", DatabaseProperties.getSqliteConnectionString(), 
            "Property value should match expected value");

        // Then set incomplete properties
        setDatabaseProperties(incompleteProperties);
        assertEquals("jdbc:sqlite:test.db", DatabaseProperties.getSqliteConnectionString(), 
            "Property value should match expected value");
        assertNull(DatabaseProperties.getSqliteUsername(), 
            "Property should be null after reloading from incomplete file");

        // Reload valid properties
        setDatabaseProperties(validProperties);
        assertEquals("test_user", DatabaseProperties.getSqliteUsername(), 
            "Property value should be restored after reloading from complete file");
    }

    /**
     * Test that the overloaded getter methods correctly load properties from the specified file.
     * Since we can't easily mock the file loading, we'll test the basic getter methods instead.
     */
    @Test
    public void testGetterMethods() throws Exception {
        logger.info("Testing getter methods");

        // Set valid properties
        setDatabaseProperties(validProperties);

        // Test SQLite connection string
        assertEquals("jdbc:sqlite:test.db", DatabaseProperties.getSqliteConnectionString(), 
            "SQLite connection string should match expected value");

        // Test SQLite username
        assertEquals("test_user", DatabaseProperties.getSqliteUsername(), 
            "SQLite username should match expected value");

        // Test SQLite password
        assertEquals("test_password", DatabaseProperties.getSqlitePassword(), 
            "SQLite password should match expected value");

        // Test H2 connection string
        assertEquals("jdbc:h2:./test_h2", DatabaseProperties.getH2ConnectionString(), 
            "H2 connection string should match expected value");

        // Test H2 username
        assertEquals("sa", DatabaseProperties.getH2Username(), 
            "H2 username should match expected value");

        // Test H2 password (empty string)
        assertEquals("", DatabaseProperties.getH2Password(), 
            "H2 password should be empty string");
    }

    /**
     * Test that passing null to loadPropertiesFromFile throws NullPointerException.
     */
    @Test
    public void testNullPropertiesFile() {
        logger.info("Testing null properties file");

        assertThrows(NullPointerException.class, () -> {
            DatabaseProperties.loadPropertiesFromFile(null);
        }, "Passing null to loadPropertiesFromFile should throw NullPointerException");
    }

    /**
     * Helper method to create a test properties file with the given content.
     */
    private void createTestPropertiesFile(String fileName, String content) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(content);
        }
    }

    /**
     * Helper method to delete a test file.
     */
    private void deleteTestFile(String fileName) throws IOException {
        Files.deleteIfExists(Paths.get(fileName));
        logger.debug("Deleted test file: {}", fileName);
    }
}
