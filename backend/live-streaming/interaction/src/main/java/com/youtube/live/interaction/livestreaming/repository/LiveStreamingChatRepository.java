package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LiveStreamingChatRepository extends JpaRepository<LiveStreamingChat, java.lang.Long> {

    @Query("SELECT new com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse(" +
            "c.id, c.username, c.message, c.messageType, c.profileImageUrl, c.createdDate) " +
            "FROM LiveStreamingChat c " +
            "WHERE c.liveStreaming.id = :livestreamId " +
            "ORDER BY c.createdDate DESC")
    List<ChatMessageResponse> findByLiveStreamingIdOrderByCreatedDateDesc(
            @Param("livestreamId") final Long livestreamId,
            final Pageable pageable
    );

    @Query("SELECT new com.youtube.live.interaction.livestreaming.repository.dto.ChatMessageResponse(" +
            "c.id, c.username, c.message, c.messageType, c.profileImageUrl, c.createdDate) " +
            "FROM LiveStreamingChat c " +
            "WHERE c.liveStreaming.id = :livestreamId AND c.id > :lastChatId " +
            "ORDER BY c.id ASC")
    List<ChatMessageResponse> findNewChatsAfter(
            @Param("livestreamId") final Long livestreamId,
            @Param("lastChatId") final Long lastChatId,
            final Pageable pageable
    );
}
