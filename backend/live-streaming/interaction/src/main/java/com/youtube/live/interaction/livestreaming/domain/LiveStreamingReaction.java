package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "live_streaming_reaction",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"live_streaming_id", "user_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLRestriction("deleted_date IS NULL")
public class LiveStreamingReaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "live_streaming_id", nullable = false)
    private LiveStreaming liveStreaming;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType type;

    public void changeType(final ReactionType newType) {
        this.type = newType;
    }

    public boolean isSameType(final ReactionType type) {
        return this.type == type;
    }
}
