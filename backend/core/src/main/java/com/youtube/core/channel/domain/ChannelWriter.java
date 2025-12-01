package com.youtube.core.channel.domain;

import com.youtube.core.channel.repository.ChannelRepository;
import com.youtube.core.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChannelWriter {

    private final ChannelRepository channelRepository;

    public Long write(final User user, final String channelName) {
        final Long newChannelId = channelRepository.save(
                Channel.builder()
                        .user(user)
                        .channelName(channelName)
                        .build()
        ).getId();

        log.info("Channel 생성 완료 - channelId: {}, userId: {}", newChannelId, user.getId());

        return newChannelId;
    }
}
