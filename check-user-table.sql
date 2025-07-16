-- User 테이블 구조 확인
USE moody;

-- 테이블 구조 확인
DESCRIBE user;

-- onboarding_completed 컬럼 확인
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'user' 
  AND COLUMN_NAME = 'onboarding_completed';

-- 최근 사용자들의 온보딩 완료 상태 확인
SELECT 
    user_id,
    name,
    onboarding_completed,
    created_at,
    updated_at
FROM user 
ORDER BY created_at DESC 
LIMIT 10; 