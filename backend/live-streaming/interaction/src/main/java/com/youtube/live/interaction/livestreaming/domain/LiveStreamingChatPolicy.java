package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.common.exception.BaseException;
import com.youtube.live.interaction.exception.LiveStreamingErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiveStreamingChatPolicy {

    public static void validate(final LiveStreamingStatus liveStreamingStatus) {
        if(liveStreamingStatus != LiveStreamingStatus.LIVE) {
            throw new BaseException(LiveStreamingErrorCode.CHAT_NOT_ALLOWED_WHEN_OFFLINE);
        }
    }
}
