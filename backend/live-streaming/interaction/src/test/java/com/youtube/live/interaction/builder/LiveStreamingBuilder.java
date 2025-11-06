package com.youtube.live.interaction.builder;

import com.youtube.core.channel.domain.Channel;
import com.youtube.live.interaction.livestreaming.domain.LiveStreaming;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LiveStreamingBuilder {

    private Long id;
    private Channel channel;
    private String title = "테스트 라이브 스트리밍";
    private String description = "테스트 라이브 스트리밍 설명";
    private String thumbnailUrl = "https://example.com/thumbnail.jpg";
    private LiveStreamingStatus status = LiveStreamingStatus.SCHEDULED;

    public static LiveStreamingBuilder LiveStreaming() {
        return new LiveStreamingBuilder();
    }

    public LiveStreaming build() {
        return LiveStreaming.builder()
            .id(this.id)
            .channel(this.channel)
            .title(this.title)
            .description(this.description)
            .thumbnailUrl(this.thumbnailUrl)
            .status(this.status)
            .build();
    }
}
