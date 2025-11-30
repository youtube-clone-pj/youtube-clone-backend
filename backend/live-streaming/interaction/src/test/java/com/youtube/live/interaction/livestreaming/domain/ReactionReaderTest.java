package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.livestreaming.service.dto.ReactionToggleResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static org.assertj.core.api.Assertions.assertThat;

class ReactionReaderTest extends IntegrationTest {

    @Autowired
    private ReactionReader sut;

    @Test
    @DisplayName("유저의 반응이 없으면 반응 타입이 null인 결과를 반환한다")
    void readUserReactionWithNoReaction() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        // when
        final ReactionToggleResult result = sut.readUserReaction(liveStreaming.getId(), user.getId());

        // then
        assertThat(result.reactionType()).isNull();
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isFalse();
    }

    @Test
    @DisplayName("유저가 좋아요를 선택했으면 LIKE 타입의 결과를 반환한다")
    void readUserReactionWithLike() {
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
        final ReactionToggleResult result = sut.readUserReaction(liveStreaming.getId(), user.getId());

        // then
        assertThat(result.reactionType()).isEqualTo(ReactionType.LIKE);
        assertThat(result.isLiked()).isTrue();
        assertThat(result.isDisliked()).isFalse();
    }

    @Test
    @DisplayName("유저가 싫어요를 선택했으면 DISLIKE 타입의 결과를 반환한다")
    void readUserReactionWithDislike() {
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
        final ReactionToggleResult result = sut.readUserReaction(liveStreaming.getId(), user.getId());

        // then
        assertThat(result.reactionType()).isEqualTo(ReactionType.DISLIKE);
        assertThat(result.isLiked()).isFalse();
        assertThat(result.isDisliked()).isTrue();
    }
}