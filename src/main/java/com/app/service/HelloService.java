package com.app.service;

import org.springframework.stereotype.Service;

@Service
public class HelloService {
    public String greet() {
        return "Hello World from Spring Boot! v0";
    }
}