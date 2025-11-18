package com.youtube.core.subscription.repository;

import com.youtube.core.subscription.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    boolean existsBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    @Query(value = "SELECT * FROM subscription " +
            "WHERE subscriber_id = :subscriberId " +
            "AND channel_id = :channelId " +
            "AND deleted_date IS NOT NULL",
            nativeQuery = true)
    Optional<Subscription> findDeletedBySubscriberIdAndChannelId(
            @Param("subscriberId") Long subscriberId,
            @Param("channelId") Long channelId
    );
}
