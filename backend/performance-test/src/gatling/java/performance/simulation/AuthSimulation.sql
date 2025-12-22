-- =====================================================
-- AuthSimulation 성능 테스트용 최소 데이터 생성 스크립트
-- =====================================================
-- MySQL 8.0+ 필수 (Recursive CTE 사용)
-- AuthSimulation에 필요한 데이터만 생성:
--   - Users: 10,000명
--   - Channels: 10,000개 (각 유저당 1개)
--   - LiveStreaming: 100개 (메타데이터 조회 테스트용)
-- =====================================================

-- =====================================================
-- 0. 기존 데이터 삭제
-- =====================================================

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

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✓ 기존 데이터 삭제 완료' AS status;

-- =====================================================
-- 1. 테이블 생성 DDL
-- =====================================================

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
-- 2. 데이터 생성 설정
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;
SET UNIQUE_CHECKS = 0;
SET AUTOCOMMIT = 0;
SET SESSION cte_max_recursion_depth = 10000;

-- =====================================================
-- 3. Users 테이블: 10,000명 생성
-- =====================================================
-- TestDataFeeder 형식에 맞춰 생성:
--   - username: loadtest1, loadtest2, ...
--   - email: loadtest1@test.com, loadtest2@test.com, ...
--   - password: password123 (BCrypt 해시)
INSERT INTO users (username, password, email, profile_image_url, created_date, last_modified_date)
WITH RECURSIVE user_seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM user_seq WHERE n < 10000
)
SELECT
    CONCAT('loadtest', n) AS username,
    '$2a$10$KieOfGco1z20nuGgShKBo.ogORddlqdqJu8yfQZhEyAvaGEVZ2nY2' AS password,  -- password123
    CONCAT('loadtest', n, '@test.com') AS email,
    CONCAT('https://storage.youtube.com/profile/', n, '.jpg') AS profile_image_url,
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY) AS created_date,
    NOW() AS last_modified_date
FROM user_seq;

COMMIT;

SELECT '✓ Users 생성 완료: 10,000명' AS status;

-- =====================================================
-- 4. Channel 테이블: 10,000개 생성 (모든 user가 1개 보유)
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

SELECT '✓ Channel 생성 완료: 10,000개' AS status;

-- =====================================================
-- 5. LiveStreaming 테이블: 100개 생성 (메타데이터 조회 테스트용)
-- =====================================================

-- 첫 100개 채널에 각 1개의 LIVE 상태 스트리밍 생성
INSERT INTO live_streaming (channel_id, title, description, thumbnail_url, status, created_date, last_modified_date)
SELECT
    c.id AS channel_id,
    CONCAT('테스트 라이브 스트리밍 #', c.id) AS title,
    CONCAT('채널 ', c.id, '의 라이브 방송') AS description,
    CONCAT('https://storage.youtube.com/thumbnail/', c.id, '.jpg') AS thumbnail_url,
    'LIVE' AS status,
    NOW() AS created_date,
    NOW() AS last_modified_date
FROM channel c
WHERE c.id BETWEEN 1 AND 100;

COMMIT;

SELECT '✓ LiveStreaming 생성 완료: 100개' AS status;

-- =====================================================
-- 6. 설정 복구 및 최종 확인
-- =====================================================
SET FOREIGN_KEY_CHECKS = 1;
SET UNIQUE_CHECKS = 1;
SET AUTOCOMMIT = 1;

-- 최종 데이터 개수 확인
SELECT 'users' AS table_name, COUNT(*) AS row_count FROM users WHERE deleted_date IS NULL
UNION ALL
SELECT 'channel', COUNT(*) FROM channel WHERE deleted_date IS NULL
UNION ALL
SELECT 'live_streaming', COUNT(*) FROM live_streaming WHERE deleted_date IS NULL;

SELECT '
=====================================================
✅ AuthSimulation 테스트 데이터 생성 완료!
=====================================================
- Users: 10,000명
- Channels: 10,000개 (각 유저당 1개)
- Live Streamings: 100개 (LIVE 상태, 메타데이터 조회용)
=====================================================
테스트 사용자 로그인 정보 (TestDataFeeder와 일치):
  - Username: loadtest1 ~ loadtest10000
  - Email: loadtest1@test.com ~ loadtest10000@test.com
  - Password: password123
=====================================================
' AS summary;