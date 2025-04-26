package dev.mars.jtable.io.jdbc;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseAvailableCondition_SQLite implements ExecutionCondition {
    private static final Logger logger = Logger.getLogger(DatabaseAvailableCondition_SQLite.class.getName());

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            Class.forName("org.sqlite.JDBC");

            DatabaseTestConfig_SQLite config = new DatabaseTestConfig_SQLite();
            try (Connection conn = DriverManager.getConnection(config.getConnectionString())) {
                return ConditionEvaluationResult.enabled("SQLite database is available");
            }
        } catch (Exception e) {
            String message = "SQLite database is not available: " + e.getMessage();
            logger.log(Level.WARNING, message);
            return ConditionEvaluationResult.disabled(message);
        }
    }
}