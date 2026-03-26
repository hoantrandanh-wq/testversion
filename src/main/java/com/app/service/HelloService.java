package com.app.service;

import com.app.repository.GreetingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

    @Autowired
    GreetingRepository greetingRepository;

    public String greet() {
        return greetingRepository.findAll()
                .stream()
                .findFirst()
                .map(greeting -> "Hello " + greeting.getName() + "!")
                .orElse("No Data");
    }
}