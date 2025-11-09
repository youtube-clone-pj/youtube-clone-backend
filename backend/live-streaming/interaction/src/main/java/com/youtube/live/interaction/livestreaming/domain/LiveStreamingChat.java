package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "live_streaming_chat")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LiveStreamingChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_streaming_id", nullable = false)
    private LiveStreaming liveStreaming;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatMessageType messageType;

    @Builder
    private LiveStreamingChat(Long id, LiveStreaming liveStreaming, User user, String message, ChatMessageType messageType) {
        LiveStreamingChatPolicy.validate(liveStreaming.getStatus());
        this.id = id;
        this.liveStreaming = liveStreaming;
        this.user = user;
        this.message = message;
        this.messageType = messageType;
    }
}
