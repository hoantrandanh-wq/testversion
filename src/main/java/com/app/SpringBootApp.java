package com.app;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.app")
@EnableJpaRepositories(basePackages = "com.app.repository")
@EntityScan(basePackages = "com.app.model")
public class SpringBootApp {
}
