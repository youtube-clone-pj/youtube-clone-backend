package com.youtube.core.channel.domain;

import com.youtube.core.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelReader {

    private final ChannelRepository channelRepository;

    public Channel readBy(final Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채널입니다"));
    }
}
