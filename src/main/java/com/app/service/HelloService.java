package com.app.service;

import com.app.repository.GreetingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    private static final Logger log = LoggerFactory.getLogger(HelloService.class);

    private final GreetingRepository greetingRepository;

    public HelloService(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    public String greet() {
        log.error("Test error log before greet execution");
        return greetingRepository.findAll()
                .stream()
                .findFirst()
                .map(greeting -> "Hello " + greeting.getName() + "!")
                .orElse("No Data");
    }
}