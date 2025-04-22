package dev.mars.jtable.io.csv;

import dev.mars.jtable.io.file.FileConnection;

public class CSVProcessor {
    private ICSVReader csvReader;
    private ICSVWriter csvWriter;

    public CSVProcessor(ICSVReader csvReader, ICSVWriter csvWriter) {
        this.csvReader = csvReader;
        this.csvWriter = csvWriter;
    }

    public void readFromCSV(ICSVDataSource dataSource, FileConnection connection, boolean hasHeaderRow, boolean allowEmptyValues) {
        csvReader.readFromCSV(dataSource, connection, hasHeaderRow, allowEmptyValues);
    }

    public void writeToCSV(ICSVDataSource dataSource, String fileName, boolean withHeaderRow) {
        csvWriter.writeToCSV(dataSource, fileName, withHeaderRow);
    }
}

