package br.com.cardoso.log;

import br.com.cardoso.model.ValidationEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.Environment;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.concurrent.*;

public class DynamicLogLevel {

    private static final int DEFAULT_VALIDATION_WINDOW_SECONDS = 60;
    private static final int DEFAULT_ERROR_THRESHOLD = 97;
    private static final int DEFAULT_WARN_THRESHOLD = 95;
    private static final int DEFAULT_INFO_THRESHOLD = 90;
    private static final int DEFAULT_ACTIVE_ERRORS_DEBUG = 5;
    private static final LogLevel DEFAULT_STARTING_LOG_LEVEL = LogLevel.ERROR;
    private static final String DEFAULT_DEBUG_ENABLED = "false";
    private static final String DEFAULT_DYNAMIC_RPS_ENABLED = "false";

    private final Logger logger = LoggerFactory.getLogger(DynamicLogLevel.class);

    private final Deque<ValidationEvent> validationEvents;
    private final LoggingSystem loggingSystem;
    private final DynamicRps dynamicRps;
    private final int validationWindowInSeconds;
    private int currentValidationWindow;
    private final int errorThreshold;
    private final int warnThreshold;
    private final int infoThreshold;
    private final boolean isDebugEnabled;
    private final int activeErrorsDebug;
    private final LogLevel startingLogLevel;
    private final boolean isDynamicRpsEnabled;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledFuture;

    public DynamicLogLevel(Environment environment, LoggingSystem loggingSystem, DynamicRps dynamicRps) {
        this.loggingSystem = loggingSystem;
        this.dynamicRps = dynamicRps;
        this.validationEvents = new ConcurrentLinkedDeque<>();
        this.validationWindowInSeconds = getProperty(environment, "validation.window.seconds", DEFAULT_VALIDATION_WINDOW_SECONDS);
        this.currentValidationWindow = validationWindowInSeconds;
        this.errorThreshold = getProperty(environment, "error.threshold", DEFAULT_ERROR_THRESHOLD);
        this.warnThreshold = getProperty(environment, "warning.threshold", DEFAULT_WARN_THRESHOLD);
        this.infoThreshold = getProperty(environment, "info.threshold", DEFAULT_INFO_THRESHOLD);
        this.isDebugEnabled = Boolean.parseBoolean(environment.getProperty("debug.enabled", DEFAULT_DEBUG_ENABLED));
        this.activeErrorsDebug = getProperty(environment, "active.errors.debug", DEFAULT_ACTIVE_ERRORS_DEBUG);
        this.startingLogLevel = LogLevel.valueOf(environment.getProperty("starting.log.level", DEFAULT_STARTING_LOG_LEVEL.name()));
        loggingSystem.setLogLevel("ROOT", startingLogLevel);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isDynamicRpsEnabled = Boolean.parseBoolean(environment.getProperty("dynamic.rps.enabled", DEFAULT_DYNAMIC_RPS_ENABLED));
    }

    private int getProperty(Environment environment, String key, int defaultValue) {
        return Integer.parseInt(environment.getProperty(key, String.valueOf(defaultValue)));
    }

    @PostConstruct
    public void startScheduler() {
        updateSchedulerInterval(validationWindowInSeconds);
    }

    public void updateSchedulerInterval(int newIntervalInSeconds) {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduledFuture = scheduler.scheduleAtFixedRate(this::removeOldValidationEvents, 0, newIntervalInSeconds, TimeUnit.SECONDS);
    }

    public void addValidationEvent(boolean isSuccess) {
        validationEvents.add(new ValidationEvent(isSuccess));
        if (isDynamicRpsEnabled) {
            dynamicRps.incrementRequestCount();
        }
        validateLogLevel();
    }

    private void validateLogLevel() {
        int totalEvents = validationEvents.size();
        if (totalEvents == 0) {
            loggingSystem.setLogLevel("ROOT", startingLogLevel);
            return;
        }

        long successCount = validationEvents.stream().filter(ValidationEvent::isSuccess).count();
        double successRate = (double) successCount / totalEvents * 100;

        LogLevel newLogLevel = determineLogLevel(successRate);
        LogLevel currentLogLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();

        if (!currentLogLevel.equals(newLogLevel)) {
            loggingSystem.setLogLevel("ROOT", newLogLevel);
            logger.atLevel(Level.valueOf(newLogLevel.name())).log(
                    MessageFormat.format("Alterando o level de log de {0} para {1}. Nível de sucesso: {2}%",
                            currentLogLevel, newLogLevel, successRate));
        }
    }

    private LogLevel determineLogLevel(double successRate) {
        if (successRate > dynamicRps.getAdjustedErrorThreshold(errorThreshold)) {
            return LogLevel.ERROR;
        } else if (successRate > dynamicRps.getAdjustedWarnThreshold(warnThreshold)) {
            return LogLevel.WARN;
        } else if (successRate > dynamicRps.getAdjustedInfoThreshold(infoThreshold)) {
            return LogLevel.INFO;
        } else {
            if (isDebugEnabled) {
                if (validationEvents.stream().filter(validationEvent -> !validationEvent.isSuccess()).count() >= activeErrorsDebug) {
                    return LogLevel.DEBUG;
                }
            }
            return LogLevel.INFO;
        }
    }

    private void removeOldValidationEvents() {
        LocalDateTime now = LocalDateTime.now();
        int adjustedValidationWindow = dynamicRps.getAdjustedValidationWindow(validationWindowInSeconds);
        if (adjustedValidationWindow != currentValidationWindow) {
            currentValidationWindow = adjustedValidationWindow;
            updateSchedulerInterval(adjustedValidationWindow);
        }
        validationEvents.removeIf(event -> {
            boolean shouldRemoveEvent = ChronoUnit.SECONDS.between(event.getTimestamp(), now) > adjustedValidationWindow;
            if (shouldRemoveEvent) {
                LogLevel currentLogLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
                logger.atLevel(Level.valueOf(currentLogLevel.name())).log(
                        MessageFormat.format("Removendo evento de sucesso={0} executado em: {1}", event.isSuccess(), event.getTimestamp()));
            }
            return shouldRemoveEvent;
        });
        validateLogLevel();
    }
}