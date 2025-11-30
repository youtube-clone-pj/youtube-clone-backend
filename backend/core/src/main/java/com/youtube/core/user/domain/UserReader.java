package com.youtube.core.user.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.core.user.exception.UserErrorCode;
import com.youtube.core.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserReader {

    private final UserRepository userRepository;

    public boolean existsBy(final String email) {
        return userRepository.existsByEmail(email);
    }

    public User readBy(final String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    }

    public User readBy(final Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    }
}
