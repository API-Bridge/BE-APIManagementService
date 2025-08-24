-- =====================================================
-- 테이블 삭제 스크립트
-- 개발/테스트 환경에서 스키마 재생성 시 사용
-- =====================================================

USE api_management_service_db;

-- 의존성 순서에 따른 테이블 삭제
-- 1. AI 분류 결과 테이블
DROP TABLE IF EXISTS ai_classifications;

-- 2. API 파라미터 테이블  
DROP TABLE IF EXISTS api_parameter;

-- 3. 외부 API 테이블
DROP TABLE IF EXISTS external_api;

-- 4. API 키 테이블
DROP TABLE IF EXISTS api_keys;

-- 완료 메시지
SELECT 'All tables dropped successfully!' as status;