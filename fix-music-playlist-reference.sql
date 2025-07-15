-- =====================================================================
-- Music 테이블 외래키 수정 스크립트
-- music.playlist_id가 playlist 테이블을 참조하도록 변경
-- =====================================================================

USE moody;

-- 1. 기존 외래키 제약조건 확인 및 제거
SET FOREIGN_KEY_CHECKS = 0;

-- music 테이블의 기존 외래키 제약조건 찾기 및 삭제
SELECT CONSTRAINT_NAME 
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'moody' 
  AND TABLE_NAME = 'music' 
  AND COLUMN_NAME = 'playlist_id' 
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 기존 외래키 제약조건이 있다면 삭제 (실제 제약조건 이름으로 변경 필요)
-- ALTER TABLE music DROP FOREIGN KEY 제약조건이름;

-- 2. playlists 테이블이 존재한다면 데이터 이전
-- playlists 테이블의 데이터를 playlist 테이블로 복사 (중복 방지)
INSERT IGNORE INTO playlist (playlist_id, title, user_id, date, created_at, updated_at)
SELECT 
    playlist_id, 
    title, 
    diary_id as user_id,  -- diary_id를 user_id로 매핑
    COALESCE(date, DATE(created_at)) as date,  -- date가 없으면 created_at의 날짜 부분 사용
    created_at,
    COALESCE(updated_at, created_at) as updated_at
FROM playlists 
WHERE EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'moody' AND TABLE_NAME = 'playlists');

-- 3. music 테이블의 playlist_id가 올바른 playlist를 참조하는지 확인
-- 참조 무결성 위반 데이터 확인
SELECT m.music_id, m.playlist_id, 'music 테이블에 존재하지만 playlist 테이블에 없는 playlist_id' as issue
FROM music m
LEFT JOIN playlist p ON m.playlist_id = p.playlist_id
WHERE p.playlist_id IS NULL;

-- 4. 참조 무결성 위반 데이터가 있다면 삭제하거나 수정
-- DELETE FROM music WHERE playlist_id NOT IN (SELECT playlist_id FROM playlist);

-- 5. 새로운 외래키 제약조건 추가
ALTER TABLE music 
ADD CONSTRAINT fk_music_playlist 
FOREIGN KEY (playlist_id) REFERENCES playlist(playlist_id) ON DELETE CASCADE;

-- 6. user_id 외래키도 확인 및 추가 (필요한 경우)
ALTER TABLE music 
ADD CONSTRAINT fk_music_user 
FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS = 1;

-- 7. 검증: 외래키 제약조건 확인
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    CONSTRAINT_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE 
WHERE TABLE_SCHEMA = 'moody' 
  AND TABLE_NAME = 'music' 
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- 8. (선택사항) playlists 테이블 삭제
-- 데이터 이전이 완료되고 모든 것이 정상 작동한다면 playlists 테이블을 삭제할 수 있습니다.
-- DROP TABLE IF EXISTS playlists;

SHOW CREATE TABLE music;
SHOW CREATE TABLE playlist; 