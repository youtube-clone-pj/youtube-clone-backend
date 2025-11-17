package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LiveStreamingChatRepository extends JpaRepository<LiveStreamingChat, java.lang.Long> {

    @Query("SELECT new com.youtube.live.interaction.livestreaming.controller.dto.ChatMessageResponse(" +
            "u.username, c.message, c.messageType, u.profileImageUrl, c.createdDate) " +
            "FROM LiveStreamingChat c " +
            "JOIN c.user u " +
            "WHERE c.liveStreaming.id = :livestreamId " +
            "ORDER BY c.createdDate DESC")
    List<ChatMessageResponse> findByLiveStreamingIdOrderByCreatedDateDesc(
            @Param("livestreamId") final Long livestreamId,
            final Pageable pageable
    );
}
