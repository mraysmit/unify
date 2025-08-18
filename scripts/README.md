# Java Header Management Scripts

This directory contains PowerShell scripts for managing Java file headers in the Unify project.

## Scripts

### `add-license-headers.ps1`
Adds Apache License 2.0 headers to Java files that don't have them.

**Usage:**
```powershell
# Add headers to files without them
./add-license-headers.ps1

# Dry run to see what would be changed
./add-license-headers.ps1 -DryRun

# Force update even if headers already exist
./add-license-headers.ps1 -Force

# Clean malformed headers while adding license headers
./add-license-headers.ps1 -CleanHeaders
```

**Features:**
- Detects existing license headers and skips files that already have them
- Can clean malformed headers (like "class: ClassName" patterns)
- Removes duplicate license headers
- Inserts headers before package declarations
- Supports dry run mode for testing

### `update-java-headers.ps1`
Cleans and standardizes Java file headers by removing malformed patterns and ensuring proper Apache license headers.

**Usage:**
```powershell
# Clean headers and ensure proper license headers
./update-java-headers.ps1

# Dry run to see what would be changed
./update-java-headers.ps1 -DryRun

# Force update all files regardless of current state
./update-java-headers.ps1 -Force
```

**Features:**
- Removes malformed JavaDoc headers with "class: ClassName" patterns
- Removes orphaned single-line comments with old file paths
- Removes duplicate Apache license headers
- Ensures clean, standardized Apache License 2.0 headers
- Provides detailed progress reporting

## What Gets Cleaned

The scripts identify and remove these problematic patterns:

1. **Malformed JavaDoc headers:**
   ```java
   /**
    * class: ClassName
    *
    * Project: Unify
    * Author: Mark Andrew Ray-Smith Cityline Ltd
    * Copyright (c) 2025
    */
   ```

2. **Orphaned file path comments:**
   ```java
   // src/main/java/dev/mars/model/Column.java
   ```

3. **Duplicate license headers:**
   - Multiple Apache license blocks in the same file
   - Malformed license headers

## Result

After running the scripts, all Java files will have clean, standardized headers:

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
package dev.mars.jtable.core.table;
```

## Execution Policy

If you encounter execution policy errors, run:
```powershell
powershell -ExecutionPolicy Bypass -File script-name.ps1
```

## Notes

- Scripts automatically exclude files in `target` directories
- Module-info.java and package-info.java files are handled appropriately
- All changes preserve the original functionality of the code
- The scripts are safe to run multiple times
