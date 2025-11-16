package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.channel.domain.Channel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "live_streaming")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@SQLRestriction("deleted_date IS NULL")
public class LiveStreaming extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(nullable = false)
    private String title;

    private String description;
    private String thumbnailUrl;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LiveStreamingStatus status;
}
