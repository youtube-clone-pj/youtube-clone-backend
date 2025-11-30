package com.youtube.notification.config;

import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.PushAsyncService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.GeneralSecurityException;
import java.security.Security;

@Configuration
@RequiredArgsConstructor
public class WebPushConfig {

    private final WebPushProperties webPushProperties;

    @Bean
    public PushAsyncService pushAsyncService() throws GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        return new PushAsyncService(
                webPushProperties.getPublicKey(),
                webPushProperties.getPrivateKey(),
                webPushProperties.getSubject()
        );
    }
}
