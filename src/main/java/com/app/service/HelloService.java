package com.app.service;

import com.app.repository.GreetingRepository;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    private final GreetingRepository greetingRepository;

    public HelloService(GreetingRepository greetingRepository) {
        this.greetingRepository = greetingRepository;
    }

    public String greet() {
        return greetingRepository.findAll()
                .stream()
                .findFirst()
                .map(greeting -> "Hello " + greeting.getName() + "!")
                .orElse("No Data");
    }
}