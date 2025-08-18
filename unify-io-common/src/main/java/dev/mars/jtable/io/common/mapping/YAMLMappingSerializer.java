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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;

/**
 * YAML implementation of the mapping serializer.
 * This class serializes and deserializes mapping configurations to and from YAML format.
 */
public class YAMLMappingSerializer extends AbstractMappingSerializer {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new YAMLMappingSerializer with default settings.
     */
    public YAMLMappingSerializer() {
        this.objectMapper = new ObjectMapper(new YAMLFactory());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Creates a new YAMLMappingSerializer with a custom ObjectMapper.
     * 
     * @param objectMapper the ObjectMapper to use
     */
    public YAMLMappingSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormat() {
        return "yaml";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(MappingConfiguration config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }
        
        return objectMapper.writeValueAsString(config);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration deserialize(String content) throws IOException {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        
        return objectMapper.readValue(content, MappingConfiguration.class);
    }
}