-- Spotify 관련 데이터베이스 마이그레이션 스크립트
-- SpotifyUser 테이블 삭제 및 User 테이블에 Spotify 컬럼 추가

-- 1. SpotifyUser 테이블 삭제 (있다면)
DROP TABLE IF EXISTS spotify_user;

-- 2. User 테이블에 Spotify 관련 컬럼 추가
ALTER TABLE user 
ADD COLUMN spotify_oauth_id VARCHAR(255) NULL,
ADD COLUMN spotify_email VARCHAR(255) NULL,
ADD COLUMN spotify_display_name VARCHAR(255) NULL,
ADD COLUMN spotify_access_token TEXT NULL,
ADD COLUMN spotify_refresh_token TEXT NULL;

-- 3. Spotify OAuth ID에 인덱스 추가 (성능 향상을 위해)
CREATE INDEX idx_user_spotify_oauth_id ON user(spotify_oauth_id);

-- 마이그레이션 완료 확인
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'user' 
  AND COLUMN_NAME LIKE 'spotify_%'
ORDER BY ORDINAL_POSITION; 