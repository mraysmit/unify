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

/**
 * Exception thrown when there are issues with CSV data processing.
 * This includes parsing errors, format issues, and I/O problems specific to CSV files.
 */
public class CSVException extends DataSourceException {
    
    private final int lineNumber;
    
    /**
     * Creates a new CSVException with the specified message.
     *
     * @param message the detail message
     */
    public CSVException(String message) {
        super(message);
        this.lineNumber = -1;
    }
    
    /**
     * Creates a new CSVException with the specified message and CSV file path.
     *
     * @param message the detail message
     * @param csvFilePath the path to the CSV file
     */
    public CSVException(String message, String csvFilePath) {
        super(message, csvFilePath);
        this.lineNumber = -1;
    }
    
    /**
     * Creates a new CSVException with the specified message, CSV file path, and line number.
     *
     * @param message the detail message
     * @param csvFilePath the path to the CSV file
     * @param lineNumber the line number where the error occurred
     */
    public CSVException(String message, String csvFilePath, int lineNumber) {
        super(message + (lineNumber > 0 ? " (Line: " + lineNumber + ")" : ""), csvFilePath);
        this.lineNumber = lineNumber;
    }
    
    /**
     * Creates a new CSVException with the specified message, CSV file path, line number, and cause.
     *
     * @param message the detail message
     * @param csvFilePath the path to the CSV file
     * @param lineNumber the line number where the error occurred
     * @param cause the cause of this exception
     */
    public CSVException(String message, String csvFilePath, int lineNumber, Throwable cause) {
        super(message + (lineNumber > 0 ? " (Line: " + lineNumber + ")" : ""), csvFilePath, cause);
        this.lineNumber = lineNumber;
    }
    
    /**
     * Gets the line number where the error occurred.
     *
     * @return the line number, or -1 if not specified
     */
    public int getLineNumber() {
        return lineNumber;
    }
}
