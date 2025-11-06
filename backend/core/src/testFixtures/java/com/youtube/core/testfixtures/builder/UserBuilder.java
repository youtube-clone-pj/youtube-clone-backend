package com.youtube.core.testfixtures.builder;

import com.youtube.core.user.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBuilder {

    private Long id;
    private String username = "테스트유저";
    private String password = "encodedPassword123!";
    private String email = "test@example.com";
    private String profileImageUrl = "https://example.com/profile.jpg";

    public static UserBuilder User() {
        return new UserBuilder();
    }

    public User build() {
        return User.builder()
            .id(this.id)
            .username(this.username)
            .password(this.password)
            .email(this.email)
            .profileImageUrl(this.profileImageUrl)
            .build();
    }
}