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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for reading database connection properties from a properties file.
 * This class provides methods to get the connection parameters for both SQLite and H2 databases.
 */
public class DatabaseProperties {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseProperties.class);
    private static final String DEFAULT_PROPERTIES_FILE = "db.properties";
    private static Properties properties = null;

    /**
     * Loads the properties from the default properties file.
     * This method is called automatically when the class is first used.
     */
    private static void loadProperties() {
        loadProperties(DEFAULT_PROPERTIES_FILE);
    }

    /**
     * Loads the properties from the specified properties file.
     * 
     * @param propertiesFile the name of the properties file to load
     */
    private static void loadProperties(String propertiesFile) {
        if (properties == null) {
            properties = new Properties();
            try (InputStream input = DatabaseProperties.class.getClassLoader().getResourceAsStream(propertiesFile)) {
                if (input == null) {
                    logger.error("Unable to find {}", propertiesFile);
                    throw new IOException("Unable to find " + propertiesFile);
                }
                properties.load(input);
                logger.debug("Successfully loaded properties from {}", propertiesFile);
            } catch (IOException e) {
                logger.error("Error loading properties from {}: {}", propertiesFile, e.getMessage());
                throw new RuntimeException("Error loading properties from " + propertiesFile, e);
            }
        }
    }

    /**
     * Loads the properties from the specified properties file.
     * This method can be called to explicitly load properties from a specific file.
     * 
     * @param propertiesFile the name of the properties file to load
     * @throws IOException if the properties file cannot be loaded
     */
    public static void loadPropertiesFromFile(String propertiesFile) throws IOException {
        // Reset properties to force reload
        properties = null;
        loadProperties(propertiesFile);
    }

    /**
     * Gets the SQLite connection string from the default properties file.
     *
     * @return the SQLite connection string
     */
    public static String getSqliteConnectionString() {
        loadProperties();
        return properties.getProperty("sqlite.connectionString");
    }

    /**
     * Gets the SQLite connection string from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the SQLite connection string
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getSqliteConnectionString(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("sqlite.connectionString");
    }

    /**
     * Gets the SQLite username from the default properties file.
     *
     * @return the SQLite username
     */
    public static String getSqliteUsername() {
        loadProperties();
        return properties.getProperty("sqlite.username");
    }

    /**
     * Gets the SQLite username from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the SQLite username
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getSqliteUsername(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("sqlite.username");
    }

    /**
     * Gets the SQLite password from the default properties file.
     *
     * @return the SQLite password
     */
    public static String getSqlitePassword() {
        loadProperties();
        return properties.getProperty("sqlite.password");
    }

    /**
     * Gets the SQLite password from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the SQLite password
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getSqlitePassword(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("sqlite.password");
    }

    /**
     * Gets the H2 connection string from the default properties file.
     *
     * @return the H2 connection string
     */
    public static String getH2ConnectionString() {
        loadProperties();
        return properties.getProperty("h2.connectionString");
    }

    /**
     * Gets the H2 connection string from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the H2 connection string
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getH2ConnectionString(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("h2.connectionString");
    }

    /**
     * Gets the H2 username from the default properties file.
     *
     * @return the H2 username
     */
    public static String getH2Username() {
        loadProperties();
        return properties.getProperty("h2.username");
    }

    /**
     * Gets the H2 username from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the H2 username
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getH2Username(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("h2.username");
    }

    /**
     * Gets the H2 password from the default properties file.
     *
     * @return the H2 password
     */
    public static String getH2Password() {
        loadProperties();
        return properties.getProperty("h2.password");
    }

    /**
     * Gets the H2 password from the specified properties file.
     *
     * @param propertiesFile the name of the properties file to load
     * @return the H2 password
     * @throws IOException if the properties file cannot be loaded
     */
    public static String getH2Password(String propertiesFile) throws IOException {
        loadPropertiesFromFile(propertiesFile);
        return properties.getProperty("h2.password");
    }
}
