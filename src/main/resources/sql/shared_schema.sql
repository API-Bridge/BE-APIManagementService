-- =====================================================
-- API 관리 서비스 (API Management Service) 공유 스키마
-- 팀장이 공유한 핵심 테이블 구조
-- =====================================================

-- 외부 API의 메타데이터
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

-- 각 외부 API에 대한 파라미터 정보
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
    -- MSA 환경에서는 외래키 제약조건 제거
    -- 데이터 일관성은 애플리케이션 레벨과 이벤트를 통해 관리
    -- CONSTRAINT fk_api_parameters_to_external_apis FOREIGN KEY (api_id) REFERENCES external_api (api_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='각 외부 API에 대한 파라미터 정보를 관리합니다.';


