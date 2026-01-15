package com.youtube.api.auth;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private static final String SESSION_USER_ID = "userId";
    private static final String SESSION_USERNAME = "username";
    private static final String SESSION_PROFILE_IMAGE_URL = "profileImageUrl";

    @PostMapping("/users")
    public ResponseEntity<Long> signUp(@RequestBody RegisterRequest request) {
        Long newMemberId = authService.signUp(request);
        return ResponseEntity.ok(newMemberId);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request, final HttpSession session) {
        final LoginResponse response = authService.login(request);
        session.setAttribute(SESSION_USER_ID, response.getUserId());
        session.setAttribute(SESSION_USERNAME, response.getUsername());
        session.setAttribute(SESSION_PROFILE_IMAGE_URL, response.getProfileImageUrl());

        return ResponseEntity.ok(response);
    }

}
