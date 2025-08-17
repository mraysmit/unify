# dev.mars.jtable.core.table Package

The `dev.mars.jtable.core.table` package contains the core table implementation classes that form the foundation of the JTable library. Based on the code context:

## Key Components:

1. **Cell Class**
    - Implements `ICell<T>` interface
    - Generic type-safe table cell implementation
    - Stores values with column-based validation
    - Provides string conversion of values

2. **Table Classes** (inferred from package structure)
    - Likely contains concrete implementations of `ITable` interface
    - Manages collections of rows and columns
    - Handles data storage and manipulation operations
    - Provides structure for tabular data

3. **Column Implementation**
    - Referenced through `IColumn<T>` interface
    - Validates values that can be stored in cells
    - Maintains column metadata (name, type)

This package represents the core model layer of the application, implementing the base table structures that adapters (from the `io.adapter` package) connect to various data sources. It implements a type-safe table model where columns define constraints and cells honour those constraints.

The package likely works together with interfaces defined in `dev.mars.jtable.core.model` to provide a complete implementation of the table model.