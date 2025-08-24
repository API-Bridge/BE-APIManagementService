-- =====================================================
-- API 관리 서비스 전체 테이블 생성 스크립트
-- 현재 프로젝트의 모든 엔티티를 기반으로 작성
-- =====================================================

-- 데이터베이스 생성 및 설정
CREATE DATABASE IF NOT EXISTS api_management_service_db
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE api_management_service_db;

-- =====================================================
-- 1. API 키 관리 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS api_keys (
    key_id VARCHAR(100) NOT NULL COMMENT 'PK. API 키 ID',
    organization_name VARCHAR(255) NOT NULL COMMENT '기관명',
    organization_code VARCHAR(100) NULL COMMENT '기관 코드',
    contact_email VARCHAR(255) NOT NULL COMMENT '연락처 이메일',
    contact_phone VARCHAR(50) NULL COMMENT '연락처 전화번호',
    api_service_name VARCHAR(100) NOT NULL COMMENT 'API 서비스명 (SGIS, KAKAO 등)',
    api_service_url VARCHAR(500) NULL COMMENT 'API 서비스 URL',
    api_key VARCHAR(500) NOT NULL COMMENT '발급받은 실제 API 키',
    secret_key VARCHAR(500) NULL COMMENT '발급받은 시크릿 키',
    daily_limit INT NULL COMMENT '일일 API 호출 제한 횟수',
    monthly_limit INT NULL COMMENT '월간 API 호출 제한 횟수',
    current_daily_usage INT NOT NULL DEFAULT 0 COMMENT '현재 일일 사용량',
    current_monthly_usage INT NOT NULL DEFAULT 0 COMMENT '현재 월간 사용량',
    issued_at DATETIME NOT NULL COMMENT 'API 키 발급일시',
    expires_at DATETIME NULL COMMENT 'API 키 만료일시',
    last_used_at DATETIME NULL COMMENT '마지막 사용일시',
    status VARCHAR(50) NOT NULL COMMENT 'API 키 상태 (ACTIVE, INACTIVE, EXPIRED, SUSPENDED, REVOKED, PENDING)',
    description TEXT NULL COMMENT 'API 키 설명',
    requested_apis TEXT NULL COMMENT '요청한 API 목록 (JSON 형태)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (key_id),
    INDEX idx_organization_name (organization_name),
    INDEX idx_api_service_name (api_service_name),
    INDEX idx_status (status),
    INDEX idx_issued_at (issued_at),
    INDEX idx_expires_at (expires_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='API 키 관리 테이블';

-- =====================================================
-- 2. 외부 API 메타데이터 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS external_api (
    api_id VARCHAR(36) NOT NULL COMMENT 'PK. 외부 API의 고유 식별자',
    api_name VARCHAR(255) NOT NULL COMMENT 'API의 이름',
    api_url VARCHAR(500) NOT NULL COMMENT 'API의 URL 주소',
    api_issuer VARCHAR(255) NOT NULL COMMENT 'API 발급처',
    api_owner VARCHAR(36) NULL COMMENT 'API를 추가한 관리자 ID',
    api_domain VARCHAR(255) NOT NULL COMMENT 'API 분류 도메인',
    api_keyword VARCHAR(255) NOT NULL COMMENT 'API 세부분류용 키워드',
    api_key_id VARCHAR(100) NULL COMMENT 'API 키 참조',
    api_token TEXT NULL COMMENT 'API 토큰 (인증용)',
    token_expires_at DATETIME NULL COMMENT '토큰 만료 시간',
    auto_token_refresh BOOLEAN NOT NULL DEFAULT TRUE COMMENT '토큰 자동 갱신 여부',
    http_method VARCHAR(10) NOT NULL COMMENT 'API의 HTTP 메소드',
    api_description TEXT NULL COMMENT 'API에 대한 상세 설명',
    api_effectiveness BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 유효성 상태',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (api_id),
    INDEX idx_api_domain (api_domain),
    INDEX idx_api_owner (api_owner),
    INDEX idx_api_keyword (api_keyword),
    INDEX idx_api_key_id (api_key_id),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci 
COMMENT='외부 API의 메타데이터를 관리';

-- =====================================================
-- 3. API 파라미터 정보 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS api_parameter (
    parameter_id VARCHAR(36) NOT NULL COMMENT 'PK. 파라미터 정보 고유 식별자',
    api_id VARCHAR(36) NOT NULL COMMENT 'FK. 이 파라미터가 속한 API의 ID',
    param_name VARCHAR(255) NOT NULL COMMENT '파라미터 이름',
    param_type VARCHAR(50) NOT NULL COMMENT '파라미터의 데이터 타입',
    is_required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '해당 파라미터가 필수인지 여부',
    default_value TEXT NULL COMMENT '파라미터의 기본값',
    param_description TEXT NULL COMMENT '파라미터에 대한 설명',
    additional_fields JSON NULL COMMENT '동적 추가 필드 (JSON 형태)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (parameter_id),
    INDEX idx_api_id (api_id),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='각 외부 API에 대한 파라미터 정보를 관리';

-- =====================================================
-- 4. AI 분류 결과 테이블
-- =====================================================
CREATE TABLE IF NOT EXISTS ai_classifications (
    classification_id VARCHAR(36) NOT NULL COMMENT 'PK. Classification ID',
    api_id VARCHAR(36) NOT NULL COMMENT 'FK. API ID',
    classified_domain VARCHAR(50) NULL COMMENT '분류된 도메인',
    classified_keyword VARCHAR(50) NULL COMMENT '분류된 키워드',
    classified_at DATETIME NULL COMMENT '분류 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    PRIMARY KEY (classification_id),
    INDEX idx_api_id (api_id),
    INDEX idx_domain (classified_domain),
    INDEX idx_keyword (classified_keyword),
    INDEX idx_classified_at (classified_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='AI 자동 분류 결과를 저장';

-- =====================================================
-- 샘플 데이터 삽입
-- =====================================================

-- API 키 샘플 데이터
INSERT INTO api_keys (key_id, organization_name, contact_email, api_service_name, api_key, status, issued_at) VALUES
('ORG_001', '서울시청', 'contact@seoul.go.kr', 'SGIS', 'sample_sgis_api_key_001', 'ACTIVE', NOW()),
('ORG_002', '부산시청', 'contact@busan.go.kr', 'KAKAO', 'sample_kakao_api_key_001', 'ACTIVE', NOW()),
('ORG_003', '대구시청', 'contact@daegu.go.kr', 'NAVER', 'sample_naver_api_key_001', 'ACTIVE', NOW());

-- 외부 API 샘플 데이터
INSERT INTO external_api (api_id, api_name, api_url, api_issuer, api_owner, api_domain, api_keyword, api_key_id, http_method, api_description) VALUES
('api-001', 'SGIS 인구밀도 조회', 'https://sgisapi.kostat.go.kr/OpenAPI3/population/density.json', '통계청', 'admin-001', 'GOVERNMENT', 'POPULATION', 'ORG_001', 'GET', '통계청 SGIS 인구밀도 조회 API'),
('api-002', '카카오 주소 검색', 'https://dapi.kakao.com/v2/local/search/address.json', '카카오', 'admin-002', 'LOCATION', 'ADDRESS_SEARCH', 'ORG_002', 'GET', '카카오 주소 검색 API'),
('api-003', '네이버 뉴스 검색', 'https://openapi.naver.com/v1/search/news.json', '네이버', 'admin-003', 'NEWS', 'NEWS_SEARCH', 'ORG_003', 'GET', '네이버 뉴스 검색 API');

-- API 파라미터 샘플 데이터
INSERT INTO api_parameter (parameter_id, api_id, param_name, param_type, is_required, default_value, param_description) VALUES
('param-001', 'api-001', 'accessToken', 'string', true, NULL, 'SGIS 접근 토큰'),
('param-002', 'api-001', 'year', 'string', true, '2020', '조회 연도'),
('param-003', 'api-001', 'adm_cd', 'string', false, NULL, '행정구역 코드'),
('param-004', 'api-002', 'query', 'string', true, NULL, '검색할 주소'),
('param-005', 'api-002', 'page', 'integer', false, '1', '페이지 번호'),
('param-006', 'api-003', 'query', 'string', true, NULL, '검색어'),
('param-007', 'api-003', 'display', 'integer', false, '10', '검색 결과 출력 건수'),
('param-008', 'api-003', 'start', 'integer', false, '1', '검색 시작 위치');

-- AI 분류 결과 샘플 데이터
INSERT INTO ai_classifications (classification_id, api_id, classified_domain, classified_keyword, classified_at) VALUES
('class-001', 'api-001', 'GOVERNMENT', 'POPULATION', NOW()),
('class-002', 'api-002', 'LOCATION', 'ADDRESS_SEARCH', NOW()),
('class-003', 'api-003', 'NEWS', 'NEWS_SEARCH', NOW());

-- 완료 메시지
SELECT 'All tables created successfully with sample data!' as status;