package dev.mars.jtable.io.jdbc;

public interface IDatabaseTestConfig {
    String getConnectionString();
    String getUsername();
    String getPassword();

    void initializeDatabase() throws Exception;
    void cleanUpDatabase() throws Exception;
}