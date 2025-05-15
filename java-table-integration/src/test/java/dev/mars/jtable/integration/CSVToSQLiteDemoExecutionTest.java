package dev.mars.jtable.integration;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class that simply executes the CSVToSQLiteDemo class.
 * This class does not perform any assertions or validations, it just runs the demo.
 */
public class CSVToSQLiteDemoExecutionTest {

    private static final Logger logger = LoggerFactory.getLogger(CSVToSQLiteDemoExecutionTest.class);

    /**
     * Test method that executes the CSVToSQLiteDemo.
     * This method simply creates an instance of CSVToSQLiteDemo and calls its run method.
     */
    @Test
    public void testExecuteCSVToSQLiteDemo() {
        logger.info("Starting CSVToSQLiteDemo execution test");
        
        // Create an instance of CSVToSQLiteDemo
        CSVToSQLiteDemo demo = new CSVToSQLiteDemo();
        
        // Run the demo
        demo.run();
        
        logger.info("Completed CSVToSQLiteDemo execution test");
    }
}