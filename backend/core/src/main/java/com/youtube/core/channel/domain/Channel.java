package com.youtube.core.channel.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "channel")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Channel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String channelName;

    private String description;
    private String profileImageUrl;
    private String bannerImageUrl;
}
