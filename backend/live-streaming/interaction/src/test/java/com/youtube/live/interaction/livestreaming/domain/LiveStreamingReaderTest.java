package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiveStreamingReaderTest extends IntegrationTest {

    @Autowired
    private LiveStreamingReader sut;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCacheNames()
                .forEach(cacheName -> cacheManager.getCache(cacheName).clear());
    }

    @Test
    @DisplayName("라이브 스트리밍 상태를 캐시에서 조회한다")
    void readCachedStatusBy_Success() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withStatus(LiveStreamingStatus.LIVE)
                        .build()
        );

        // when
        final LiveStreamingStatus status = sut.readCachedStatusBy(liveStreaming.getId());

        // then
        assertThat(status).isEqualTo(LiveStreamingStatus.LIVE);

        // 캐시에 저장되었는지 확인
        final LiveStreamingStatus cachedStatus = cacheManager.getCache("liveStreamingStatus")
                .get(liveStreaming.getId(), LiveStreamingStatus.class);
        assertThat(cachedStatus).isEqualTo(LiveStreamingStatus.LIVE);
    }
}