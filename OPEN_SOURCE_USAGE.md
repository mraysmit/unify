# Open Source Usage and License Compliance Guide

## Overview

Java Table is an open source project licensed under the **Apache License 2.0**. This document outlines the open source components used, license requirements, and compliance guidelines.

## Project License

**License:** Apache License 2.0
**Copyright:** 2025 Mark Andrew Ray-Smith Cityline Ltd
**License File:** [LICENSE](./LICENSE)
**Attribution File:** [NOTICE.md](./NOTICE.md)

### Apache License 2.0 Summary

**Permissions:**
- Commercial use
- Modification
- Distribution
- Patent use
- Private use

**Conditions:**
- License and copyright notice
- State changes
- Include NOTICE file

**Limitations:**
- Trademark use
- Liability
- Warranty

## Required License Headers

All Java source files must include the following license header:

```java
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
```

## Third-Party Dependencies

### Runtime Dependencies

#### JSON Processing
- **Jackson Databind** (2.13.0) - Apache License 2.0
- **Jackson Dataformat YAML** (2.15.2) - Apache License 2.0

#### Database Drivers
- **H2 Database Engine** (2.2.224) - Mozilla Public License 2.0 / Eclipse Public License 1.0
- **SQLite JDBC Driver** (3.42.0.0) - Apache License 2.0

#### Logging
- **SLF4J API** (2.0.9) - MIT License
- **Logback Classic** (1.4.11) - Eclipse Public License 1.0 / GNU Lesser General Public License 2.1

### Test Dependencies

#### Testing Frameworks
- **JUnit Jupiter** (5.8.1) - Eclipse Public License 2.0
- **QuickTheories** (0.26) - Apache License 2.0

#### Mock Testing
- **WireMock JRE8** (2.35.0) - Apache License 2.0
- **MockWebServer** (4.10.0) - Apache License 2.0

### Build Tools

#### Code Coverage
- **JaCoCo Maven Plugin** (0.8.12) - Eclipse Public License 2.0

## License Compatibility Matrix

| License | Compatible with Apache 2.0 | Notes |
|---------|----------------------------|-------|
| Apache 2.0 | Yes | Same license |
| MIT | Yes | Permissive, compatible |
| Eclipse Public License 1.0/2.0 | Yes | Compatible with Apache 2.0 |
| Mozilla Public License 2.0 | Yes | Compatible with Apache 2.0 |
| GNU LGPL 2.1 | Conditional | Dynamic linking only |

## Compliance Requirements

### For Distribution

1. **Include License File:** Copy of Apache License 2.0
2. **Include NOTICE File:** Attribution notices for all dependencies
3. **Preserve Copyright Notices:** Keep all existing copyright headers
4. **Document Changes:** If you modify the code, document the changes

### For Commercial Use

**Allowed:**
- Use in commercial products
- Sell products containing PeeGeeQ
- Modify for commercial purposes
- Create proprietary derivatives

**Required:**
- Include license and copyright notices
- Include NOTICE file in distributions
- Don't use "Java Table" trademark without permission

### For Modification

**Allowed:**
- Modify source code
- Create derivative works
- Distribute modifications

**Required:**
- Mark modified files with change notices
- Include original license headers
- Include NOTICE file

## Attribution Requirements

When using Java Table in your project, include:

### In Documentation
```
This product includes Java Table (https://github.com/your-repo/java-table)
Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
Licensed under the Apache License 2.0
```

### In Software
- Include the NOTICE file in your distribution
- Preserve all copyright headers in source code
- Include Apache License 2.0 text

## Automated Compliance

### Header Management Script

Use the provided script to ensure all files have proper headers:

```powershell
# Check current status
.\update-java-headers.ps1 -DryRun

# Update headers with license information
.\update-java-headers.ps1
```

### Maven License Plugin

Consider adding the Maven License Plugin to your build:

```xml
<plugin>
    <groupId>com.mycila</groupId>
    <artifactId>license-maven-plugin</artifactId>
    <version>4.2</version>
    <configuration>
        <header>LICENSE-HEADER.txt</header>
        <includes>
            <include>**/*.java</include>
        </includes>
    </configuration>
</plugin>
```

## Frequently Asked Questions

### Q: Can I use Java Table in my commercial product?
**A:** Yes, the Apache License 2.0 explicitly allows commercial use.

### Q: Do I need to open source my modifications?
**A:** No, Apache License 2.0 does not require derivative works to be open source.

### Q: Can I remove the license headers?
**A:** No, you must preserve all copyright and license notices.

### Q: Do I need to contribute back my changes?
**A:** No, but contributions are welcome and appreciated.

### Q: Can I use the "Java Table" name for my product?
**A:** The license doesn't grant trademark rights. Contact the copyright holder for trademark usage.

## Implementation Status

**Complete Implementation:**
- Apache License 2.0 headers required for all Java files
- LICENSE file with full Apache License 2.0 text
- NOTICE.md file with comprehensive third-party attribution
- POM.xml files with proper license metadata
- Module system with proper exports and dependencies
- Comprehensive exception hierarchy for better error handling
- Performance optimizations and profiling capabilities

## Recent Architectural Improvements (2025)

### Phase 1: Critical Architecture Fixes
The Java Table project has undergone significant architectural improvements:

**Completed:**
- **Eliminated Table/TableCore wrapper pattern** - Removed unnecessary indirection that provided no value
- **Implemented comprehensive exception hierarchy** - Domain-specific exceptions with rich context information
- **Enhanced module system** - Proper package exports and dependency management

**Exception Hierarchy:**
- `TableException` - Base exception for all table operations
- `ColumnException` - Column-specific errors with column name context
- `RowException` - Row-specific errors with row index context
- `CellException` - Cell-specific errors with precise location context
- `DataTypeException` - Type conversion and validation errors
- `DataSourceException` - Base for all I/O related exceptions
- `CSVException`, `DatabaseException`, `XMLException`, `MappingException` - Format-specific errors

**Benefits:**
- Better error handling with meaningful exception types
- Improved debugging with rich context information
- Reduced code duplication and maintenance overhead
- Enhanced type safety and compile-time checking

## Contact

For license questions or trademark permissions:
- **Copyright Holder:** Mark Andrew Ray-Smith Cityline Ltd
- **Project Repository:** [Java Table Repository]
- **License Questions:** [Contact Information]

## Resources

- [Apache License 2.0 Full Text](https://www.apache.org/licenses/LICENSE-2.0)
- [Apache License FAQ](https://www.apache.org/foundation/license-faq.html)
- [Open Source Initiative](https://opensource.org/licenses/Apache-2.0)
- [SPDX License Identifier](https://spdx.org/licenses/Apache-2.0.html)

---

**Note:** This document provides general guidance. For specific legal questions, consult with a qualified attorney familiar with open source licensing.
