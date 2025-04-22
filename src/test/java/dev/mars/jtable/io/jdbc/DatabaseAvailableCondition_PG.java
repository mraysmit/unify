package dev.mars.jtable.io.jdbc;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseAvailableCondition_PG implements ExecutionCondition {
    private static final Logger logger = Logger.getLogger(DatabaseAvailableCondition_PG.class.getName());

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            Class.forName("org.postgresql.Driver");

            // Use the existing config class
            DatabaseTestConfig_PG config = new DatabaseTestConfig_PG();
            Properties props = new Properties();
            props.setProperty("user", config.getUsername());
            props.setProperty("password", config.getPassword());

            try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost/testdb", props)) {
                return ConditionEvaluationResult.enabled("PostgreSQL database is available");
            }
        } catch (ClassNotFoundException e) {
            String message = "PostgreSQL driver not found: " + e.getMessage();
            logger.log(Level.WARNING, message);
            return ConditionEvaluationResult.disabled(message);
        } catch (SQLException e) {
            String message = "PostgreSQL database is not available: " + e.getMessage();
            logger.log(Level.WARNING, message);
            return ConditionEvaluationResult.disabled(message);
        }
    }
}