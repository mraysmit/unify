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
 * Exception thrown when there are issues with XML data processing.
 * This includes parsing errors, format issues, and I/O problems specific to XML files.
 */
public class XMLException extends DataSourceException {
    
    private final int lineNumber;
    private final int columnNumber;
    
    /**
     * Creates a new XMLException with the specified message.
     *
     * @param message the detail message
     */
    public XMLException(String message) {
        super(message);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }
    
    /**
     * Creates a new XMLException with the specified message and XML file path.
     *
     * @param message the detail message
     * @param xmlFilePath the path to the XML file
     */
    public XMLException(String message, String xmlFilePath) {
        super(message, xmlFilePath);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }
    
    /**
     * Creates a new XMLException with the specified message, XML file path, and position.
     *
     * @param message the detail message
     * @param xmlFilePath the path to the XML file
     * @param lineNumber the line number where the error occurred
     * @param columnNumber the column number where the error occurred
     */
    public XMLException(String message, String xmlFilePath, int lineNumber, int columnNumber) {
        super(message + (lineNumber > 0 ? " (Line: " + lineNumber + 
              (columnNumber > 0 ? ", Column: " + columnNumber : "") + ")" : ""), xmlFilePath);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }
    
    /**
     * Creates a new XMLException with the specified message, XML file path, position, and cause.
     *
     * @param message the detail message
     * @param xmlFilePath the path to the XML file
     * @param lineNumber the line number where the error occurred
     * @param columnNumber the column number where the error occurred
     * @param cause the cause of this exception
     */
    public XMLException(String message, String xmlFilePath, int lineNumber, int columnNumber, Throwable cause) {
        super(message + (lineNumber > 0 ? " (Line: " + lineNumber + 
              (columnNumber > 0 ? ", Column: " + columnNumber : "") + ")" : ""), xmlFilePath, cause);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }
    
    /**
     * Gets the line number where the error occurred.
     *
     * @return the line number, or -1 if not specified
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the column number where the error occurred.
     *
     * @return the column number, or -1 if not specified
     */
    public int getColumnNumber() {
        return columnNumber;
    }
}
