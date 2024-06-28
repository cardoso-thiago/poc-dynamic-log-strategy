package br.com.cardoso.configuration;

import br.com.cardoso.log.DynamicLogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class DynamicLogLevelConfiguration {

    @Bean
    public DynamicLogLevel dynamicLogLevel(Environment environment, LoggingSystem loggingSystem) {
        return new DynamicLogLevel(environment, loggingSystem);
    }
}
