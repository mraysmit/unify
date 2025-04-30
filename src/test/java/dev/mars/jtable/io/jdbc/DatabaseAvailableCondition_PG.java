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

    // Connection details for PostgreSQL
    private static final String connectionString = "jdbc:postgresql://localhost/testdb";
    private static final String username = "postgres_user";
    private static final String password = "postgres_password";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        try {
            Class.forName("org.postgresql.Driver");

            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);

            try (Connection conn = DriverManager.getConnection(connectionString, props)) {
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
