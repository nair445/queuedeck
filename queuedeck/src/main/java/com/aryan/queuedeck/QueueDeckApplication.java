package com.aryan.queuedeck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QueueDeckApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueDeckApplication.class, args);
    }
}
