package com.youtube.live.interaction.livestreaming.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveStreamingChatPolicyTest {

    @Test
    @DisplayName("라이브 방송 중일 때 검증에 성공한다")
    void validateWhenLive() {
        // given
        final LiveStreamingStatus liveStatus = LiveStreamingStatus.LIVE;

        // when & then
        assertThatCode(() -> LiveStreamingChatPolicy.validate(liveStatus))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @EnumSource(value = LiveStreamingStatus.class, names = {"SCHEDULED", "ENDED"})
    @DisplayName("라이브 방송 중이 아닐 때 예외가 발생한다")
    void validateWhenNotLive(final LiveStreamingStatus status) {
        // when & then
        assertThatThrownBy(() -> LiveStreamingChatPolicy.validate(status))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("채팅은 라이브 방송 중에만 가능합니다");
    }
}
