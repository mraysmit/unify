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
package dev.mars.jtable.io.files.json;

import dev.mars.jtable.io.common.datasource.IDataReader;
import dev.mars.jtable.io.common.datasource.IJSONDataSource;

/**
 * Interface for reading data from JSON files.
 * This interface extends the generic IDataReader interface and adds JSON-specific methods.
 */
public interface IJSONReader extends IDataReader {
    /**
     * Reads data from a JSON file into a data source.
     *
     * @param dataSource the data source to read into
     * @param fileName the name of the file to read from
     * @param rootElement the name of the root element in the JSON file (optional)
     */
    void readFromJSON(IJSONDataSource dataSource, String fileName, String rootElement);
}