package dev.mars.jtable;

import dev.mars.jtable.io.csv.CSVUtils;
import dev.mars.jtable.io.mapping.ColumnMapping;
import dev.mars.jtable.io.mapping.MappingConfiguration;
import dev.mars.jtable.core.table.TableBuilder;
import dev.mars.jtable.core.table.TableCore;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {

        // Create a mapping configuration for csv
        MappingConfiguration config = new MappingConfiguration()
                .setSourceLocation("persondata.csv")
                .setOption("hasHeaderRow", true)
                .setOption("allowEmptyValues", false)
                .addColumnMapping(new ColumnMapping("Name", "FullName", "string"))
                .addColumnMapping(new ColumnMapping("Age", "Age", "int"))
                .addColumnMapping(new ColumnMapping("Salary", "Income", "double"));


        // Create a table
        TableBuilder tableBuilder = new TableBuilder();

        var tableIn = tableBuilder
                .addStringColumn("Name")
                .addIntColumn("Age")
                .addDoubleColumn("Salary")
                .setCreateDefaultValue(true)
                .addRow("Name", "Alice", "Age", "30", "Salary", "50000")
                .addRow("Name", "Bob", "Age", "25", "Salary", "60000")
                .addRow("Name", "Charlie", "Age", "35", "Salary", "70000")
                .build();


        CSVUtils.writeToCSV(tableIn, "persondata.csv", true);

        var tableOut = new TableCore();

        // Read data from the CSV file using the mapping configuration
        CSVUtils.readFromCSV(tableOut, config);



        TableCore table = new TableCore();

        var columnNames = new LinkedHashMap<String,String>();
        columnNames.put("Name", "string");
        columnNames.put("Age", "int");
        columnNames.put("Occupation", "string");

        table.setColumns(columnNames);

        Map<String, String> row1 = new HashMap<>();
        row1.put("Name", "Alice");
        row1.put("Age", "30");
        row1.put("Occupation", "Engineer");

        Map<String, String> row2 = new HashMap<>();
        row2.put("Name", "Bob");
        row2.put("Age", "25");
        row2.put("Occupation", "Designer");

        table.addRow(row1);
        table.addRow(row2);

        table.printTable();

        System.out.println("Value at (0, 'Age'): " + table.getValueAt(0, "Age"));
        table.setValueAt(0, "Age", "31");
        System.out.println("Updated Value at (0, 'Age'): " + table.getValueAt(0, "Age"));

        table.printTable();

        // Write the table data to a CSV file
        CSVUtils.writeToCSV(table, "output.csv", false);

        TableCore table2 = new TableCore();

        // Read the table data from a CSV file
        CSVUtils.readFromCSV(table2, "output.csv", false, false);

        // Print the table to verify the content
        table2.printTable();
    }
}
