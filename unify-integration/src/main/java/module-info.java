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

/**
 * Integration module for the Unify data integration framework.
 * 
 * This module provides comprehensive integration examples, demonstrations,
 * and utilities for working with the Unify framework. It showcases real-world
 * usage patterns and provides end-to-end examples of data integration workflows.
 * 
 * Key features:
 * - CSV to database integration examples (SQLite, H2)
 * - Database connection management utilities
 * - Mapping configuration management
 * - Performance benchmarking utilities
 * - Comprehensive integration testing
 * 
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 1.0
 */
module dev.mars.jtable.integration {
    // Core Unify framework dependencies
    requires dev.mars.jtable.core;
    requires dev.mars.jtable.io.common;
    requires dev.mars.jtable.io.files;
    
    // Java standard library modules
    requires java.sql;
    requires java.base;
    
    // Logging framework
    requires org.slf4j;
    requires ch.qos.logback.classic;
    
    // Database drivers (automatic modules)
    requires com.h2database;
    requires java.sql.rowset;
    
    // Export integration packages for use by other modules
    exports dev.mars.jtable.integration;
    exports dev.mars.jtable.integration.config;
    exports dev.mars.jtable.integration.csv;
    exports dev.mars.jtable.integration.db;
    exports dev.mars.jtable.integration.utils;
    exports dev.mars.jtable.integration.performance;
    exports dev.mars.jtable.integration.async;
}
