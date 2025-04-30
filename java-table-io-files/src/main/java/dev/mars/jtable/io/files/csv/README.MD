# dev.mars.jtable.io.csv Package

The `dev.mars.jtable.io.csv` package provides functionality for CSV file operations within the JTable library. This package handles reading from and writing to CSV files with configurable mapping behaviors.

## Key Components:

1. **CSVWriter**
    - Implements `ICSVWriter` interface
    - Writes table data to CSV files
    - Supports header row inclusion
    - Uses `FileConnection` for file access

2. **CSVMappingReader**
    - Reads CSV files according to mapping configurations
    - Maps CSV columns to table columns based on configuration settings
    - Handles header rows and column name/index mappings
    - Provides extensive validation and error handling

3. **ICSVDataSource**
    - Interface for CSV-specific data source implementations
    - Provides methods to access column information and cell values

## Core Functionality:

- **CSV Reading**: Loading CSV data into table structures
- **CSV Writing**: Exporting table data to CSV format
- **Data Mapping**: Configuring how CSV columns map to table columns
- **Validation**: Checking for file existence and proper formatting
- **Error Handling**: Comprehensive error detection and reporting

This package integrates with the broader JTable library to enable CSV import/export capabilities with flexible mapping options for data transformation.