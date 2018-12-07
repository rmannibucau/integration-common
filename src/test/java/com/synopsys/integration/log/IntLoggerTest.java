package com.synopsys.integration.log;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.util.IntEnvironmentVariables;

public class IntLoggerTest {
    @Test
    public void testSetLogLevelWithVariables() {
        final IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.INFO);
        final IntEnvironmentVariables variables = new IntEnvironmentVariables(false);
        logger.setLogLevel(variables);
        assertEquals(LogLevel.INFO, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "FAKE");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.INFO, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "error");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.ERROR, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "erRor");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.ERROR, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "OFF");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.OFF, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "ERROR");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.ERROR, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "WARN");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.WARN, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "INFO");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.INFO, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "DEBUG");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.DEBUG, logger.getLogLevel());

        variables.put("BLACK_DUCK_LOG_LEVEL", "TRACE");
        logger.setLogLevel(variables);
        assertEquals(LogLevel.TRACE, logger.getLogLevel());
    }

}
