-- =====================================================
-- API 관리 서비스 데이터베이스 초기화 스크립트
-- =====================================================

-- 1. 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS api_management_service_db
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 데이터베이스 사용
USE api_management_service_db;

-- 3. 공유 스키마 테이블 생성 (팀장이 공유한 구조)
-- external_api 테이블
CREATE TABLE IF NOT EXISTS external_api (
    api_id VARCHAR(36) NOT NULL COMMENT 'PK. 외부 API의 고유 식별자',
    api_name VARCHAR(255) NOT NULL COMMENT 'API의 이름',
    api_url VARCHAR(500) NOT NULL COMMENT 'API의 URL 주소',
    api_issuer VARCHAR(255) NOT NULL COMMENT 'API 발급처',
    api_owner VARCHAR(36) NULL COMMENT 'API를 추가한 사용자 ID (다른 서비스의 user_id)',
    api_domain VARCHAR(255) NOT NULL COMMENT 'API 분류 도메인 (e.g., weather, stock)',
    api_keyword VARCHAR(255) NOT NULL COMMENT 'API 세부분류용 키워드',
    http_method VARCHAR(10) NOT NULL COMMENT 'API의 HTTP 메소드 (GET, POST 등)',
    api_description TEXT NULL COMMENT 'API에 대한 상세 설명',
    api_effectiveness BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 유효성 상태 (헬스체크 결과 반영)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (api_id),
    INDEX idx_api_domain (api_domain),
    INDEX idx_api_owner (api_owner),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='외부 API의 메타데이터를 관리합니다.';

-- api_parameter 테이블
CREATE TABLE IF NOT EXISTS api_parameter (
    parameter_id VARCHAR(36) NOT NULL COMMENT 'PK. 파라미터 정보 고유 식별자',
    api_id VARCHAR(36) NOT NULL COMMENT 'FK. 이 파라미터가 속한 API의 ID',
    param_name VARCHAR(255) NOT NULL COMMENT '파라미터 이름 (e.g., user_id, page)',
    param_type VARCHAR(50) NOT NULL COMMENT '파라미터의 데이터 타입 (string, integer 등)',
    is_required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '해당 파라미터가 필수인지 여부',
    default_value TEXT NULL COMMENT '파라미터의 기본값 (선택사항)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (parameter_id),
    INDEX idx_api_id (api_id),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='각 외부 API에 대한 파라미터 정보를 관리합니다.';



-- 4. 샘플 데이터 삽입 (테스트용)
INSERT INTO external_api (api_id, api_name, api_url, api_issuer, api_owner, api_domain, api_keyword, http_method, api_description, api_effectiveness) VALUES
('api-001', 'Weather API', 'https://api.weather.com/v1/current', 'Weather Service Inc.', 'user-001', 'weather', 'current_weather', 'GET', '현재 날씨 정보를 제공하는 API', true),
('api-002', 'Stock Price API', 'https://api.stocks.com/v1/price', 'Stock Market Corp.', 'user-002', 'finance', 'stock_price', 'GET', '실시간 주식 가격 정보를 제공하는 API', true),
('api-003', 'News API', 'https://api.news.com/v1/headlines', 'News Media Ltd.', 'user-003', 'news', 'breaking_news', 'GET', '최신 뉴스 헤드라인을 제공하는 API', true);

INSERT INTO api_parameter (parameter_id, api_id, param_name, param_type, is_required, default_value) VALUES
('param-001', 'api-001', 'city', 'string', true, 'Seoul'),
('param-002', 'api-001', 'country', 'string', false, 'KR'),
('param-003', 'api-002', 'symbol', 'string', true, NULL),
('param-004', 'api-002', 'exchange', 'string', false, 'NYSE'),
('param-005', 'api-003', 'category', 'string', false, 'general'),
('param-006', 'api-003', 'limit', 'integer', false, '10');

-- 5. 완료 메시지
SELECT 'Database initialization completed successfully!' as status;
