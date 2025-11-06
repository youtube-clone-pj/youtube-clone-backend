package com.youtube.api.auth;

import com.youtube.api.config.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AuthServiceTest extends IntegrationTest {

    @Autowired
    private AuthService sut;

    @Test
    @DisplayName("하나의 이메일로는 하나의 계정만 생성할 수 있다")
    void nonDuplicatedEmail() throws InterruptedException {
        // given
        final int threadCount = 5;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    latch.countDown();
                    latch.await();

                    sut.signUp(new RegisterRequest(
                            "testname" + index,
                            "test@test.com",
                            "testpassword",
                            "https://example.com/profile.jpg"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(4);
    }

}