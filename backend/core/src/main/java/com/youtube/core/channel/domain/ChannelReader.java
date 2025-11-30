package com.youtube.core.channel.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.core.channel.exception.ChannelErrorCode;
import com.youtube.core.channel.repository.ChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChannelReader {

    private final ChannelRepository channelRepository;

    public Channel readBy(final Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new BaseException(ChannelErrorCode.CHANNEL_NOT_FOUND));
    }

    public Channel readByUserId(final Long userId) {
        return channelRepository.findByUserId(userId)
                .orElseThrow(() -> new BaseException(ChannelErrorCode.USER_CHANNEL_NOT_FOUND));
    }
}
