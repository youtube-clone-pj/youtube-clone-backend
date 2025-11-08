package com.youtube.live.interaction.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * STOMP 메시지에서 사용자 정보를 담는 Principal 구현체
 */
@Getter
@AllArgsConstructor
public class StompPrincipal implements Principal {

    private final Long userId;
    private final String username;

    @Override
    public String getName() {
        return username;
    }
}
