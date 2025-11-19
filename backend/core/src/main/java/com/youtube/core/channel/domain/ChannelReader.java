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

    public Channel readByUserId(final Long userId) {
        return channelRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저의 채널이 존재하지 않습니다"));
    }
}
