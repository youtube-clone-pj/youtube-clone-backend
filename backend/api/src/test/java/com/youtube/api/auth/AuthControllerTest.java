package com.youtube.api.auth;

import com.youtube.api.config.RestAssuredTest;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest extends RestAssuredTest {

    @Test
    @DisplayName("회원가입 API 정상 테스트")
    void signUp() {
        // given
        Map<String, String> params = new HashMap<>();
        params.put("username", "testuser");
        params.put("email", "test@test.com");
        params.put("password", "testpassword");
        params.put("profileImageUrl", "https://example.com/profile.jpg");

        //when
        ExtractableResponse<Response> response = given().log().all().
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(params).
                when().
                post("/api/auth/users").
                then().
                log().all().
                extract();

        //then
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        Long userId = response.as(Long.class);
        assertThat(userId).isNotNull();
    }

    @Test
    @DisplayName("로그인 API 정상 테스트")
    void login() {
        // given - 회원가입
        final String email = "logintest@test.com";
        final String username = "loginuser";
        final String password = "loginpassword";

        Map<String, String> signUpParams = new HashMap<>();
        signUpParams.put("username", username);
        signUpParams.put("email", email);
        signUpParams.put("password", password);
        signUpParams.put("profileImageUrl", "https://example.com/profile.jpg");

        ExtractableResponse<Response> signUpResponse = given().log().all().
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(signUpParams).
                when().
                post("/api/auth/users").
                then().
                log().all().
                extract();

        Long userId = signUpResponse.as(Long.class);

        // when - 로그인
        Map<String, String> loginParams = new HashMap<>();
        loginParams.put("email", email);
        loginParams.put("password", password);

        ExtractableResponse<Response> loginResponse = given().log().all().
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(loginParams).
                when().
                post("/api/auth/login").
                then().
                log().all().
                extract();

        // then
        assertThat(loginResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // 응답 body 검증
        LoginResponse response = loginResponse.as(LoginResponse.class);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getUsername()).isEqualTo(username);

        // 세션 쿠키 검증
        final String sessionCookie = loginResponse.cookie("JSESSIONID");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie).isNotEmpty();
    }
}