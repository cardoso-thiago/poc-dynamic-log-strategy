package br.com.cardoso.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.core.env.Environment;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicRps {

    private final Logger logger = LoggerFactory.getLogger(DynamicRps.class);

    private static final int DEFAULT_RPS_INTERVAL_SECONDS = 60;
    private static final String DEFAULT_HIGH_RPS = "1000";
    private static final String DEFAULT_MEDIUM_RPS = "500";

    private final LoggingSystem loggingSystem;
    private final AtomicInteger requestCount;
    private LocalDateTime lastResetTime;
    private final int highRps;
    private final int mediumRps;

    public DynamicRps(LoggingSystem loggingSystem, Environment environment) {
        this.loggingSystem = loggingSystem;
        this.requestCount = new AtomicInteger(0);
        this.lastResetTime = LocalDateTime.now();
        this.highRps = Integer.parseInt(environment.getProperty("high.rps", DEFAULT_HIGH_RPS));
        this.mediumRps = Integer.parseInt(environment.getProperty("medium.rps", DEFAULT_MEDIUM_RPS));
    }

    public int getAverageRPS() {
        LocalDateTime now = LocalDateTime.now();
        long secondsElapsed = Duration.between(lastResetTime, now).getSeconds();
        if (secondsElapsed == 0) {
            return 0;
        }
        if (secondsElapsed > DEFAULT_RPS_INTERVAL_SECONDS) {
            int count = requestCount.getAndSet(0);
            lastResetTime = now;
            return (int) (count / secondsElapsed);
        }
        return (int) (requestCount.get() / secondsElapsed);
    }

    public void incrementRequestCount() {
        requestCount.incrementAndGet();
    }

    public int getAdjustedValidationWindow(int validationWindow) {
        LogLevel currentLogLevel = loggingSystem.getLoggerConfiguration("ROOT").getConfiguredLevel();
        int rps = getAverageRPS();
        if (rps > highRps) {
            int max = Math.max(10, validationWindow / 3);
            logger.atLevel(Level.valueOf(currentLogLevel.name())).log(
                    MessageFormat.format("RPS: {0}. Ajustando janela de validação para {1} segundos.", rps, max));
            return max;
        } else if (rps > mediumRps) {
            int max = Math.max(10, validationWindow / 2);
            logger.atLevel(Level.valueOf(currentLogLevel.name())).log(
                    MessageFormat.format("RPS: {0}. Ajustando janela de validação para {1} segundos.", rps, max));
            return max;
        }
        return validationWindow;
    }

    public int getAdjustedErrorThreshold(int errorThreshold) {
        int rps = getAverageRPS();
        if (rps > highRps) {
            return Math.min(99, errorThreshold + 2);
        } else if (rps > mediumRps) {
            return Math.min(99, errorThreshold + 1);
        }
        return errorThreshold;
    }

    public int getAdjustedWarnThreshold(int warnThreshold) {
        int rps = getAverageRPS();
        if (rps > highRps) {
            return Math.min(97, warnThreshold + 2);
        } else if (rps > mediumRps) {
            return Math.min(97, warnThreshold + 1);
        }
        return warnThreshold;
    }

    public int getAdjustedInfoThreshold(int infoThreshold) {
        int rps = getAverageRPS();
        if (rps > highRps) {
            return Math.min(95, infoThreshold + 5);
        } else if (rps > mediumRps) {
            return Math.min(95, infoThreshold + 3);
        }
        return infoThreshold;
    }
}
