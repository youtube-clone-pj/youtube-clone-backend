package performance.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/**
 * 채팅 테스트 데이터 Feeder 생성 유틸리티
 * <p>
 * WebSocket 채팅 성능 테스트에 필요한 채팅 행동 패턴과 메시지 데이터를 생성합니다.
 */
public class ChatTestDataFeeder {

    /**
     * 채팅 행동 패턴 Feeder 생성
     * <p>
     * 각 가상 사용자(VU)마다 다른 채팅 행동 패턴을 랜덤하게 생성합니다.
     * <p>
     * 초기 활발한 채팅은 5분(300초) 동안 진행되며,
     * 나머지 시간은 안정화된 채팅으로 진행됩니다.
     * <p>
     * 제공되는 키:
     * - sessionDuration: 전체 세션 시간 (초)
     * - normalChatDuration: 안정화된 채팅 시간 (초) = sessionDuration - 300
     *
     * @param minSessionDuration 최소 세션 시간 (초, 최소 300초 이상 권장)
     * @param maxSessionDuration 최대 세션 시간 (초)
     * @return 무한 Iterator (sessionDuration, normalChatDuration 키로 제공)
     */
    public static Iterator<Map<String, Object>> createChatBehaviorFeeder(
            final int minSessionDuration,
            final int maxSessionDuration
    ) {
        return Stream.generate(() -> {
            final int sessionDuration = ThreadLocalRandom.current()
                    .nextInt(minSessionDuration, maxSessionDuration + 1);

            // 안정화된 채팅 시간 = 전체 세션 시간 - 초기 활발한 채팅 시간(5분)
            final int normalChatDuration = Math.max(0, sessionDuration - 300);

            return Map.<String, Object>of(
                    "sessionDuration", sessionDuration,
                    "normalChatDuration", normalChatDuration
            );
        }).iterator();
    }

    /**
     * 초기 활발한 채팅 메시지 Feeder 생성
     * <p>
     * 라이브 스트리밍 시작 직후 사용자들이 전송하는 짧고 간단한 메시지들입니다.
     * <p>
     * 메시지 특징:
     * - 짧은 길이 (1~5자)
     * - 높은 전송 빈도
     * - 단순한 반응 표현
     *
     * @return 무한 Iterator (chatMessage 키로 제공)
     */
    public static Iterator<Map<String, Object>> createInitialChatMessageFeeder() {
        final String[] initialMessages = {
                "ㄱㄱㄱㄱ",
                "시작!",
                "왔다!",
                "하이",
                "오!",
                "ㅋㅋㅋ",
                "ㅎㅇ",
                "킹갓",
                "레츠고",
                "고고",
                "!!",
                "왔어요",
                "시작했네",
                "드디어",
                "ㅇㅇ"
        };

        return Stream.generate(() -> {
            final String message = initialMessages[
                    ThreadLocalRandom.current().nextInt(initialMessages.length)
                    ];
            return Map.<String, Object>of("chatMessage", message);
        }).iterator();
    }

    /**
     * 안정화된 채팅 메시지 Feeder 생성
     * <p>
     * 라이브 스트리밍이 진행되면서 사용자들이 전송하는 일반적인 채팅 메시지들입니다.
     * <p>
     * 메시지 특징:
     * - 다양한 길이
     * - 낮은 전송 빈도
     * - 정상적인 대화 패턴
     *
     * @return 무한 Iterator (chatMessage 키로 제공)
     */
    public static Iterator<Map<String, Object>> createNormalChatMessageFeeder() {
        final String[] normalMessages = {
                "재밌네요",
                "좋아요",
                "ㅋㅋㅋㅋ",
                "오늘 방송 재밌어요",
                "굿굿",
                "최고",
                "잘보고 있어요",
                "오 이거 좋은데",
                "역시",
                "대박",
                "ㅇㅈ",
                "인정",
                "ㄹㅇ",
                "그러게요",
                "맞아요",
                "응원합니다",
                "화이팅",
                "잘하시네요",
                "멋져요",
                "👍",
                "ㅎㅎㅎ",
                "오 신기하다",
                "와",
                "대단하네"
        };

        return Stream.generate(() -> {
            final String message = normalMessages[
                    ThreadLocalRandom.current().nextInt(normalMessages.length)
                    ];
            return Map.<String, Object>of("chatMessage", message);
        }).iterator();
    }

    private ChatTestDataFeeder() {
        // 유틸리티 클래스: 인스턴스화 방지
    }
}