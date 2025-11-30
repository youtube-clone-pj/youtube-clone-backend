package com.youtube.notification.domain;

import com.youtube.core.common.BaseEntity;
import com.youtube.core.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Web Push Protocol(RFC 8030, RFC 8291)을 사용한 푸시 알림 구독 정보를 관리하는 엔티티.
 * 클라이언트(브라우저)가 푸시 알림을 구독할 때 생성되며, 서버가 해당 클라이언트에게
 * 암호화된 푸시 메시지를 전송하는 데 필요한 정보를 저장합니다.
 */
@Entity
@Table(
        name = "push_subscription",
        uniqueConstraints = @UniqueConstraint(columnNames = "endpoint")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 푸시 서비스의 고유 엔드포인트 URL.
     * 서버가 이 URL로 푸시 메시지를 전송하면, 푸시 서비스가 클라이언트에게 전달합니다.
     * 각 구독마다 고유한 값을 가지며, 구독을 식별하는 데 사용됩니다.
     */
    @Column(nullable = false)
    private String endpoint;

    /**
     * 클라이언트의 공개키 (P-256 Elliptic Curve Diffie-Hellman Public Key).
     * Base64로 인코딩된 형태로 저장됩니다.
     * 서버는 이 공개키를 사용하여 ECDH 키 교환을 통해 공유 비밀을 생성하고,
     * 이를 기반으로 푸시 메시지를 AES-GCM 대칭 암호화합니다.
     */
    @Column(nullable = false)
    private String p256dh;

    /**
     * 인증 시크릿 (Authentication Secret).
     * 클라이언트가 생성한 랜덤 16바이트 값으로, Base64로 인코딩되어 저장됩니다.
     * p256dh와 함께 사용되어 암호화 키를 파생하고, 메시지의 무결성을 보장합니다.
     */
    @Column(nullable = false)
    private String auth;

    /**
     * 구독한 클라이언트의 User-Agent 문자열.
     * 어떤 브라우저/기기에서 구독했는지 추적하고, 디버깅 및 문제 해결에 사용됩니다.
     * 예: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0"
     */
    private String userAgent;

    /**
     * 해당 구독을 마지막으로 사용한 시간.
     * 푸시 메시지 전송 성공 시 업데이트되며, 비활성 구독을 식별하는 데 사용됩니다.
     */
    private Instant lastUsedDate;

    /**
     * 구독의 활성화 상태.
     * 로그인 시 활성화되고, 로그아웃 시 비활성화됩니다.
     * 알림은 활성화된 구독에만 전송됩니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public void updateLastUsedDate() {
        this.lastUsedDate = Instant.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
