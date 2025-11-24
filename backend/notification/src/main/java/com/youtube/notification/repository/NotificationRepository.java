package com.youtube.notification.repository;

import com.youtube.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n
        WHERE n.receiver.id = :receiverId
        AND (:cursor IS NULL OR n.id < :cursor)
        AND n.createdDate >= :createdAfter
        ORDER BY n.id DESC
        """)
    List<Notification> findByReceiverIdWithCursor(
            @Param("receiverId") Long receiverId,
            @Param("cursor") Long cursor,
            @Param("createdAfter") Instant createdAfter,
            Pageable pageable
    );
}
