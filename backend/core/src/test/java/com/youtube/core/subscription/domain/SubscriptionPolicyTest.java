package com.youtube.core.subscription.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionPolicyTest {

    @Test
    @DisplayName("자기 자신의 채널을 구독하면 예외가 발생한다")
    void validateNotSelfSubscription_SelfChannel_ThrowException() {
        // given
        final User user = User().withId(1L).build();
        final Channel channel = Channel().withUser(user).build();

        // when & then
        assertThatThrownBy(() -> SubscriptionPolicy.validateNotSelfSubscription(user.getId(), channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자기 자신의 채널은 구독할 수 없습니다.");
    }

    @Test
    @DisplayName("다른 사람의 채널을 구독하면 예외가 발생하지 않는다")
    void validateNotSelfSubscription_OtherChannel_NoException() {
        // given
        final User subscriber = User().withId(1L).build();
        final User channelOwner = User().withId(2L).withEmail("owner@example.com").build();
        final Channel channel = Channel().withUser(channelOwner).build();

        // when & then
        assertThatCode(() -> SubscriptionPolicy.validateNotSelfSubscription(subscriber.getId(), channel))
                .doesNotThrowAnyException();
    }
}