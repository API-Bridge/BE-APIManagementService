-- =====================================================
-- 테스트용 H2 데이터베이스 스키마
-- =====================================================

-- external_api 테이블 (엔티티 필드명과 정확히 일치)
CREATE TABLE IF NOT EXISTS external_api (
    api_id VARCHAR(36) NOT NULL,
    api_name VARCHAR(255) NOT NULL,
    api_url VARCHAR(500) NOT NULL,
    api_issuer VARCHAR(255) NOT NULL,
    api_owner VARCHAR(36) NULL,
    api_domain VARCHAR(255) NOT NULL,
    api_keyword VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    api_description TEXT NULL,
    api_effectiveness TINYINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (api_id)
);

-- api_parameter 테이블  
CREATE TABLE IF NOT EXISTS api_parameter (
    parameter_id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    param_name VARCHAR(255) NOT NULL,
    param_type VARCHAR(50) NOT NULL,
    is_required TINYINT NOT NULL DEFAULT 0,
    default_value TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (parameter_id)
);

-- ai_classifications 테이블
CREATE TABLE IF NOT EXISTS ai_classifications (
    classification_id VARCHAR(36) NOT NULL,
    api_id VARCHAR(36) NOT NULL,
    classified_domain VARCHAR(255) NOT NULL,
    classified_keyword VARCHAR(255) NOT NULL,
    classified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    classification_log TEXT,
    analyzed_text TEXT,
    model_version VARCHAR(255),
    metadata TEXT,
    PRIMARY KEY (classification_id)
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_api_domain ON external_api (api_domain);
CREATE INDEX IF NOT EXISTS idx_api_owner ON external_api (api_owner);
CREATE INDEX IF NOT EXISTS idx_deleted ON external_api (deleted);
CREATE INDEX IF NOT EXISTS idx_created_at_ext ON external_api (created_at);

CREATE INDEX IF NOT EXISTS idx_api_id ON api_parameter (api_id);
CREATE INDEX IF NOT EXISTS idx_deleted_param ON api_parameter (deleted);
CREATE INDEX IF NOT EXISTS idx_created_at_param ON api_parameter (created_at);
