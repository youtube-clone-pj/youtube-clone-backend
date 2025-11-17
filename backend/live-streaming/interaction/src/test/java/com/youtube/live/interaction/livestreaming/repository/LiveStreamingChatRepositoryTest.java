package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.config.IntegrationTest;
import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.live.interaction.builder.LiveStreamingBuilder.LiveStreaming;
import static com.youtube.live.interaction.builder.LiveStreamingChatBuilder.LiveStreamingChat;
import static org.assertj.core.api.Assertions.assertThat;

class LiveStreamingChatRepositoryTest extends IntegrationTest {

    @Autowired
    private LiveStreamingChatRepository sut;

    @Test
    @DisplayName("createdDate가 내림차순으로 정렬된 채팅 메시지를 요청한 개수만큼 조회한다")
    void findMessagesByLiveStreamingId_OrderedByCreatedDateDesc() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        for (int i = 1; i <= 31; i++) {
            testSupport.save(
                    LiveStreamingChat()
                            .withLiveStreaming(liveStreaming)
                            .withUser(user)
                            .withMessage("메시지 " + i)
                            .build()
            );
        }

        // when
        final List<ChatMessageResponse> result = sut.findByLiveStreamingIdOrderByCreatedDateDesc(
                liveStreaming.getId(),
                PageRequest.of(0, 30)
        );

        // then
        assertThat(result).hasSize(30);
        assertThat(result.get(0).getMessage()).isEqualTo("메시지 31");
        assertThat(result.get(29).getMessage()).isEqualTo("메시지 2");
    }

    @Test
    @DisplayName("채팅 메시지가 30개 미만일 때 모든 메시지를 조회한다")
    void findMessagesByLiveStreamingId_LessThan30Chats_ReturnsAll() {
        // given
        final User user = testSupport.save(User().build());
        final Channel channel = testSupport.save(Channel().withUser(user).build());
        final LiveStreaming liveStreaming = testSupport.save(LiveStreaming().withChannel(channel).build());

        for (int i = 1; i <= 10; i++) {
            testSupport.save(
                    LiveStreamingChat()
                            .withLiveStreaming(liveStreaming)
                            .withUser(user)
                            .withMessage("메시지 " + i)
                            .build()
            );
        }

        // when
        final List<ChatMessageResponse> result = sut.findByLiveStreamingIdOrderByCreatedDateDesc(
                liveStreaming.getId(),
                PageRequest.of(0, 30)
        );

        // then
        assertThat(result).hasSize(10);
    }
}
