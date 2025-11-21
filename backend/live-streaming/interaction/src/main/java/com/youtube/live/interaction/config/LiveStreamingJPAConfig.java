package com.youtube.live.interaction.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("!interaction-test & !notification-test")
@EntityScan(basePackages = "com.youtube.live.interaction")
@EnableJpaRepositories(basePackages = "com.youtube.live.interaction")
public class LiveStreamingJPAConfig {
}
