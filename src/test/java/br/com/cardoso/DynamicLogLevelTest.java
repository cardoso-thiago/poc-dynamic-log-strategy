package br.com.cardoso;

import br.com.cardoso.log.DynamicLogLevel;
import br.com.cardoso.log.DynamicRps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@TestPropertySource(locations = "classpath:test-application.properties")
public class DynamicLogLevelTest {

    @Autowired
    private DynamicLogLevel dynamicLogLevel;
    @Autowired
    private LoggingSystem loggingSystem;
    @Autowired
    private DynamicRps dynamicRps;

    @BeforeEach
    void clear() {
        loggingSystem.setLogLevel("ROOT", LogLevel.ERROR);
        dynamicLogLevel.clearValidationEvents(LocalDateTime.now(), -1);
    }

    @Test
    void shouldReturnStartingLevelWhenValidationEventIsEmpty() {
        //given
        dynamicLogLevel.addValidationEvent(false);
        dynamicLogLevel.updateSchedulerInterval(1);
        dynamicLogLevel.clearValidationEvents(LocalDateTime.now(), -1);
        await().during(2, TimeUnit.SECONDS).until(() -> true);
        dynamicLogLevel.updateSchedulerInterval(60);
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.ERROR, logLevel);
    }

    @Test
    void shouldReturnLevelInfoWhenValidationEventsContainsOnlyErrors() {
        //given
        dynamicLogLevel.addValidationEvent(false);
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.INFO, logLevel);
    }

    @Test
    void shouldReturnStartingLevelWhenValidationEventsContainsOnlySuccess() {
        //given
        dynamicLogLevel.addValidationEvent(true);
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.ERROR, logLevel);
    }

    @Test
    void shouldReturnLevelInfoWhenSuccessRateIs70Percent() {
        //given
        for (int i = 0; i < 7; i++) {
            dynamicLogLevel.addValidationEvent(true);
        }
        for (int i = 0; i < 3; i++) {
            dynamicLogLevel.addValidationEvent(false);
        }
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.INFO, logLevel);
    }

    @Test
    void shouldReturnLevelWarnInfoWhenSuccessRateIs80Percent() {
        //given
        for (int i = 0; i < 8; i++) {
            dynamicLogLevel.addValidationEvent(true);
        }
        for (int i = 0; i < 2; i++) {
            dynamicLogLevel.addValidationEvent(false);
        }
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.WARN, logLevel);
    }

    @Test
    void shouldReturnLevelErrorInfoWhenSuccessRateIs80Percent() {
        //given
        for (int i = 0; i < 9; i++) {
            dynamicLogLevel.addValidationEvent(true);
        }
        for (int i = 0; i < 1; i++) {
            dynamicLogLevel.addValidationEvent(false);
        }
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.ERROR, logLevel);
    }

    @Test
    void shouldReturnLevelDebugWhenSuccessRateIsBelow80PercentAndHasFiveOrMoreErrors() {
        //given
        for (int i = 0; i < 5; i++) {
            dynamicLogLevel.addValidationEvent(true);
        }
        for (int i = 0; i < 5; i++) {
            dynamicLogLevel.addValidationEvent(false);
        }
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.DEBUG, logLevel);
    }

    @Test
    void shouldReturnLevelInfoWhenSuccessRateIsBelow80PercentAndHasLessThanFiveErrors() {
        //given
        for (int i = 0; i < 6; i++) {
            dynamicLogLevel.addValidationEvent(true);
        }
        for (int i = 0; i < 4; i++) {
            dynamicLogLevel.addValidationEvent(false);
        }
        //when
        LogLevel logLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        //then
        assertEquals(LogLevel.INFO, logLevel);
    }
}
