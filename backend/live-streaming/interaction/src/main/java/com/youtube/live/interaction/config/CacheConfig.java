package com.youtube.live.interaction.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.youtube.live.interaction.livestreaming.domain.LiveStreamingStatus;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        final CaffeineCacheManager cacheManager = new CaffeineCacheManager("liveStreamingStatus");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfter(new Expiry<Object, Object>() {
                    @Override
                    public long expireAfterCreate(
                            final Object key,
                            final Object value,
                            final long currentTime
                    ) {
                        return getTtlByStatus(value);
                    }

                    @Override
                    public long expireAfterUpdate(
                            final Object key,
                            final Object value,
                            final long currentTime,
                            final long currentDuration
                    ) {
                        return getTtlByStatus(value);
                    }

                    @Override
                    public long expireAfterRead(
                            final Object key,
                            final Object value,
                            final long currentTime,
                            final long currentDuration
                    ) {
                        return currentDuration;
                    }

                    private long getTtlByStatus(final Object value) {
                        if (value instanceof LiveStreamingStatus) {
                            final LiveStreamingStatus status = (LiveStreamingStatus) value;
                            return switch (status) {
                                case LIVE -> TimeUnit.HOURS.toNanos(12);
                                case ENDED -> TimeUnit.MINUTES.toNanos(30);
                                case SCHEDULED -> TimeUnit.HOURS.toNanos(1);
                            };
                        }
                        return TimeUnit.HOURS.toNanos(12);
                    }
                });
    }
}
