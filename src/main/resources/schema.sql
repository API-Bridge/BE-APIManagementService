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
                                    api_id VARCHAR(36) PRIMARY KEY COMMENT '외부 API 고유 식별자',
                                    credential_id VARCHAR(100) NOT NULL COMMENT 'API 호출 시 사용할 자격증명 ID (FK)',
                                    api_name VARCHAR(255) NOT NULL COMMENT 'API 이름 (예: 현재 날씨 조회)',
                                    api_description TEXT COMMENT 'API에 대한 상세 설명',
                                    api_issuer VARCHAR(255) NOT NULL COMMENT 'API 발급처',
                                    api_url VARCHAR(500) NOT NULL COMMENT 'API Endpoint URL',
                                    http_method VARCHAR(10) NOT NULL COMMENT 'HTTP 메소드 (GET, POST 등)',
                                    domain_id INT UNSIGNED COMMENT 'API 분류 도메인 ID (FK)',
                                    keyword_id INT UNSIGNED COMMENT 'API 세부 키워드 ID (FK)',
                                    is_active BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 현재 사용 가능 여부',
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
                                parameter_id VARCHAR(36) PRIMARY KEY COMMENT '파라미터 고유 식별자',
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