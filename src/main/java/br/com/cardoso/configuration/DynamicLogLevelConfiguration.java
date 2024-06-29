package br.com.cardoso.configuration;

import br.com.cardoso.log.DynamicLogLevel;
import br.com.cardoso.log.DynamicRps;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DynamicLogLevelConfiguration {

    @Bean
    public DynamicLogLevel dynamicLogLevel(Environment environment, LoggingSystem loggingSystem, DynamicRps dynamicRps) {
        return new DynamicLogLevel(environment, loggingSystem, dynamicRps);
    }

    @Bean
    public DynamicRps dynamicRps(LoggingSystem loggingSystem, Environment environment) {
        return new DynamicRps(loggingSystem, environment);
    }
}
