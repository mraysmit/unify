# Bug Description: Table.addRow() Method

## Bug Summary
There is a bug in the `addRow()` method of the `Table` class where an exception is thrown even after default values are added for missing columns when the `createDefaultValue` flag is set to `true`.

## Bug Details

### Current Implementation
The current implementation of the `addRow()` method has the following logic:

1. It first checks if the row size doesn't match the columns size (line 36).
2. If they don't match and `createDefaultValue` is `true`, it adds default values for missing columns (lines 38-45).
3. However, after adding these default values, it still throws an `IllegalArgumentException` with the message "Row size does not match column count" (line 48) without rechecking if the row size now matches the columns size.

### Code Snippet
```java
public void addRow(Map<String, String> row) {
    if (row.size() != columns.size()) {

        if (createDefaultValue)  {
            if (row.size() != columns.size()) {
                for (String column : columns.keySet()) {
                    if (!row.containsKey(column)) {
                        row.put(column, getDefaultValue(columns.get(column)));
                    }
                }
            }
        }

        throw new IllegalArgumentException("Row size does not match column count");
    }
    // Rest of the method...
}
```

### Issues Identified
1. **Redundant Condition Check**: The condition `if (row.size() != columns.size())` is checked twice (lines 36 and 39), which is unnecessary.
2. **Exception After Adding Default Values**: Even after adding default values to the row, the method still throws an exception without rechecking if the row size now matches the columns size.
3. **Incorrect Control Flow**: The exception is thrown unconditionally after the `if (createDefaultValue)` block, regardless of whether default values were added or not.

### Expected Behavior
When `createDefaultValue` is `true` and a row is missing some columns:
1. Default values should be added for the missing columns.
2. The method should recheck if the row size now matches the columns size.
3. If the row size now matches, the method should continue with validation and add the row to the table.
4. Only if the row size still doesn't match after adding default values (which shouldn't happen), or if `createDefaultValue` is `false`, should an exception be thrown.

## Impact
This bug prevents the `createDefaultValue` functionality from working as intended. Even when `createDefaultValue` is set to `true`, rows with missing columns cannot be added to the table, which defeats the purpose of having this feature.

## Test Case
The bug is demonstrated in the `testCreateDefaultValueTrue` test in `TableTest.java`:
```java
@Test
void testCreateDefaultValueTrue() throws Exception {
    // Use reflection to ensure createDefaultValue is true
    java.lang.reflect.Field createDefaultValueField = Table.class.getDeclaredField("createDefaultValue");
    createDefaultValueField.setAccessible(true);
    createDefaultValueField.set(table, true);

    // Create a row with a missing column
    Map<String, String> row = new HashMap<>();
    row.put("Name", "Alice");
    row.put("Age", "30");
    // Occupation is missing

    // Currently, this throws an exception because of the bug in addRow
    // Even though createDefaultValue is true and default values are added,
    // the method still throws an exception because it doesn't re-check the row size
    assertThrows(IllegalArgumentException.class, () -> table.addRow(row));
}
```

## Fix Recommendation
To fix this bug, the `addRow()` method should be modified to:
1. Remove the redundant condition check.
2. Only throw an exception if the row size doesn't match the columns size after attempting to add default values (when `createDefaultValue` is `true`).
3. Recheck the row size after adding default values before throwing an exception.