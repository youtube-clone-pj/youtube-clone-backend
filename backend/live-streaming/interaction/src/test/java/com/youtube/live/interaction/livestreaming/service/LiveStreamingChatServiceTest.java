package com.youtube.live.interaction.livestreaming.service;

import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingChatInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import java.time.Instant;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveStreamingChatServiceTest extends IntegrationTest {

    @Autowired
    private LiveStreamingChatService sut;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    @DisplayName("라이브 방송 중에 채팅을 전송한다")
    void sendMessage_Success() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withStatus(LiveStreamingStatus.LIVE)
                        .build()
        );

        final Long userId = user.getId();
        final String username = "테스트유저";
        final String profileImageUrl = "https://example.com/profile.jpg";
        final String message = "안녕하세요!";
        final ChatMessageType messageType = ChatMessageType.CHAT;

        // when
        final LiveStreamingChatInfo chatInfo = sut.sendMessage(
                liveStreaming.getId(),
                userId,
                username,
                profileImageUrl,
                message,
                messageType,
                Instant.now()
        );

        // then
        assertThat(chatInfo.getUsername()).isEqualTo(username);
        assertThat(chatInfo.getUserProfileImageUrl()).isEqualTo(profileImageUrl);
        assertThat(chatInfo.getMessage()).isEqualTo(message);
        assertThat(chatInfo.getChatMessageType()).isEqualTo(messageType);
    }

    @Test
    @DisplayName("라이브 방송이 아닐 때 채팅을 전송할 수 없다")
    void sendMessage_NotLive_ThrowException() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withStatus(LiveStreamingStatus.ENDED)
                        .build()
        );

        // when & then
        assertThatThrownBy(() -> sut.sendMessage(
                liveStreaming.getId(),
                user.getId(),
                "테스트유저",
                "https://example.com/profile.jpg",
                "채팅 메시지",
                ChatMessageType.CHAT,
                Instant.now()
        ))
                .isInstanceOf(BaseException.class)
                .hasMessage(LiveStreamingErrorCode.CHAT_NOT_ALLOWED_WHEN_OFFLINE.getMessage());
    }
}