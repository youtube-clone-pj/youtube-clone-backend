package com.youtube.live.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EntityScan(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction", "com.youtube.notification"})
@ComponentScan(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction", "com.youtube.notification"})
@EnableJpaRepositories(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction", "com.youtube.notification"})
@EnableJpaAuditing
@EnableAsync
public class InteractionTestApplication {

    public static void main(final String[] args) {
        SpringApplication.run(InteractionTestApplication.class, args);
    }
}
