package br.com.cardoso.schedule;

import br.com.cardoso.log.DynamicRps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class LogSchedule {

    private final Logger logger = LoggerFactory.getLogger(LogSchedule.class);
    private final DynamicRps dynamicRps;

    public LogSchedule(DynamicRps dynamicRps) {
        this.dynamicRps = dynamicRps;
    }

    @Scheduled(fixedRate = 5000)
    public void log() {
        logger.debug("DEBUG. RPS => {}", dynamicRps.getAverageRPS());
        logger.info("INFO. RPS => {}", dynamicRps.getAverageRPS());
        logger.warn("WARN. RPS => {}", dynamicRps.getAverageRPS());
        logger.error("ERROR. RPS => {}", dynamicRps.getAverageRPS());
    }
}
