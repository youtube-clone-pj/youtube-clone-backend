package com.youtube.core.subscription.repository;

import com.youtube.core.subscription.domain.Subscription;
import com.youtube.core.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    boolean existsBySubscriberIdAndChannelId(Long subscriberId, Long channelId);

    List<Subscription> findByChannelId(Long channelId);

    @Query(value = "SELECT * FROM subscription " +
            "WHERE subscriber_id = :subscriberId " +
            "AND channel_id = :channelId " +
            "AND deleted_date IS NOT NULL",
            nativeQuery = true)
    Optional<Subscription> findDeletedBySubscriberIdAndChannelId(
            @Param("subscriberId") Long subscriberId,
            @Param("channelId") Long channelId
    );

    @Query("SELECT u FROM User u " +
            "JOIN Subscription s ON s.subscriber.id = u.id " +
            "WHERE s.channel.id = :channelId")
    List<User> findSubscribersByChannelId(@Param("channelId") final Long channelId);
}
