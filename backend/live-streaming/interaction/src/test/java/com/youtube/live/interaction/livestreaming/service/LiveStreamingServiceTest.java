package com.youtube.live.interaction.livestreaming.service;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateRequest;
import com.youtube.live.interaction.livestreaming.service.dto.LiveStreamingCreateResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingServiceTest extends IntegrationTest {

    @Autowired
    private LiveStreamingService sut;

    @Test
    @DisplayName("라이브 스트리밍을 시작한다 (V1)")
    void startLiveStreamingV1_Success() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());

        final LiveStreamingCreateRequest request = new LiveStreamingCreateRequest(
                "테스트 라이브 제목",
                "테스트 라이브 설명",
                "https://example.com/thumbnail.jpg"
        );

        // when
        final LiveStreamingCreateResponse response = sut.startLiveStreamingV1(
                user.getId(),
                request
        );

        // then
        assertThat(response.status()).isEqualTo(LiveStreamingStatus.LIVE);
    }

    @Test
    @DisplayName("라이브 스트리밍을 시작한다 (V2)")
    void startLiveStreamingV2_Success() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());

        final LiveStreamingCreateRequest request = new LiveStreamingCreateRequest(
                "테스트 라이브 제목",
                "테스트 라이브 설명",
                "https://example.com/thumbnail.jpg"
        );

        // when
        final LiveStreamingCreateResponse response = sut.startLiveStreamingV2(
                user.getId(),
                request
        );

        // then
        assertThat(response.status()).isEqualTo(LiveStreamingStatus.LIVE);
    }
}
