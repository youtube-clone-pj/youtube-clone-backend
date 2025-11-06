package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveStreamingRepository extends JpaRepository<LiveStreaming, Long> {
}
