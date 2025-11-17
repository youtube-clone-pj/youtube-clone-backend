package com.youtube.live.interaction.livestreaming.service;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.livestreaming.domain.*;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import com.youtube.live.interaction.websocket.event.dto.LikeCountBroadcastResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LiveStreamingReactionServiceTest extends IntegrationTest {

    @Autowired
    private LiveStreamingReactionService sut;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    @DisplayName("좋아요/싫어요가 없을 떄 좋아요를 선택하면, 좋아요가 생성되고 이벤트가 발행된다")
    void toggleReaction_NoExistingReaction_CreateLike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // when
        final ReactionToggleResult result = sut.toggleReaction(
                liveStreaming.getId(),
                user.getId(),
                ReactionType.LIKE
        );

        // then
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isDisliked()).isFalse();

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(messagingTemplate, times(1))
                            .convertAndSend(
                                    eq("/topic/livestreams/" + liveStreaming.getId() + "/like-count"),
                                    any(LikeCountBroadcastResponse.class)
                            );
                });
    }

    @Test
    @DisplayName("좋아요/싫어요가 없을 때 싫어요를 선택하면, 싫어요가 생성되고 이벤트가 발행되지 않는다")
    void toggleReaction_NoExistingReaction_CreateDislike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // when
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.DISLIKE
        );

        // then
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isTrue();

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("좋아요가 있을 때 좋아요를 선택하면 좋아요가 해제되고 이벤트가 발행되지 않는다")
    void toggleReaction_ExistingLike_RemoveLike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());
        testSupport.save(
            LiveStreamingReaction.builder()
                .liveStreaming(liveStreaming)
                .user(user)
                .type(ReactionType.LIKE)
                .build()
        );

        // when
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.LIKE
        );

        // then
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isFalse();

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("좋아요가 있을 때 싫어요를 선택하면 싫어요로 변경되고 이벤트가 발행되지 않는다")
    void toggleReaction_ExistingLike_ChangeToDislike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());
        testSupport.save(
            LiveStreamingReaction.builder()
                .liveStreaming(liveStreaming)
                .user(user)
                .type(ReactionType.LIKE)
                .build()
        );

        // when
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.DISLIKE
        );

        // then
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isTrue();

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("싫어요가 있을 때 좋아요를 선택하면 좋아요로 변경되고 이벤트가 발행된다")
    void toggleReaction_ExistingDislike_ChangeToLike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());
        testSupport.save(
            LiveStreamingReaction.builder()
                .liveStreaming(liveStreaming)
                .user(user)
                .type(ReactionType.DISLIKE)
                .build()
        );

        // when
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.LIKE
        );

        // then
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isDisliked()).isFalse();

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(messagingTemplate, times(1))
                            .convertAndSend(
                                    eq("/topic/livestreams/" + liveStreaming.getId() + "/like-count"),
                                    any(LikeCountBroadcastResponse.class)
                            );
                });
    }

    @Test
    @DisplayName("싫어요가 있을 때 싫어요를 선택하면 싫어요가 해제되고 이벤트가 발행되지 않는다")
    void toggleReaction_ExistingDislike_RemoveDislike() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());
        testSupport.save(
            LiveStreamingReaction.builder()
                .liveStreaming(liveStreaming)
                .user(user)
                .type(ReactionType.DISLIKE)
                .build()
        );

        // when
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.DISLIKE
        );

        // then
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isFalse();

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("좋아요를 선택한 후 해제하고 다시 선택하면 좋아요가 생성되고 이벤트가 발행된다")
    void toggleReaction_CreateLike_RemoveLike_CreateLikeAgain() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // 첫 번째: 좋아요 선택
        sut.toggleReaction(liveStreaming.getId(), user.getId(), ReactionType.LIKE);

        // 두 번째: 좋아요 해제
        sut.toggleReaction(liveStreaming.getId(), user.getId(), ReactionType.LIKE);

        // when: 세 번째: 좋아요 다시 선택
        final ReactionToggleResult result = sut.toggleReaction(
            liveStreaming.getId(),
            user.getId(),
            ReactionType.LIKE
        );

        // then
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isDisliked()).isFalse();

        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    verify(messagingTemplate, atLeast(1))
                            .convertAndSend(
                                    eq("/topic/livestreams/" + liveStreaming.getId() + "/like-count"),
                                    any(LikeCountBroadcastResponse.class)
                            );
                });
    }
}
