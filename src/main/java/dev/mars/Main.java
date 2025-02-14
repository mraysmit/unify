package dev.mars;

import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Table table = new Table();

        Map<Integer, String> columnNames = new HashMap<>();
        columnNames.put(0, "Name");
        columnNames.put(1, "Age");
        columnNames.put(2, "Occupation");

        Map<String, String> columnTypes = new HashMap<>();
        columnTypes.put("Name", "string");
        columnTypes.put("Age", "int");
        columnTypes.put("Occupation", "string");

        table.setColumnNames(columnNames, columnTypes);

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
        table.writeToCSV("output.csv");

        var table2 = new Table();

        // Read the table data from a CSV file
        table2.readFromCSV("output.csv", false);

        // Print the table to verify the content
        table2.printTable();
    }
}