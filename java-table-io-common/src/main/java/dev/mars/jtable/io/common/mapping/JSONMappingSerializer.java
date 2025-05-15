package dev.mars.jtable.io.common.mapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;

/**
 * JSON implementation of the mapping serializer.
 * This class serializes and deserializes mapping configurations to and from JSON format.
 */
public class JSONMappingSerializer extends AbstractMappingSerializer {
    
    private final ObjectMapper objectMapper;
    
    /**
     * Creates a new JSONMappingSerializer with default settings.
     */
    public JSONMappingSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    /**
     * Creates a new JSONMappingSerializer with a custom ObjectMapper.
     * 
     * @param objectMapper the ObjectMapper to use
     */
    public JSONMappingSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormat() {
        return "json";
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