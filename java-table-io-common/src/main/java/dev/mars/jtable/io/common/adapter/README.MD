# dev.mars.jtable.io.adapter Package Functionality

The `dev.mars.jtable.io.adapter` package implements the Adapter pattern to connect various data sources to table objects in the application. This package serves as a bridge between the core table model and different data formats.

## Key Components:

1. **BaseTableAdapter** - An abstract base class that:
    - Implements both `ITableAdapter` and `IDataSource` interfaces
    - Wraps an `ITable` instance
    - Delegates data operations to the underlying table
    - Provides common adapter functionality

2. **Concrete Adapters** - Based on the conversation history, this package includes:
    - `JDBCTableAdapter` - Adapts database connections to table structures
    - `RESTTableAdapter` - Connects REST API endpoints to table structures

3. **Core Functionality**:
    - Data transformation between different formats
    - Table structure mapping
    - Type inference for data values
    - Column management
    - Row addition and manipulation

The adapter pattern used here allows the application to work with multiple data sources without changing the core table implementation. This creates a clean separation between data access and the table model, making the system more modular and easier to extend with new data sources.