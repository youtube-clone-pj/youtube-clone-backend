package com.youtube.notification.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("!interaction-test & !notification-test")
@EntityScan(basePackages = "com.youtube.notification")
@EnableJpaRepositories(basePackages = "com.youtube.notification")
public class NotificationJPAConfig {
}
