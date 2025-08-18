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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface for serializing and deserializing mapping configurations.
 * This interface defines methods for reading and writing mapping configurations
 * to and from different formats (JSON, YAML, etc.) and data sources (files, JDBC, etc.).
 */
public interface IMappingSerializer {
    
    /**
     * Gets the format supported by this serializer (e.g., "json", "yaml", "jdbc").
     * 
     * @return the format name
     */
    String getFormat();
    
    /**
     * Serializes a mapping configuration to a string.
     * 
     * @param config the mapping configuration to serialize
     * @return the serialized string
     * @throws IOException if there is an error during serialization
     */
    String serialize(MappingConfiguration config) throws IOException;
    
    /**
     * Deserializes a mapping configuration from a string.
     * 
     * @param content the string to deserialize
     * @return the deserialized mapping configuration
     * @throws IOException if there is an error during deserialization
     */
    MappingConfiguration deserialize(String content) throws IOException;
    
    /**
     * Writes a mapping configuration to an output stream.
     * 
     * @param config the mapping configuration to write
     * @param outputStream the output stream to write to
     * @throws IOException if there is an error writing to the output stream
     */
    void write(MappingConfiguration config, OutputStream outputStream) throws IOException;
    
    /**
     * Reads a mapping configuration from an input stream.
     * 
     * @param inputStream the input stream to read from
     * @return the mapping configuration read from the input stream
     * @throws IOException if there is an error reading from the input stream
     */
    MappingConfiguration read(InputStream inputStream) throws IOException;
    
    /**
     * Writes a mapping configuration to a file.
     * 
     * @param config the mapping configuration to write
     * @param filePath the path to the file to write to
     * @throws IOException if there is an error writing to the file
     */
    void writeToFile(MappingConfiguration config, String filePath) throws IOException;
    
    /**
     * Reads a mapping configuration from a file.
     * 
     * @param filePath the path to the file to read from
     * @return the mapping configuration read from the file
     * @throws IOException if there is an error reading from the file
     */
    MappingConfiguration readFromFile(String filePath) throws IOException;
}