package com.iciftci.deneme.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableMongoRepositories
@EnableMongoAuditing
@EnableAsync
public class NotificationApplication {


    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}
