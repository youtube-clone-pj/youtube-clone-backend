package com.youtube.core.subscription.domain;

import com.youtube.core.channel.domain.Channel;
import com.youtube.core.config.IntegrationTest;
import com.youtube.core.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static com.youtube.core.testfixtures.builder.ChannelBuilder.Channel;
import static com.youtube.core.testfixtures.builder.SubscriptionBuilder.Subscription;
import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static org.assertj.core.api.Assertions.assertThatCode;

class SubscriptionWriterTest extends IntegrationTest {

    @Autowired
    private SubscriptionWriter sut;

    @Test
    @DisplayName("구독하지 않은 채널을 구독하면, 새로운 구독이 생성된다")
    void subscribe_NewChannel_CreateSubscription() {
        // given
        final User subscriber = testSupport.save(User().build());
        final User channelOwner = testSupport.save(User().withEmail("owner@example.com").build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());

        // when & then
        assertThatCode(() -> sut.subscribe(subscriber, channel))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("구독 중인 채널을 구독 해제한다")
    void unsubscribe_SubscribedChannel() {
        // given
        final User subscriber = testSupport.save(User().build());
        final User channelOwner = testSupport.save(User().withEmail("owner@example.com").build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());
        testSupport.save(Subscription().withSubscriber(subscriber).withChannel(channel).build());

        // when & then
        assertThatCode(() -> sut.unsubscribe(subscriber.getId(), channel.getId(), Instant.now()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("삭제된 구독을 다시 구독하면, 구독이 복원된다")
    void subscribe_DeletedSubscription_Restore() {
        // given
        final User subscriber = testSupport.save(User().build());
        final User channelOwner = testSupport.save(User().withEmail("owner@example.com").build());
        final Channel channel = testSupport.save(Channel().withUser(channelOwner).build());
        sut.subscribe(subscriber, channel);
        sut.unsubscribe(subscriber.getId(), channel.getId(), Instant.now());

        // when & then
        assertThatCode(() -> sut.subscribe(subscriber, channel))
                .doesNotThrowAnyException();
    }
}
