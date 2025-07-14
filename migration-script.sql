-- =====================================================================
-- AWS RDS MySQL 데이터베이스 초기 설정 스크립트
-- =====================================================================
--
-- 🚀 이 스크립트를 MySQL Workbench나 다른 SQL 클라이언트를 통해
--    AWS RDS 인스턴스에 연결하여 실행해주세요.
--
-- ⚠️ 주의: RDS 인스턴스 생성 시 'Initial database name'을 설정했다면,
--         아래 `CREATE DATABASE`는 필요 없을 수 있습니다.
--         만약 `Unknown database 'moody'` 에러가 발생했다면
--         반드시 아래 1, 2번을 실행해야 합니다.
--
-- =====================================================================

-- 1. 데이터베이스 생성 (가장 먼저 실행!)
--    `moody` 라는 이름의 데이터베이스가 없다면 이 명령어를 실행하세요.
CREATE DATABASE IF NOT EXISTS moody CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 생성한 데이터베이스 사용
USE moody;

-- 3. Playlist 테이블 생성
--    애플리케이션에 필요한 테이블을 생성합니다.
CREATE TABLE IF NOT EXISTS playlist (
    playlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    diary_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_diary_id (diary_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- (필요 시) 다른 테이블들도 여기에 추가...

-- 플레이리스트 테이블에 date 컬럼 추가 (어느 날짜의 일기를 기반으로 생성되었는지 구분)
ALTER TABLE playlist ADD COLUMN date DATE;

-- 기존 데이터의 date 컬럼을 created_at의 날짜 부분으로 업데이트
UPDATE playlist SET date = DATE(created_at) WHERE date IS NULL;

-- date 컬럼을 NOT NULL로 설정
ALTER TABLE playlist MODIFY COLUMN date DATE NOT NULL;

-- 4. 설정 확인
--    아래 명령어로 테이블이 잘 생성되었는지 확인합니다.
SHOW TABLES;
DESCRIBE playlist;

-- =====================================================================
-- 기존 데이터가 있는 경우, 아래에 INSERT 문을 추가하여 데이터를 이전할 수 있습니다.
-- 예시:
-- INSERT INTO playlist (title, diary_id, created_at) VALUES
-- ('Sample Playlist 1', 1, NOW()),
-- ('Sample Playlist 2', 2, NOW());
-- ===================================================================== 