package com.youtube.notification.domain;

import com.youtube.core.user.domain.User;
import com.youtube.notification.config.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.youtube.core.testfixtures.builder.UserBuilder.User;
import static com.youtube.notification.testfixtures.builder.PushSubscriptionBuilder.PushSubscription;
import static org.assertj.core.api.Assertions.assertThat;

class PushSubscriptionWriterTest extends IntegrationTest {

    @Autowired
    private PushSubscriptionWriter sut;

    @Test
    @DisplayName("존재하는 구독을 삭제하면 DB에서 삭제된다")
    void hardDelete_ExistingSubscription_DeleteFromDatabase() {
        // given
        final User user = testSupport.save(User().build());
        final PushSubscription subscription = testSupport.save(
                PushSubscription()
                        .withUser(user)
                        .build()
        );
        final Long subscriptionId = subscription.getId();

        // when
        sut.hardDelete(subscriptionId);

        // then
        final PushSubscription deletedSubscription = testSupport.jpaQueryFactory
                .selectFrom(QPushSubscription.pushSubscription)
                .where(QPushSubscription.pushSubscription.id.eq(subscriptionId))
                .fetchOne();

        assertThat(deletedSubscription).isNull();
    }
}