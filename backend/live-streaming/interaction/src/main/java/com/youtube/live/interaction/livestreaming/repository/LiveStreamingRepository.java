package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LiveStreamingRepository extends JpaRepository<LiveStreaming, Long> {

    @Query("SELECT ls.status FROM LiveStreaming ls WHERE ls.id = :liveStreamingId")
    Optional<LiveStreamingStatus> findStatusById(@Param("liveStreamingId") final Long liveStreamingId);

    @Query("""
    SELECT new com.youtube.live.interaction.livestreaming.repository.dto.LiveStreamingMetadataResponse(
        c.id, c.channelName, c.profileImageUrl, ls.title, ls.description, ls.createdDate, COUNT(s.id)
    )
    FROM LiveStreaming ls
    JOIN ls.channel c
    LEFT JOIN Subscription s ON s.channel.id = c.id
    WHERE ls.id = :liveStreamingId
    GROUP BY c.id, c.channelName, c.profileImageUrl,
             ls.title, ls.description, ls.createdDate
    """)
    LiveStreamingMetadataResponse findMetadataById(@Param("liveStreamingId") final Long liveStreamingId);
}
