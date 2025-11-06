package com.youtube.core.testfixtures.builder;

import com.youtube.core.user.domain.User;
import com.youtube.core.channel.domain.Channel;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.With;

@With
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChannelBuilder {

    private Long id;
    private User user;
    private String channelName = "테스트 채널";
    private String description = "테스트 채널 설명";
    private String profileImageUrl = "https://example.com/profile.jpg";
    private String bannerImageUrl = "https://example.com/banner.jpg";

    public static ChannelBuilder Channel() {
        return new ChannelBuilder();
    }

    public Channel build() {
        return Channel.builder()
            .id(this.id)
            .user(this.user)
            .channelName(this.channelName)
            .description(this.description)
            .profileImageUrl(this.profileImageUrl)
            .bannerImageUrl(this.bannerImageUrl)
            .build();
    }
}
