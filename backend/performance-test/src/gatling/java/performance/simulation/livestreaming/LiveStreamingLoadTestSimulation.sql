-- =====================================================
-- 유튜브 클론 성능 테스트용 대량 데이터 생성 스크립트
-- =====================================================
-- MySQL 8.0+ 필수 (Recursive CTE 사용)
-- 실행 전 확인사항:
--   1. 충분한 메모리 확보 (최소 4GB 권장)
--   2. innodb_buffer_pool_size 충분히 설정
--   3. max_allowed_packet 크기 확인 (기본 64MB 이상 권장)
-- =====================================================

-- =====================================================
-- 0. 테이블 생성 DDL
-- =====================================================

-- =====================================================
-- 생성된 테스트 데이터 전체 삭제
-- =====================================================
-- 외래키 체크를 임시로 비활성화하여 순서 상관없이 삭제 가능하게 함

SET FOREIGN_KEY_CHECKS = 0;

-- TRUNCATE를 사용하여 빠르게 삭제 및 AUTO_INCREMENT 초기화
TRUNCATE TABLE notification;
TRUNCATE TABLE live_streaming_reaction;
TRUNCATE TABLE live_streaming_chat;
TRUNCATE TABLE live_streaming;
TRUNCATE TABLE push_subscription;
TRUNCATE TABLE subscription;
TRUNCATE TABLE channel;
TRUNCATE TABLE users;

-- 외래키 체크 재활성화
SET FOREIGN_KEY_CHECKS = 1;

-- 삭제 확인
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users
UNION ALL
SELECT 'channel', COUNT(*) FROM channel
UNION ALL
SELECT 'subscription', COUNT(*) FROM subscription
UNION ALL
SELECT 'push_subscription', COUNT(*) FROM push_subscription
UNION ALL
SELECT 'live_streaming', COUNT(*) FROM live_streaming
UNION ALL
SELECT 'live_streaming_chat', COUNT(*) FROM live_streaming_chat
UNION ALL
SELECT 'live_streaming_reaction', COUNT(*) FROM live_streaming_reaction
UNION ALL
SELECT 'notification', COUNT(*) FROM notification;

SELECT '
=====================================================
✅ 테스트 데이터 삭제 완료!
=====================================================
모든 테이블의 데이터가 삭제되었습니다.
TRUNCATE 사용으로 AUTO_INCREMENT 값도 1로 초기화되었습니다.
=====================================================
' AS summary;

-- 기존 테이블 삭제 (주의: 모든 데이터가 삭제됩니다!)
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS push_subscription;
DROP TABLE IF EXISTS live_streaming_reaction;
DROP TABLE IF EXISTS live_streaming_chat;
DROP TABLE IF EXISTS live_streaming;
DROP TABLE IF EXISTS subscription;
DROP TABLE IF EXISTS channel;
DROP TABLE IF EXISTS users;

-- users 테이블
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    profile_image_url VARCHAR(255) NOT NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- channel 테이블
CREATE TABLE channel (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    channel_name VARCHAR(255) NOT NULL,
    description TEXT,
    profile_image_url VARCHAR(255),
    banner_image_url VARCHAR(255),
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_channel_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- subscription 테이블
CREATE TABLE subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscriber_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_subscription_subscriber FOREIGN KEY (subscriber_id) REFERENCES users(id),
    CONSTRAINT fk_subscription_channel FOREIGN KEY (channel_id) REFERENCES channel(id),
    UNIQUE KEY uk_subscriber_channel (subscriber_id, channel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- live_streaming 테이블
CREATE TABLE live_streaming (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    thumbnail_url VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_live_streaming_channel FOREIGN KEY (channel_id) REFERENCES channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- live_streaming_chat 테이블
CREATE TABLE live_streaming_chat (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_streaming_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_chat_live_streaming FOREIGN KEY (live_streaming_id) REFERENCES live_streaming(id),
    CONSTRAINT fk_chat_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- live_streaming_reaction 테이블
CREATE TABLE live_streaming_reaction (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_streaming_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_reaction_live_streaming FOREIGN KEY (live_streaming_id) REFERENCES live_streaming(id),
    CONSTRAINT fk_reaction_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_live_streaming_user (live_streaming_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- push_subscription 테이블
CREATE TABLE push_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    endpoint VARCHAR(512) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    user_agent TEXT,
    last_used_date TIMESTAMP(6) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_push_subscription_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY uk_endpoint (endpoint)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- notification 테이블
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    title VARCHAR(255),
    thumbnail_url VARCHAR(255),
    deeplink_url VARCHAR(255),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP(6) NULL,
    created_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_modified_date TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_date TIMESTAMP(6) NULL,
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SELECT '✓ 테이블 생성 완료' AS status;

-- =====================================================
-- 데이터 생성 설정
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;
SET SESSION cte_max_recursion_depth = 100000;

-- =====================================================
-- 1. Users 테이블: 100,000명 생성
-- =====================================================
INSERT INTO users (username, password, email, profile_image_url, created_date, last_modified_date)
WITH RECURSIVE user_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM user_seq WHERE n < 100000
)
SELECT
    CONCAT('loadtest', n) AS username,
    '$2a$10$dummypasswordhash1234567890123456789012345678901234' AS password,  -- BCrypt 형식
    CONCAT('loadtest', n, '@test.com') AS email,
    CONCAT('https://storage.youtube.com/profile/', n, '.jpg') AS profile_image_url,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) AS created_date,
    NOW() AS last_modified_date
FROM user_seq;

COMMIT;

SELECT '✓ Users 생성 완료: 100,000명' AS status;

-- =====================================================
-- 2. Channel 테이블: 100,000개 생성 (모든 user가 1개 보유)
-- =====================================================
INSERT INTO channel (user_id, channel_name, description, profile_image_url, banner_image_url, created_date, last_modified_date)
SELECT
    u.id AS user_id,
    CONCAT(u.username, '의 채널') AS channel_name,
    CONCAT('채널 설명 - ', u.username) AS description,
    CONCAT('https://storage.youtube.com/channel/profile/', u.id, '.jpg') AS profile_image_url,
    CONCAT('https://storage.youtube.com/channel/banner/', u.id, '.jpg') AS banner_image_url,
    u.created_date AS created_date,
    NOW() AS last_modified_date
FROM users u
WHERE u.deleted_date IS NULL;

COMMIT;

SELECT '✓ Channel 생성 완료: 100,000개' AS status;

-- =====================================================
-- 3. Subscription 테이블: 2,000,000행 생성
-- =====================================================
-- 복잡한 분포를 위해 단계별로 나누어 생성

-- 3-1. 인기 채널 10개 선정 (각 약 50,000명 구독자)
-- 첫 10개 채널을 인기 채널로 지정
-- 각 채널은 전체 유저 중 랜덤하게 약 50%를 구독자로 확보
INSERT INTO subscription (subscriber_id, channel_id, created_date, last_modified_date)
SELECT
    u.id AS subscriber_id,
    c.id AS channel_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
CROSS JOIN users u
WHERE c.id BETWEEN 1 AND 10
    AND u.id BETWEEN 1 AND 100000
    AND u.id != c.id  -- 자기 자신은 구독 불가
    AND RAND() < 0.5;  -- 50% 확률로 구독 (평균 50,000명)

COMMIT;

SELECT '✓ 인기 채널 10개 구독자 생성 완료: 약 500,000건' AS status;

-- 3-2. 중간 채널 100개 (각 1,000~5,000명 구독자)
-- 11번~110번 채널
INSERT INTO subscription (subscriber_id, channel_id, created_date, last_modified_date)
SELECT
    u.id AS subscriber_id,
    c.id AS channel_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
CROSS JOIN users u
WHERE c.id BETWEEN 11 AND 110
    AND u.id BETWEEN 1 AND 50000
    AND u.id != c.id
    AND RAND() < 0.05;  -- 약 5% 확률로 구독 (평균 2,500명 정도)

COMMIT;

SELECT '✓ 중간 채널 100개 구독자 생성 완료' AS status;

-- 3-3. 일반 채널 구독자 (랜덤하게 분산)
-- 25,000개 채널에 대해 0~50명 정도의 구독자 (약 1,250,000건)
INSERT INTO subscription (subscriber_id, channel_id, created_date, last_modified_date)
SELECT
    u.id AS subscriber_id,
    c.id AS channel_id,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
CROSS JOIN users u
WHERE c.id BETWEEN 111 AND 25110
    AND u.id BETWEEN 1 AND 100000
    AND u.id != c.id
    AND RAND() < 0.0005;  -- 매우 낮은 확률 (평균 50명 정도)

COMMIT;

SELECT '✓ Subscription 생성 완료: 총 2,000,000건 이상' AS status;

-- =====================================================
-- 4. PushSubscription 테이블: 50,000건 생성
-- =====================================================
-- 인기 채널 10개의 구독자 중 각 5,000명씩 웹푸시 구독 (총 50,000건)

INSERT INTO push_subscription (user_id, endpoint, p256dh, auth, user_agent, active, created_date, last_modified_date)
SELECT
    s.subscriber_id AS user_id,
    CONCAT('https://fcm.googleapis.com/fcm/send/', UUID()) AS endpoint,
    'BMryPRbvNfLNEAGy2KgXMhMCFpVUkk9dT4LmKW5T0Z8zQm5nZ8y3MjA4NjE2NTczODk0' AS p256dh,  -- 더미 Base64
    'ExampleAuthSecret123456' AS auth,  -- 더미 Auth
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36' AS user_agent,
    TRUE AS active,
    NOW() AS created_date,
    NOW() AS last_modified_date
FROM (
    SELECT
        subscriber_id,
        channel_id,
        ROW_NUMBER() OVER (PARTITION BY channel_id ORDER BY RAND()) AS rn
    FROM subscription
    WHERE channel_id BETWEEN 1 AND 10
) s
WHERE s.rn <= 5000;

COMMIT;

SELECT '✓ PushSubscription 생성 완료: 50,000건' AS status;

-- =====================================================
-- 5. LiveStreaming 테이블: 1,111개 생성
-- =====================================================

-- 5-0. 성능 테스트용 LIVE 상태 라이브 스트리밍 생성 (총 1개)
-- 인기 채널 1번에 생성 (약 50,000명의 구독자 보유, AUTO_INCREMENT로 id=1 할당됨)
INSERT INTO live_streaming (channel_id, title, description, thumbnail_url, status, created_date, last_modified_date)
SELECT
    c.id AS channel_id,
    '🔴 LIVE: 성능 테스트용 라이브 스트리밍' AS title,
    '50,000명 동시 접속 성능 테스트를 위한 라이브 방송입니다.' AS description,
    'https://storage.youtube.com/thumbnail/performance-test-live.jpg' AS thumbnail_url,
    'LIVE' AS status,
    NOW() AS created_date,
    NOW() AS last_modified_date
FROM channel c
WHERE c.id = 1;

COMMIT;

SELECT '✓ LIVE 상태 라이브 스트리밍 생성 완료: 1개 (id=1)' AS status;

-- 5-1. 인기 채널 10개: 각 1개씩 과거 라이브 (총 10개)
INSERT INTO live_streaming (channel_id, title, description, thumbnail_url, status, created_date, last_modified_date)
SELECT
    c.id AS channel_id,
    CONCAT('인기 라이브 스트리밍 #', c.id) AS title,
    CONCAT('인기 채널 ', c.id, '의 라이브 방송') AS description,
    CONCAT('https://storage.youtube.com/thumbnail/', c.id, '.jpg') AS thumbnail_url,
    'ENDED' AS status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
WHERE c.id BETWEEN 1 AND 10;

COMMIT;

-- 5-2. 중간 채널 100개: 각 1개씩 과거 라이브 (총 100개)
INSERT INTO live_streaming (channel_id, title, description, thumbnail_url, status, created_date, last_modified_date)
SELECT
    c.id AS channel_id,
    CONCAT('중간 라이브 스트리밍 #', c.id) AS title,
    CONCAT('중간 채널 ', c.id, '의 라이브 방송') AS description,
    CONCAT('https://storage.youtube.com/thumbnail/', c.id, '.jpg') AS thumbnail_url,
    'ENDED' AS status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
WHERE c.id BETWEEN 11 AND 110;

COMMIT;

-- 5-3. 일반 채널 1,000개: 각 1개씩 과거 라이브 (총 1,000개)
INSERT INTO live_streaming (channel_id, title, description, thumbnail_url, status, created_date, last_modified_date)
SELECT
    c.id AS channel_id,
    CONCAT('일반 라이브 스트리밍 #', c.id) AS title,
    CONCAT('일반 채널 ', c.id, '의 라이브 방송') AS description,
    CONCAT('https://storage.youtube.com/thumbnail/', c.id, '.jpg') AS thumbnail_url,
    'ENDED' AS status,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY) AS created_date,
    NOW() AS last_modified_date
FROM channel c
WHERE c.id BETWEEN 111 AND 1110;

COMMIT;

SELECT '✓ LiveStreaming 생성 완료: 1,111개 (ENDED 상태)' AS status;

-- =====================================================
-- 6. LiveStreamingChat 테이블: 300,000건 생성
-- =====================================================

-- 6-1. 인기 라이브 10개: 각 10,000건 채팅 (총 100,000건)
INSERT INTO live_streaming_chat (live_streaming_id, user_id, message, message_type, created_date, last_modified_date)
WITH RECURSIVE chat_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM chat_seq WHERE n < 10000
)
SELECT
    ls.id AS live_streaming_id,
    (FLOOR(RAND() * 100000) + 1) AS user_id,  -- 랜덤 유저
    CONCAT('채팅 메시지 #', cs.n, ' - 라이브 ', ls.id) AS message,
    'CHAT' AS message_type,
    DATE_ADD(ls.created_date, INTERVAL FLOOR(RAND() * 3600) SECOND) AS created_date,
    NOW() AS last_modified_date
FROM chat_seq cs
CROSS JOIN live_streaming ls
WHERE ls.channel_id BETWEEN 1 AND 10;

COMMIT;

SELECT '✓ 인기 라이브 채팅 생성 완료: 100,000건' AS status;

-- 6-2. 중간 라이브 100개: 각 1,000건 채팅 (총 100,000건)
INSERT INTO live_streaming_chat (live_streaming_id, user_id, message, message_type, created_date, last_modified_date)
WITH RECURSIVE chat_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM chat_seq WHERE n < 1000
)
SELECT
    ls.id AS live_streaming_id,
    (FLOOR(RAND() * 100000) + 1) AS user_id,
    CONCAT('채팅 메시지 #', cs.n, ' - 라이브 ', ls.id) AS message,
    'CHAT' AS message_type,
    DATE_ADD(ls.created_date, INTERVAL FLOOR(RAND() * 3600) SECOND) AS created_date,
    NOW() AS last_modified_date
FROM chat_seq cs
CROSS JOIN live_streaming ls
WHERE ls.channel_id BETWEEN 11 AND 110;

COMMIT;

SELECT '✓ 중간 라이브 채팅 생성 완료: 100,000건' AS status;

-- 6-3. 일반 라이브 1,000개: 각 100건 채팅 (총 100,000건)
INSERT INTO live_streaming_chat (live_streaming_id, user_id, message, message_type, created_date, last_modified_date)
WITH RECURSIVE chat_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM chat_seq WHERE n < 100
)
SELECT
    ls.id AS live_streaming_id,
    (FLOOR(RAND() * 100000) + 1) AS user_id,
    CONCAT('채팅 메시지 #', cs.n, ' - 라이브 ', ls.id) AS message,
    'CHAT' AS message_type,
    DATE_ADD(ls.created_date, INTERVAL FLOOR(RAND() * 3600) SECOND) AS created_date,
    NOW() AS last_modified_date
FROM chat_seq cs
CROSS JOIN live_streaming ls
WHERE ls.channel_id BETWEEN 111 AND 1110;

COMMIT;

SELECT '✓ 일반 라이브 채팅 생성 완료: 100,000건' AS status;

-- =====================================================
-- 7. 설정 복구 및 최종 확인
-- =====================================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- 최종 데이터 개수 확인
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users WHERE deleted_date IS NULL
UNION ALL
SELECT 'channel', COUNT(*) FROM channel WHERE deleted_date IS NULL
UNION ALL
SELECT 'subscription', COUNT(*) FROM subscription WHERE deleted_date IS NULL
UNION ALL
SELECT 'push_subscription', COUNT(*) FROM push_subscription
UNION ALL
SELECT 'live_streaming', COUNT(*) FROM live_streaming WHERE deleted_date IS NULL
UNION ALL
SELECT 'live_streaming_chat', COUNT(*) FROM live_streaming_chat WHERE deleted_date IS NULL;

SELECT '
=====================================================
✅ 성능 테스트 데이터 생성 완료!
=====================================================
- Users: 100,000명
- Channels: 100,000개
- Subscriptions: 2,000,000+ 건
- Push Subscriptions: 50,000건
- Live Streamings: 1,111개 (LIVE 1개 + ENDED 1,110개)
- Live Streaming Chats: 300,000건
=====================================================
성능 테스트용 LIVE 스트리밍: id=1 (채널 1번)
인기 채널 (1~10번): 각 50,000+ 구독자
중간 채널 (11~110번): 각 1,000~5,000 구독자
일반 채널 (111~25,110번): 0~50 구독자
=====================================================
' AS summary;


