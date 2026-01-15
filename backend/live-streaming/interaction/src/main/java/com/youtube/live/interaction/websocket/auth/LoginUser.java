package com.youtube.live.interaction.websocket.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginUser {

    private final Long userId;
    private final String username;
    private final String profileImageUrl;
}
