package com.youtube.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("!websocket-test")
@EntityScan(basePackages = "com.youtube.core")
@EnableJpaRepositories(basePackages = "com.youtube.core")
@EnableJpaAuditing
public class JPAConfig {
}
