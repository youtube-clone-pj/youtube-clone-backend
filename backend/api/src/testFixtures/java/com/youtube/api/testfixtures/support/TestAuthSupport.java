package com.youtube.api.testfixtures.support;

import java.util.HashMap;
import java.util.Map;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import static io.restassured.RestAssured.given;
import org.springframework.http.MediaType;

public class TestAuthSupport {

    public static ExtractableResponse<Response> signUp(final String email, final String username, final String password) {
        Map<String, String> signUpParams = new HashMap<>();
        signUpParams.put("username", username);
        signUpParams.put("email", email);
        signUpParams.put("password", password);
        signUpParams.put("profileImageUrl", "https://example.com/profile.jpg");

        return given().log().all().
                contentType(MediaType.APPLICATION_JSON_VALUE).
                body(signUpParams).
                when().
                post("/api/auth/users").
                then().
                log().all().
                extract();
    }

    public static String login(final String email, final String password) {
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

        //jsessionid 반환
        return loginResponse.cookie("JSESSIONID");
    }
}
