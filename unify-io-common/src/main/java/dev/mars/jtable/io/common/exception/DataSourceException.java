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
package dev.mars.jtable.io.common.exception;

import dev.mars.jtable.core.exception.TableException;

/**
 * Base exception class for all data source related exceptions.
 * This includes CSV, JSON, XML, JDBC, and REST data source errors.
 */
public class DataSourceException extends TableException {
    
    private final String dataSourceLocation;
    
    /**
     * Creates a new DataSourceException with the specified message.
     *
     * @param message the detail message
     */
    public DataSourceException(String message) {
        super(message);
        this.dataSourceLocation = null;
    }
    
    /**
     * Creates a new DataSourceException with the specified message and data source location.
     *
     * @param message the detail message
     * @param dataSourceLocation the location of the data source (file path, URL, etc.)
     */
    public DataSourceException(String message, String dataSourceLocation) {
        super(message + (dataSourceLocation != null ? " (Source: " + dataSourceLocation + ")" : ""));
        this.dataSourceLocation = dataSourceLocation;
    }
    
    /**
     * Creates a new DataSourceException with the specified message, data source location, and cause.
     *
     * @param message the detail message
     * @param dataSourceLocation the location of the data source
     * @param cause the cause of this exception
     */
    public DataSourceException(String message, String dataSourceLocation, Throwable cause) {
        super(message + (dataSourceLocation != null ? " (Source: " + dataSourceLocation + ")" : ""), cause);
        this.dataSourceLocation = dataSourceLocation;
    }
    
    /**
     * Gets the location of the data source that caused this exception.
     *
     * @return the data source location, or null if not specified
     */
    public String getDataSourceLocation() {
        return dataSourceLocation;
    }
}
