package com.youtube.api.auth;

import lombok.*;

@Data
public class RegisterRequest {

    private final String username;
    private final String email;
    private final String password;
    private final String profileImageUrl;
}
