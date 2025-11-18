package com.youtube.api.auth;

import com.youtube.api.util.PasswordEncoder;
import com.youtube.core.channel.domain.ChannelWriter;
import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import com.youtube.core.user.domain.UserWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserReader userReader;
    private final UserWriter userWriter;
    private final ChannelWriter channelWriter;

    @Transactional
    public Long signUp(final RegisterRequest request) {
        if(userReader.existsBy(request.getEmail())) {
            throw new IllegalStateException("이미 가입된 이메일입니다");
        }

        final Long userId = userWriter.write(
                request.getUsername(),
                request.getEmail(),
                PasswordEncoder.encode(request.getPassword()),
                request.getProfileImageUrl()
        );

        channelWriter.write(userReader.readBy(userId), request.getUsername());

        return userId;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(final LoginRequest request) {
        final User user = userReader.readBy(request.getEmail());

        if (!PasswordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다");
        }

        return LoginResponse.from(user);
    }
}
