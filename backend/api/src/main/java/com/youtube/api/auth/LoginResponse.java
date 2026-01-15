package com.youtube.api.auth;

import com.youtube.core.user.domain.User;
import lombok.*;

//TODO record로 변경
@Data
public class LoginResponse {

    private final Long userId;
    private final String username;
    private final String profileImageUrl;

    public static LoginResponse from(final User user) {
        return new LoginResponse(user.getId(), user.getUsername(), user.getProfileImageUrl());
    }
}
