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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DynamicLogLevel {

    private static final int DEFAULT_VALIDATION_RANGE_SECONDS = 30;
    private static final int DEFAULT_ERROR_THRESHOLD = 97;
    private static final int DEFAULT_WARN_THRESHOLD = 95;
    private static final int DEFAULT_INFO_THRESHOLD = 90;
    private static final LogLevel DEFAULT_STARTING_LOG_LEVEL = LogLevel.ERROR;

    private final Logger logger = LoggerFactory.getLogger(DynamicLogLevel.class);

    private final List<ValidationEvent> validationEvents;
    private final int validationWindowInSeconds;
    private final LoggingSystem loggingSystem;
    private final int errorThreshold;
    private final int warnThreshold;
    private final int infoThreshold;
    private final LogLevel startingLogLevel;
    private final ScheduledExecutorService scheduler;

    public DynamicLogLevel(Environment environment, LoggingSystem loggingSystem) {
        this.loggingSystem = loggingSystem;
        this.validationEvents = new ArrayList<>();
        this.validationWindowInSeconds = getProperty(environment, "validation.window.seconds", DEFAULT_VALIDATION_RANGE_SECONDS);
        this.errorThreshold = getProperty(environment, "error.threshold", DEFAULT_ERROR_THRESHOLD);
        this.warnThreshold = getProperty(environment, "warning.threshold", DEFAULT_WARN_THRESHOLD);
        this.infoThreshold = getProperty(environment, "info.threshold", DEFAULT_INFO_THRESHOLD);
        this.startingLogLevel = LogLevel.valueOf(environment.getProperty("starting.log.level", DEFAULT_STARTING_LOG_LEVEL.name()));
        loggingSystem.setLogLevel("ROOT", startingLogLevel);
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    private int getProperty(Environment environment, String key, int defaultValue) {
        return Integer.parseInt(environment.getProperty(key, String.valueOf(defaultValue)));
    }

    @PostConstruct
    public void startScheduler() {
        scheduler.scheduleAtFixedRate(this::removeOldValidationEvents, 0, validationWindowInSeconds, TimeUnit.SECONDS);
    }

    public void addValidationEvent(boolean isSuccess) {
        validationEvents.add(new ValidationEvent(isSuccess));
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
            logger.atLevel(Level.valueOf(newLogLevel.name())).log(
                    MessageFormat.format("Alterando o level de log de {0} para {1}. Nível de sucesso: {2}%",
                            currentLogLevel, newLogLevel, successRate));
            loggingSystem.setLogLevel("ROOT", newLogLevel);
        }
    }

    private LogLevel determineLogLevel(double successRate) {
        if (successRate > errorThreshold) {
            return LogLevel.ERROR;
        } else if (successRate > warnThreshold) {
            return LogLevel.WARN;
        } else if (successRate > infoThreshold) {
            return LogLevel.INFO;
        } else {
            return LogLevel.DEBUG;
        }
    }

    private void removeOldValidationEvents() {
        LocalDateTime now = LocalDateTime.now();
        validationEvents.removeIf(event -> {
            boolean shouldRemoveEvent = ChronoUnit.SECONDS.between(event.getTimestamp(), now) > validationWindowInSeconds;
            if (shouldRemoveEvent) {
                logger.info(MessageFormat.format("Removendo evento de sucesso={0} executado em: {1}", event.isSuccess(), event.getTimestamp()));
            }
            return shouldRemoveEvent;
        });
        validateLogLevel();
    }
}