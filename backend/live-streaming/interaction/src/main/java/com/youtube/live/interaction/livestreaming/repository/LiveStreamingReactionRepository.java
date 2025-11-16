package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingReaction;
import com.youtube.live.interaction.livestreaming.domain.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LiveStreamingReactionRepository extends JpaRepository<LiveStreamingReaction, Long> {

    Optional<LiveStreamingReaction> findByLiveStreamingIdAndUserId(Long liveStreamingId, Long userId);

    long countByLiveStreamingIdAndType(Long liveStreamingId, ReactionType type);
}
