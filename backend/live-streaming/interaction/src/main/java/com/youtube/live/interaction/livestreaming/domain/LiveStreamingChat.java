package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
        name = "live_streaming_chat",
        indexes = {
                @Index(name = "idx_livestreaming_id_deleted_date_id", columnList = "live_streaming_id, deleted_date, id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class LiveStreamingChat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "live_streaming_id", nullable = false)
    private LiveStreaming liveStreaming;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Column
    private String profileImageUrl;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatMessageType messageType;

    @Builder
    private LiveStreamingChat(Long id, LiveStreaming liveStreaming, Long userId, String username, String profileImageUrl, String message, ChatMessageType messageType) {
        LiveStreamingChatPolicy.validate(liveStreaming.getStatus());
        this.id = id;
        this.liveStreaming = liveStreaming;
        this.userId = userId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.message = message;
        this.messageType = messageType;
    }
}
