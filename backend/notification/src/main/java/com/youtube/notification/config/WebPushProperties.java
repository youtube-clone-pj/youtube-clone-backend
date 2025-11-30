package com.youtube.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "webpush.vapid")
public class WebPushProperties {

    private String publicKey;
    private String privateKey;
    private String subject;
}
