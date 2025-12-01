package com.youtube.api.auth;

import com.youtube.common.exception.AuthErrorCode;
import com.youtube.api.util.PasswordEncoder;
import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.domain.ChannelWriter;
import com.youtube.core.user.domain.User;
import com.youtube.core.user.domain.UserReader;
import com.youtube.core.user.domain.UserWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserReader userReader;
    private final UserWriter userWriter;
    private final ChannelWriter channelWriter;

    @Transactional
    public Long signUp(final RegisterRequest request) {
        if(userReader.existsBy(request.getEmail())) {
            throw new BaseException(AuthErrorCode.ALREADY_REGISTERED_EMAIL);
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
            throw new BaseException(AuthErrorCode.PASSWORD_MISMATCH);
        }

        log.info("User 로그인 성공 - userId: {}", user.getId());

        return LoginResponse.from(user);
    }
}
