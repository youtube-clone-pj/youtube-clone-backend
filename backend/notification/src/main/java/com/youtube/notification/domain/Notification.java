package com.youtube.notification.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationTargetType targetType;

    /**
     * 알림이 가리키는 대상 리소스의 ID
     * SYSTEM 알림 등의 경우 null일 수 있음
     */
    private Long targetId;

    private String title;
    private String thumbnailUrl;

    /**
     * 알림 클릭 시 이동할 딥링크
     * 예: "/live/{id}", "/watch/{id}", "/channel/{id}"
     */
    private String deeplinkUrl;

    @Column(nullable = false)
    private boolean isRead;

    private Instant readAt;
}
