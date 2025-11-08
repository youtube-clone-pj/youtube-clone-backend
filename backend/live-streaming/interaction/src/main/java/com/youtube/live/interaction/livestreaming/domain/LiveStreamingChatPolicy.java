package com.youtube.live.interaction.livestreaming.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiveStreamingChatPolicy {

    public static void validate(final LiveStreamingStatus liveStreamingStatus) {
        if(liveStreamingStatus != LiveStreamingStatus.LIVE) {
            throw new IllegalStateException("채팅은 라이브 방송 중에만 가능합니다");
        }
    }
}
