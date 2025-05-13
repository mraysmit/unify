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
    private static final String PROPERTIES_FILE = "db.properties";
    private static Properties properties = null;

    /**
     * Loads the properties from the properties file.
     * This method is called automatically when the class is first used.
     */
    private static void loadProperties() {
        if (properties == null) {
            properties = new Properties();
            try (InputStream input = DatabaseProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
                if (input == null) {
                    logger.error("Unable to find {}", PROPERTIES_FILE);
                    throw new IOException("Unable to find " + PROPERTIES_FILE);
                }
                properties.load(input);
                logger.debug("Successfully loaded properties from {}", PROPERTIES_FILE);
            } catch (IOException e) {
                logger.error("Error loading properties from {}: {}", PROPERTIES_FILE, e.getMessage());
                throw new RuntimeException("Error loading properties from " + PROPERTIES_FILE, e);
            }
        }
    }

    /**
     * Gets the SQLite connection string.
     *
     * @return the SQLite connection string
     */
    public static String getSqliteConnectionString() {
        loadProperties();
        return properties.getProperty("sqlite.connectionString");
    }

    /**
     * Gets the SQLite username.
     *
     * @return the SQLite username
     */
    public static String getSqliteUsername() {
        loadProperties();
        return properties.getProperty("sqlite.username");
    }

    /**
     * Gets the SQLite password.
     *
     * @return the SQLite password
     */
    public static String getSqlitePassword() {
        loadProperties();
        return properties.getProperty("sqlite.password");
    }

    /**
     * Gets the H2 connection string.
     *
     * @return the H2 connection string
     */
    public static String getH2ConnectionString() {
        loadProperties();
        return properties.getProperty("h2.connectionString");
    }

    /**
     * Gets the H2 username.
     *
     * @return the H2 username
     */
    public static String getH2Username() {
        loadProperties();
        return properties.getProperty("h2.username");
    }

    /**
     * Gets the H2 password.
     *
     * @return the H2 password
     */
    public static String getH2Password() {
        loadProperties();
        return properties.getProperty("h2.password");
    }
}