package com.youtube.live.interaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction"})
@ComponentScan(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction"})
@EnableJpaRepositories(basePackages = {"com.youtube.api", "com.youtube.core", "com.youtube.live.interaction"})
@EnableJpaAuditing
public class InteractionTestApplication {

    public static void main(final String[] args) {
        SpringApplication.run(InteractionTestApplication.class, args);
    }
}
