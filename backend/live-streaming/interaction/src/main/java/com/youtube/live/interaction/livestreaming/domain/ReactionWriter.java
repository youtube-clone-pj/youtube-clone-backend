package com.youtube.live.interaction.livestreaming.domain;

import com.youtube.core.user.domain.User;
import com.youtube.live.interaction.livestreaming.repository.LiveStreamingReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactionWriter {

    private final LiveStreamingReactionRepository liveStreamingReactionRepository;
    private final ReactionReader reactionReader;

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
        log.info("LiveStreamingReaction 삭제 - liveStreamingId: {}, userId: {}, type: {}",
                reaction.getLiveStreaming().getId(), reaction.getUser().getId(), reaction.getType());
    }

    @Transactional
    public boolean restoreAndChangeType(final Long liveStreamingId, final Long userId, final ReactionType type) {
        return reactionReader.readDeletedBy(liveStreamingId, userId)
                .map(reaction -> {
                    reaction.restore();
                    reaction.changeType(type);
                    log.info("삭제된 LiveStreamingReaction 복원 - liveStreamingId: {}, userId: {}, type: {}",
                            liveStreamingId, userId, type);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 리액션 토글 프로세스를 처리합니다.
     * 기존 리액션이 있으면 타입을 토글하고, 없으면 생성 또는 복원합니다.
     *
     * 주의: 이 메서드는 반드시 트랜잭션 범위 내에서 호출되어야 합니다.
     *
     * @return 변경 후의 타입 (삭제된 경우 null)
     */
    public ReactionType processToggle(
            final LiveStreaming liveStreaming,
            final User user,
            final ReactionType requestType
    ) {
        final Optional<LiveStreamingReaction> existingReaction = reactionReader.readBy(liveStreaming.getId(), user.getId());
        if (existingReaction.isEmpty()) {
            return handleNoExistingReaction(liveStreaming, user, requestType);
        }

        return toggleType(existingReaction.get(), requestType);
    }

    /**
     * Reaction의 타입을 토글합니다.
     * 요청한 타입과 같으면 삭제, 다르면 타입을 변경합니다.
     *
     * 주의: 이 메서드는 반드시 트랜잭션 범위 내에서 호출되어야 하며,
     * reaction 파라미터는 영속 상태(managed)여야 합니다.
     *
     * @param reaction 토글할 reaction (영속 상태여야 함)
     * @return 변경 후의 타입 (삭제된 경우 null)
     */
    public ReactionType toggleType(final LiveStreamingReaction reaction, final ReactionType requestType) {
        if (reaction.isSameType(requestType)) {
            remove(reaction);
            return null;
        }
        final ReactionType previousType = reaction.getType();
        reaction.changeType(requestType);
        log.info("LiveStreamingReaction 타입 변경 - liveStreamingId: {}, userId: {}, {} -> {}",
                reaction.getLiveStreaming().getId(), reaction.getUser().getId(), previousType, requestType);
        return requestType;
    }

    private ReactionType handleNoExistingReaction(
            final LiveStreaming liveStreaming,
            final User user,
            final ReactionType requestType
    ) {
        if (restoreAndChangeType(liveStreaming.getId(), user.getId(), requestType)) {
            return requestType;
        }

        createReaction(liveStreaming, user, requestType);
        return requestType;
    }

    private void createReaction(final LiveStreaming liveStreaming, final User user, final ReactionType type) {
        write(LiveStreamingReaction.builder()
                .liveStreaming(liveStreaming)
                .user(user)
                .type(type)
                .build()
        );
        log.info("LiveStreamingReaction 생성 - liveStreamingId: {}, userId: {}, type: {}",
                liveStreaming.getId(), user.getId(), type);
    }
}
