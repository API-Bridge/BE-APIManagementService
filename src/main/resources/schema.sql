-- ============================================
-- 모든 테이블 삭제 (기존 데이터 완전 정리)
-- ============================================
DROP TABLE IF EXISTS api_parameters;
DROP TABLE IF EXISTS api_tokens;
DROP TABLE IF EXISTS external_api_specs;
DROP TABLE IF EXISTS api_keywords;
DROP TABLE IF EXISTS api_credentials;
DROP TABLE IF EXISTS api_domains;
DROP VIEW IF EXISTS v_api_request_details;

-- ============================================
-- 1. API 분류: 도메인 (Lookup Table)
-- ENUM 대신 별도 테이블로 분리하여 유연성 확보
-- ============================================
CREATE TABLE api_domains (
                             domain_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                             domain_name VARCHAR(100) NOT NULL UNIQUE COMMENT 'API 분류 도메인명',
                             description VARCHAR(255) COMMENT '도메인 설명',
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT = 'API 분류 도메인 정보';

-- ============================================
-- 2. API 분류: 키워드 (Lookup Table)
-- ENUM 대신 별도 테이블로 분리하여 유연성 확보
-- ============================================
CREATE TABLE api_keywords (
                              keyword_id INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                              keyword_name VARCHAR(100) NOT NULL UNIQUE COMMENT 'API 세부 키워드명 (stock_price, current_weather 등)',
                              domain_id INT UNSIGNED NOT NULL COMMENT '해당 키워드가 속한 도메인 ID',
                              description VARCHAR(255) COMMENT '키워드 설명',
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_keyword_domain FOREIGN KEY (domain_id) REFERENCES api_domains (domain_id)
) COMMENT = 'API 분류 키워드 정보';

-- ============================================
-- 3. API 발급 정보 및 자격증명 관리 (구: service)
-- ============================================
CREATE TABLE api_credentials (
                                 credential_id VARCHAR(100) PRIMARY KEY COMMENT '자격증명 고유 식별자',
                                 organization_name VARCHAR(255) NOT NULL COMMENT '서비스명 (예: SGIS OpenAPI, 공공데이터포털 등)',
                                 api_key VARCHAR(500) NOT NULL COMMENT '[보안] 발급받은 액세스 키 (암호화 저장)',
                                 secret_key VARCHAR(500) COMMENT '[보안] 발급받은 시크릿 키 (암호화 저장)',
                                 status ENUM('ACTIVE', 'INACTIVE', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE' COMMENT '자격증명 상태',
                                 issued_at TIMESTAMP NOT NULL COMMENT 'API 키 발급일시',
                                 contact_email VARCHAR(255) COMMENT '담당자 이메일',
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 INDEX idx_organization_name (organization_name)
) COMMENT = '외부 API 서비스의 발급 정보 및 자격증명(Credentials)';

-- ============================================
-- 4. 외부 API 명세(Specification) 관리 (구: external_api)
-- 테이블 이름을 명확히 하고, 중복 컬럼 제거 및 정규화
-- ============================================
CREATE TABLE external_api_specs (
                                    api_id VARCHAR(36) PRIMARY KEY COMMENT '외부 API 고유 식별자 (UUID)',
                                    credential_id VARCHAR(100) NOT NULL COMMENT 'API 호출 시 사용할 자격증명 ID (FK)',
                                    api_name VARCHAR(255) NOT NULL COMMENT 'API 이름 (예: 현재 날씨 조회)',
                                    api_description TEXT COMMENT 'API에 대한 상세 설명',
                                    api_issuer VARCHAR(255) NOT NULL COMMENT 'API 발급처',
                                    api_url VARCHAR(500) NOT NULL UNIQUE COMMENT 'API Endpoint URL (중복 등록 방지)',
                                    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP 메소드 (GET, POST 등)',
                                    domain_id INT UNSIGNED COMMENT 'API 분류 도메인 ID (FK)',
                                    keyword_id INT UNSIGNED COMMENT 'API 세부 키워드 ID (FK)',
                                    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 현재 사용 가능 여부',
                                    health_status ENUM('HEALTHY', 'UNHEALTHY', 'UNKNOWN') NOT NULL DEFAULT 'UNKNOWN' COMMENT 'API 헬스체크 상태',
                                    health_check_path VARCHAR(255) COMMENT 'API 헬스체크 경로',
                                    last_health_check TIMESTAMP NULL COMMENT '마지막 헬스체크 수행 시간',
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_spec_credential FOREIGN KEY (credential_id) REFERENCES api_credentials (credential_id),
                                    CONSTRAINT fk_spec_domain FOREIGN KEY (domain_id) REFERENCES api_domains (domain_id),
                                    CONSTRAINT fk_spec_keyword FOREIGN KEY (keyword_id) REFERENCES api_keywords (keyword_id),
                                    INDEX idx_api_issuer (api_issuer)
) COMMENT = '외부 API 명세 정보';

-- ============================================
-- 5. API 파라미터 정보
-- ============================================
CREATE TABLE api_parameters (
                                parameter_id VARCHAR(36) PRIMARY KEY COMMENT '파라미터 고유 식별자 (UUID)',
                                api_id VARCHAR(36) NOT NULL COMMENT '이 파라미터가 속한 API의 ID (FK)',
                                param_name VARCHAR(255) NOT NULL COMMENT '파라미터 이름',
                                param_type VARCHAR(50) NOT NULL COMMENT '파라미터 데이터 타입 (string, integer 등)',
                                is_required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '필수 파라미터 여부',
                                param_description TEXT COMMENT '파라미터 설명',
                                default_value TEXT COMMENT '파라미터 기본값',
                                additional_fields JSON COMMENT '유연한 추가 필드 (JSON)',
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                CONSTRAINT fk_param_api FOREIGN KEY (api_id) REFERENCES external_api_specs (api_id) ON DELETE CASCADE,
                                INDEX idx_api_id (api_id)
) COMMENT = 'API별 파라미터 구조 및 메타데이터';

-- ============================================
-- 6. 주기적 갱신 토큰 관리
-- ============================================
CREATE TABLE api_tokens (
                            token_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            credential_id VARCHAR(100) NOT NULL UNIQUE COMMENT '토큰 발급에 사용된 자격증명 ID(사용할 API 발급한곳) (FK)',
                            access_token TEXT NOT NULL COMMENT '[보안] 액세스 토큰 (암호화 저장)',
                            refresh_token TEXT COMMENT '[보안] 리프레시 토큰 (암호화 저장)',
                            token_type VARCHAR(50) COMMENT '토큰 유형 (예: Bearer)',
                            expires_at DATETIME NOT NULL COMMENT '토큰 만료 일시',
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                            CONSTRAINT fk_token_credential FOREIGN KEY (credential_id) REFERENCES api_credentials (credential_id) ON DELETE CASCADE,
                            INDEX idx_expires_at (expires_at)
) COMMENT = '주기적으로 갱신되는 API 액세스 토큰';


-- ============================================
-- API 데이터 요청 반환용 View 테이블
-- ============================================
CREATE OR REPLACE VIEW v_api_request_details AS
SELECT
    spec.api_id,
    spec.api_name,
    spec.api_url,
    spec.http_method,
    -- 자격증명 정보
    cred.credential_id,
    cred.api_key,
    cred.secret_key,
    -- 토큰 정보 (토큰이 없을 수도 있으므로 LEFT JOIN)
    token.access_token,
    token.token_type,
    token.expires_at,
    -- 파라미터 정보 (JSON 배열로 집계)
    (
        SELECT JSON_ARRAYAGG(
                       JSON_OBJECT(
                               'name', p.param_name,
                               'type', p.param_type,
                               'required', p.is_required,
                               'description', p.param_description
                       )
               )
        FROM api_parameters p
        WHERE p.api_id = spec.api_id
    ) AS parameters
FROM
    external_api_specs AS spec
        JOIN
    api_credentials AS cred ON spec.credential_id = cred.credential_id
        LEFT JOIN
    api_tokens AS token ON cred.credential_id = token.credential_id;

-- ============================================
-- 성능 최적화를 위한 추가 인덱스
-- ============================================

-- 1. api_domains 테이블 인덱스
-- domain_name으로 검색하는 경우가 많음
CREATE INDEX idx_domains_name ON api_domains (domain_name);

-- 2. api_keywords 테이블 인덱스  
-- keyword_name으로 검색 및 domain_id로 그룹핑하는 경우가 많음
CREATE INDEX idx_keywords_name ON api_keywords (keyword_name);
CREATE INDEX idx_keywords_domain_name ON api_keywords (domain_id, keyword_name);

-- 3. api_credentials 테이블 인덱스
-- status로 필터링하는 경우가 많음 (ACTIVE 상태의 자격증명 조회)
CREATE INDEX idx_credentials_status ON api_credentials (status);
-- organization_name과 status 조합 검색
CREATE INDEX idx_credentials_org_status ON api_credentials (organization_name, status);

-- 4. external_api_specs 테이블 인덱스
-- API 이름으로 검색하는 경우가 많음 (LIKE 검색 최적화)
CREATE INDEX idx_specs_api_name ON external_api_specs (api_name);
-- 활성화 상태로 필터링하는 경우가 매우 많음
CREATE INDEX idx_specs_is_active ON external_api_specs (is_active);
-- credential_id로 해당 자격증명의 모든 API 조회
CREATE INDEX idx_specs_credential ON external_api_specs (credential_id);
-- domain 별 API 조회
CREATE INDEX idx_specs_domain ON external_api_specs (domain_id);
-- keyword 별 API 조회  
CREATE INDEX idx_specs_keyword ON external_api_specs (keyword_id);
-- 헬스체크 상태별 조회 (모니터링용)
CREATE INDEX idx_specs_health_status ON external_api_specs (health_status);
-- 복합 인덱스: 활성 상태 + 자격증명 (가장 빈번한 조회 패턴)
CREATE INDEX idx_specs_active_credential ON external_api_specs (is_active, credential_id);
-- 복합 인덱스: 활성 상태 + 도메인 (카테고리별 조회)
CREATE INDEX idx_specs_active_domain ON external_api_specs (is_active, domain_id);
-- API URL 조회 최적화 (중복 체크용)
CREATE INDEX idx_specs_api_url ON external_api_specs (api_url);

-- 5. api_parameters 테이블 인덱스
-- param_name으로 특정 파라미터 조회 (예: accessToken 파라미터 찾기)
CREATE INDEX idx_parameters_name ON api_parameters (param_name);
-- 필수 파라미터 필터링
CREATE INDEX idx_parameters_required ON api_parameters (is_required);
-- API별 파라미터 조회 + 파라미터명 (가장 빈번한 조회)
CREATE INDEX idx_parameters_api_name ON api_parameters (api_id, param_name);
-- 특정 자격증명의 API들 중 특정 파라미터명 조회 (SGIS accessToken 업데이트용)
CREATE INDEX idx_parameters_credential_name ON api_parameters (api_id, param_name);

-- 6. api_tokens 테이블 인덱스
-- 토큰 유효성 검사 시 만료시간 체크
CREATE INDEX idx_tokens_valid ON api_tokens (credential_id, expires_at);
-- 토큰 타입별 조회
CREATE INDEX idx_tokens_type ON api_tokens (token_type);
-- 만료 예정 토큰 조회 (스케줄러용)
CREATE INDEX idx_tokens_expiring ON api_tokens (expires_at, credential_id);