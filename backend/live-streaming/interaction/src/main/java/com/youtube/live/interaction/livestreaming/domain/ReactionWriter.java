package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.live.interaction.livestreaming.repository.LiveStreamingReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReactionWriter {

    private final LiveStreamingReactionRepository liveStreamingReactionRepository;

    public LiveStreamingReaction write(final LiveStreamingReaction reaction) {
        return liveStreamingReactionRepository.save(reaction);
    }

    /**
     * 주의: 이 메서드는 반드시 트랜잭션 범위 내에서 호출되어야 하며,
     * reaction 파라미터는 영속 상태(managed)여야 합니다.
     */
    public void remove(final LiveStreamingReaction reaction) {
        //TODO 글로벌 시간 저장 필요
        reaction.softDelete(LocalDateTime.now());
    }
}
