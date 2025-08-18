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
package dev.mars.jtable.io.common.mapping;

import dev.mars.jtable.io.common.datasource.DbConnection;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating mapping serializers.
 * This class provides methods for creating serializers based on format or file extension.
 */
public class MappingSerializerFactory {
    
    /**
     * Creates a serializer for the specified format.
     * 
     * @param format the format to create a serializer for (e.g., "json", "yaml", "jdbc")
     * @return a serializer for the specified format
     * @throws IllegalArgumentException if the format is not supported
     */
    public static IMappingSerializer createSerializer(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Format cannot be null or empty");
        }
        
        format = format.toLowerCase();
        
        switch (format) {
            case "json":
                return new JSONMappingSerializer();
            case "yaml":
            case "yml":
                return new YAMLMappingSerializer();
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
    
    /**
     * Creates a serializer for the specified file path based on its extension.
     * 
     * @param filePath the file path to create a serializer for
     * @return a serializer for the specified file path
     * @throws IllegalArgumentException if the file extension is not supported
     */
    public static IMappingSerializer createSerializerForFile(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        Path path = Paths.get(filePath);
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("File has no extension: " + filePath);
        }
        
        String extension = fileName.substring(dotIndex + 1).toLowerCase();
        
        switch (extension) {
            case "json":
                return new JSONMappingSerializer();
            case "yaml":
            case "yml":
                return new YAMLMappingSerializer();
            default:
                throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }
    }
    
    /**
     * Creates a JDBC serializer with the specified connection parameters.
     * 
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     * @param tableName the name of the table to store mapping configurations in
     * @return a JDBC serializer
     */
    public static JDBCMappingSerializer createJDBCSerializer(String connectionString, String username, String password, String tableName) {
        return new JDBCMappingSerializer(connectionString, username, password, tableName);
    }
    
    /**
     * Creates a JDBC serializer with the specified connection parameters and the default table name.
     * 
     * @param connectionString the JDBC connection string
     * @param username the database username
     * @param password the database password
     * @return a JDBC serializer
     */
    public static JDBCMappingSerializer createJDBCSerializer(String connectionString, String username, String password) {
        return new JDBCMappingSerializer(connectionString, username, password);
    }

    /**
     * Creates a JDBC serializer with the specified DbConnection.
     *
     * @param dbConnection the DbConnection to use
     * @return a JDBC serializer
     */
    public static JDBCMappingSerializer createJDBCSerializer(DbConnection dbConnection) {
        return new JDBCMappingSerializer(dbConnection);
    }
}