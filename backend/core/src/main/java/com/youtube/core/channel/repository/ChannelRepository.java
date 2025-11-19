package com.youtube.core.channel.repository;

import com.youtube.core.channel.domain.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    Optional<Channel> findByUserId(final Long userId);
}
