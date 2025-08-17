# NOTICE

Java Table Project
Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd

This product includes software developed by Mark Andrew Ray-Smith Cityline Ltd.

## Third-Party Software

This software contains third-party software components that are subject to their own license terms:

### Runtime Dependencies

#### JSON Processing
- **Jackson Databind** (2.13.0)
  - Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
  - Licensed under the Apache License 2.0
  - https://github.com/FasterXML/jackson-databind

- **Jackson Dataformat YAML** (2.15.2)
  - Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
  - Licensed under the Apache License 2.0
  - https://github.com/FasterXML/jackson-dataformats-text

#### Database Drivers
- **H2 Database Engine** (2.2.224)
  - Copyright (c) 2004-2023 H2 Group
  - Licensed under the Mozilla Public License 2.0 or Eclipse Public License 1.0
  - https://h2database.com/

- **SQLite JDBC Driver** (3.42.0.0)
  - Copyright (c) 2007 David Crawshaw <david@zentus.com>
  - Licensed under the Apache License 2.0
  - https://github.com/xerial/sqlite-jdbc

#### Logging
- **SLF4J API** (2.0.9)
  - Copyright (c) 2004-2023 QOS.ch
  - Licensed under the MIT License
  - https://www.slf4j.org/

- **Logback Classic** (1.4.11)
  - Copyright (c) 1999-2023 QOS.ch
  - Licensed under the Eclipse Public License 1.0 and GNU Lesser General Public License 2.1
  - https://logback.qos.ch/

### Test Dependencies

#### Testing Frameworks
- **JUnit Jupiter** (5.8.1)
  - Copyright (c) 2015-2023 the original author or authors
  - Licensed under the Eclipse Public License 2.0
  - https://junit.org/junit5/

- **QuickTheories** (0.26)
  - Copyright (c) 2015-2023 NCR Corporation
  - Licensed under the Apache License 2.0
  - https://github.com/quicktheories/QuickTheories

#### Mock Testing
- **WireMock JRE8** (2.35.0)
  - Copyright (c) 2011-2023 Tom Akehurst
  - Licensed under the Apache License 2.0
  - https://wiremock.org/

- **MockWebServer** (4.10.0)
  - Copyright (c) 2019 Square, Inc.
  - Licensed under the Apache License 2.0
  - https://github.com/square/okhttp

### Build Tools

#### Code Coverage
- **JaCoCo Maven Plugin** (0.8.12)
  - Copyright (c) 2009, 2023 Mountainminds GmbH & Co. KG and Contributors
  - Licensed under the Eclipse Public License 2.0
  - https://www.jacoco.org/

## Architectural Improvements (2025)

This version includes significant architectural improvements:

### Phase 1: Critical Architecture Fixes
- **Eliminated Table/TableCore wrapper pattern** - Removed unnecessary indirection
- **Implemented comprehensive exception hierarchy** - Domain-specific exceptions with rich context
- **Enhanced module system** - Proper package exports and dependencies

### Exception Hierarchy
The project now includes a comprehensive exception hierarchy:
- `TableException` - Base exception for all table operations
- `ColumnException` - Column-specific errors with column name context
- `RowException` - Row-specific errors with row index context
- `CellException` - Cell-specific errors with precise location context
- `DataTypeException` - Type conversion and validation errors
- `DataSourceException` - Base for all I/O related exceptions
- `CSVException` - CSV parsing errors with line number context
- `DatabaseException` - Database operation errors with SQL context
- `XMLException` - XML parsing errors with position context
- `MappingException` - Column mapping configuration errors

### Performance Optimizations
- **Optimized collection usage** - LinkedHashMap for columns, ArrayList for rows
- **Concurrent collection support** - Thread-safe implementations available
- **Memory optimization** - Reduced object creation and improved caching
- **Profiling framework** - Built-in performance monitoring capabilities

## License Information

This project is licensed under the Apache License 2.0.
See the LICENSE file for the full license text.

For questions about licensing or third-party components, please contact:
Mark Andrew Ray-Smith Cityline Ltd

---

This NOTICE file is required to be included in all distributions of this software
as per the Apache License 2.0 requirements.
