package com.youtube.live.interaction.livestreaming.repository;

import com.youtube.live.interaction.livestreaming.domain.LiveStreamingChat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveStreamingChatRepository extends JpaRepository<LiveStreamingChat, java.lang.Long> {
}
