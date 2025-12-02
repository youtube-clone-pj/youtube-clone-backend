package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.SubscriptionBuilder.Subscription;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingRepositoryTest extends IntegrationTest {

    @Autowired
    private LiveStreamingRepository sut;

    @Test
    @DisplayName("구독자가 없는 채널의 라이브 스트리밍 메타데이터를 조회한다")
    void findMetadataById_WithNoSubscriber() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(
                Channel()
                        .withUser(user)
                        .withChannelName("Test Channel")
                        .withProfileImageUrl("https://example.com/profile.jpg")
                        .build()
        );
        final LiveStreaming liveStreaming = testSupport.save(
                LiveStreaming()
                        .withChannel(channel)
                        .withTitle("Test Live Title")
                        .withDescription("Test Live Description")
                        .build()
        );

        // when
        final LiveStreamingMetadataResponse result = sut.findMetadataById(liveStreaming.getId());

        // then
        assertThat(result.channelId()).isEqualTo(channel.getId());
        assertThat(result.channelName()).isEqualTo("Test Channel");
        assertThat(result.channelProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(result.liveStreamingTitle()).isEqualTo("Test Live Title");
        assertThat(result.liveStreamingDescription()).isEqualTo("Test Live Description");
        assertThat(result.liveStreamingStartedAt()).isEqualTo(liveStreaming.getCreatedDate());
        assertThat(result.subscriberCount()).isZero();
    }

    @Test
    @DisplayName("구독자가 있는 채널의 라이브 스트리밍 메타데이터를 조회한다")
    void findMetadataById_WithSubscribers() {
        // given
        final User channelOwner = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        final User subscriber1 = testSupport.save(User().withEmail("subscriber1@test.com").build());
        final User subscriber2 = testSupport.save(User().withEmail("subscriber2@test.com").build());
        final User subscriber3 = testSupport.save(User().withEmail("subscriber3@test.com").build());

        testSupport.save(Subscription().withSubscriber(subscriber1).withChannel(channel).build());
        testSupport.save(Subscription().withSubscriber(subscriber2).withChannel(channel).build());
        testSupport.save(Subscription().withSubscriber(subscriber3).withChannel(channel).build());

        // when
        final LiveStreamingMetadataResponse result = sut.findMetadataById(liveStreaming.getId());

        // then
        assertThat(result.subscriberCount()).isEqualTo(3);
    }
}
