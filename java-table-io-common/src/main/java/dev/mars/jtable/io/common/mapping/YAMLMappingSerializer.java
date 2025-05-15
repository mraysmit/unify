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