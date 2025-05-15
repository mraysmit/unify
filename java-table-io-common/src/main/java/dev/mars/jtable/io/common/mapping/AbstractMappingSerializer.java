package dev.mars.jtable.io.common.mapping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Abstract base class for mapping serializers.
 * This class provides common functionality for serializing and deserializing mapping configurations.
 */
public abstract class AbstractMappingSerializer implements IMappingSerializer {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void write(MappingConfiguration config, OutputStream outputStream) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }
        if (outputStream == null) {
            throw new IllegalArgumentException("Output stream cannot be null");
        }
        
        String serialized = serialize(config);
        outputStream.write(serialized.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration read(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null");
        }
        
        byte[] bytes = inputStream.readAllBytes();
        String content = new String(bytes, StandardCharsets.UTF_8);
        return deserialize(content);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void writeToFile(MappingConfiguration config, String filePath) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("Mapping configuration cannot be null");
        }
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        try (FileOutputStream outputStream = new FileOutputStream(new File(filePath))) {
            write(config, outputStream);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public MappingConfiguration readFromFile(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("File does not exist: " + filePath);
        }
        if (!file.isFile()) {
            throw new IOException("Path is not a file: " + filePath);
        }
        
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return read(inputStream);
        }
    }
}