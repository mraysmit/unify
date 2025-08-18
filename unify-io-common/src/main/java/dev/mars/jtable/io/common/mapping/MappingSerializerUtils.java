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

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for mapping serializers.
 * This class provides utility methods for working with mapping serializers.
 */
public class MappingSerializerUtils {
    
    /**
     * The default directory for storing mapping configurations.
     */
    private static final String DEFAULT_MAPPINGS_DIR = "mappings";
    
    /**
     * Gets the default directory for storing mapping configurations.
     * 
     * @return the default directory for storing mapping configurations
     */
    public static String getDefaultMappingsDirectory() {
        return DEFAULT_MAPPINGS_DIR;
    }
    
    /**
     * Gets the path to the default directory for storing mapping configurations.
     * This method first checks if the directory exists in the resources folder.
     * If not, it creates the directory in the current working directory.
     * 
     * @return the path to the default directory for storing mapping configurations
     */
    public static Path getDefaultMappingsPath() {
        // First, try to get the path from the resources folder
        Path resourcesPath = Paths.get(ClassLoader.getSystemResource("").getPath());
        Path mappingsPath = resourcesPath.resolve(DEFAULT_MAPPINGS_DIR);
        
        // If the directory doesn't exist in the resources folder, use the current working directory
        if (!mappingsPath.toFile().exists()) {
            mappingsPath = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_MAPPINGS_DIR);
        }
        
        return mappingsPath;
    }
    
    /**
     * Gets the path to a mapping configuration file in the default directory.
     * 
     * @param fileName the name of the mapping configuration file
     * @return the path to the mapping configuration file
     */
    public static String getMappingFilePath(String fileName) {
        return getDefaultMappingsPath().resolve(fileName).toString();
    }
}