package br.com.cardoso.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class LogSchedule {

    private final Logger logger = LoggerFactory.getLogger(LogSchedule.class);

    @Scheduled(fixedRate = 5000)
    public void log() {
        logger.debug("DEBUG");
        logger.info("INFO");
        logger.warn("WARN");
        logger.error("ERROR");
    }
}
