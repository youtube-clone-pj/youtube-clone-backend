package com.youtube.live.interaction.builder;

import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.domain.ChatMessageType;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChat;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiveStreamingChatBuilder {

    private Long id;
    private LiveStreaming liveStreaming;
    private User user;
    private String message = "테스트 채팅 메시지";
    private ChatMessageType messageType = ChatMessageType.CHAT;

    public static LiveStreamingChatBuilder LiveStreamingChat() {
        return new LiveStreamingChatBuilder();
    }

    public LiveStreamingChat build() {
        return LiveStreamingChat.builder()
                .id(this.id)
                .liveStreaming(this.liveStreaming)
                .userId(this.user.getId())
                .username(this.user.getUsername())
                .profileImageUrl(this.user.getProfileImageUrl())
                .message(this.message)
                .messageType(this.messageType)
                .build();
    }
}
