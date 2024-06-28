package br.com.cardoso.controller;

import br.com.cardoso.log.DynamicLogLevel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final DynamicLogLevel dynamicLogLevel;

    public TestController(DynamicLogLevel dynamicLogLevel) {
        this.dynamicLogLevel = dynamicLogLevel;
    }

    @GetMapping("/test/{value}")
    public String test(@PathVariable("value") String value) {
        if (value.equals("error")) {
            dynamicLogLevel.addValidationEvent(false);
            return value;
        }
        dynamicLogLevel.addValidationEvent(true);
        return value;
    }
}
