package com.youtube.live.interaction.websocket.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

@Getter
@AllArgsConstructor
public class AuthenticatedPrincipal implements Principal {

    private final Long userId;
    private final String username;
    private final String profileImageUrl;

    @Override
    public String getName() {
        return username;
    }
}
