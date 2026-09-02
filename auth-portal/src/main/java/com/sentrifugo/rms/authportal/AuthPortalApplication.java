package com.sentrifugo.rms.authportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.sentrifugo.rms")
@EntityScan(basePackages = "com.sentrifugo.rms.db.entity")
@EnableJpaRepositories(basePackages = "com.sentrifugo.rms.db.repository")
public class AuthPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthPortalApplication.class, args);
    }
}
