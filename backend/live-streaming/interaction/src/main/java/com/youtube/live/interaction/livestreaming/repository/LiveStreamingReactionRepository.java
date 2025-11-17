package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReaction;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LiveStreamingReactionRepository extends JpaRepository<LiveStreamingReaction, Long> {

    Optional<LiveStreamingReaction> findByLiveStreamingIdAndUserId(Long liveStreamingId, Long userId);

    long countByLiveStreamingIdAndType(Long liveStreamingId, ReactionType type);
    
    @Query(value = "SELECT * FROM live_streaming_reaction " +
            "WHERE live_streaming_id = :liveStreamingId " +
            "AND user_id = :userId " +
            "AND deleted_date IS NOT NULL",
            nativeQuery = true)
    Optional<LiveStreamingReaction> findDeletedByLiveStreamingIdAndUserId(
            @Param("liveStreamingId") Long liveStreamingId,
            @Param("userId") Long userId
    );
}
