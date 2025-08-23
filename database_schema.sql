-- ============================================
-- API Management Service 데이터베이스 스키마
-- ============================================

-- Enum 타입들을 위한 제약조건 정의

-- API Key Status Enum 값들
-- ACTIVE, INACTIVE, EXPIRED, SUSPENDED, REVOKED, PENDING

-- API Domain Enum 값들
-- finance, weather, news, transportation, commerce, government, 
-- entertainment, sports, healthcare, education, realestate, travel, 
-- technology, lifestyle, others

-- API Keyword Enum 값들 (총 66개)
-- 금융: stock_price, stock_index, exchange_rate, cryptocurrency, interest_rate, economic_indicator
-- 날씨: current_weather, weather_forecast, air_quality, temperature, precipitation, uv_index
-- 뉴스: breaking_news, politics, economy_news, tech_news, sports_news, social_trend
-- 교통: subway_info, bus_info, traffic_condition, parking_info, taxi_fare
-- 쇼핑: product_info, price_comparison, product_review, coupon_discount, shopping_rank
-- 정부: public_data, legal_info, statistics, public_service, policy_info
-- 엔터테인먼트: movie_info, music_chart, tv_schedule, celebrity_news, game_info
-- 스포츠: soccer_result, baseball_result, basketball_result, sports_schedule, player_stats, team_ranking
-- 헬스케어: hospital_info, health_tip, medicine_info, fitness_data
-- 교육: exam_schedule, course_info, scholarship, school_info
-- 부동산: house_price, rent_info, real_estate_trend
-- 여행: flight_info, hotel_info, tourist_spot, restaurant_info
-- 기술: api_document, tech_trend, developer_tool
-- 라이프스타일: fashion_trend, beauty_tip, recipe, interior_tip

-- ============================================
-- 1. API Key 관리 테이블
-- ============================================
CREATE TABLE api_keys (
    -- 기본 정보
    key_id VARCHAR(100) PRIMARY KEY COMMENT '고유 API 키 ID (예: ORG_001, ORG_002)',
    organization_name VARCHAR(255) NOT NULL COMMENT '기관명 (예: 서울시청, 부산대학교)',
    organization_code VARCHAR(100) COMMENT '기관 코드 (예: SEOUL001, PNU001)',
    contact_email VARCHAR(255) NOT NULL COMMENT '연락처 이메일',
    contact_phone VARCHAR(50) COMMENT '연락처 전화번호',
    
    -- API 서비스 정보
    api_service_name VARCHAR(100) NOT NULL COMMENT 'API 서비스명 (예: SGIS, KAKAO, NAVER)',
    api_service_url VARCHAR(500) COMMENT 'API 서비스 URL',
    api_key VARCHAR(500) NOT NULL COMMENT '발급받은 실제 API 키',
    secret_key VARCHAR(500) COMMENT '발급받은 시크릿 키',
    
    -- 사용량 제한 및 통계
    daily_limit INT COMMENT '일일 API 호출 제한 횟수',
    monthly_limit INT COMMENT '월간 API 호출 제한 횟수',
    current_daily_usage INT DEFAULT 0 COMMENT '현재 일일 사용량',
    current_monthly_usage INT DEFAULT 0 COMMENT '현재 월간 사용량',
    
    -- 날짜 정보
    issued_at TIMESTAMP NOT NULL COMMENT 'API 키 발급일시',
    expires_at TIMESTAMP COMMENT 'API 키 만료일시',
    last_used_at TIMESTAMP COMMENT '마지막 사용일시',
    
    -- 상태 및 메타 정보
    status ENUM('ACTIVE', 'INACTIVE', 'EXPIRED', 'SUSPENDED', 'REVOKED', 'PENDING') NOT NULL DEFAULT 'ACTIVE' COMMENT 'API 키 상태',
    description TEXT COMMENT 'API 키 설명',
    requested_apis TEXT COMMENT '요청한 API 목록 (JSON 형태)',
    
    -- 공통 필드 (BaseEntity 상속)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '엔티티 생성 일시',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '엔티티 마지막 수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Soft Delete를 위한 삭제 플래그',
    
    -- 인덱스
    INDEX idx_organization_name (organization_name),
    INDEX idx_api_service_name (api_service_name),
    INDEX idx_status (status),
    INDEX idx_expires_at (expires_at),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) COMMENT = 'API 키 관리 테이블 - 외부 API 서비스의 API 키 정보를 체계적으로 관리';

-- ============================================
-- 2. 외부 API 메타데이터 테이블
-- ============================================
CREATE TABLE external_api (
    -- 기본 정보
    api_id VARCHAR(36) PRIMARY KEY COMMENT '외부 API의 고유 식별자',
    api_name VARCHAR(255) NOT NULL COMMENT 'API의 이름',
    api_url VARCHAR(500) NOT NULL COMMENT 'API의 URL 주소',
    api_issuer VARCHAR(255) NOT NULL COMMENT 'API 발급처',
    api_owner VARCHAR(36) COMMENT 'API를 추가한 사용자 ID',
    
    -- AI 분류 정보
    api_domain ENUM('finance', 'weather', 'news', 'transportation', 'commerce', 'government', 
                   'entertainment', 'sports', 'healthcare', 'education', 'realestate', 
                   'travel', 'technology', 'lifestyle', 'others') NOT NULL COMMENT 'API 분류 도메인 (AI 자동 분류 결과)',
    api_keyword ENUM('stock_price', 'stock_index', 'exchange_rate', 'cryptocurrency', 'interest_rate', 'economic_indicator',
                    'current_weather', 'weather_forecast', 'air_quality', 'temperature', 'precipitation', 'uv_index',
                    'breaking_news', 'politics', 'economy_news', 'tech_news', 'sports_news', 'social_trend',
                    'subway_info', 'bus_info', 'traffic_condition', 'parking_info', 'taxi_fare',
                    'product_info', 'price_comparison', 'product_review', 'coupon_discount', 'shopping_rank',
                    'public_data', 'legal_info', 'statistics', 'public_service', 'policy_info',
                    'movie_info', 'music_chart', 'tv_schedule', 'celebrity_news', 'game_info',
                    'soccer_result', 'baseball_result', 'basketball_result', 'sports_schedule', 'player_stats', 'team_ranking',
                    'hospital_info', 'health_tip', 'medicine_info', 'fitness_data',
                    'exam_schedule', 'course_info', 'scholarship', 'school_info',
                    'house_price', 'rent_info', 'real_estate_trend',
                    'flight_info', 'hotel_info', 'tourist_spot', 'restaurant_info',
                    'api_document', 'tech_trend', 'developer_tool',
                    'fashion_trend', 'beauty_tip', 'recipe', 'interior_tip') NOT NULL COMMENT 'API 세부분류용 키워드 (AI 자동 분류 결과)',
    
    -- 인증 정보
    api_key_id VARCHAR(100) COMMENT 'API 키 참조 (FK)',
    api_token TEXT COMMENT 'API 토큰 (인증용)',
    token_expires_at TIMESTAMP COMMENT '토큰 만료 시간',
    auto_token_refresh BOOLEAN NOT NULL DEFAULT TRUE COMMENT '토큰 자동 갱신 여부',
    
    -- API 메타 정보
    http_method VARCHAR(10) NOT NULL COMMENT 'API의 HTTP 메소드',
    api_description TEXT COMMENT 'API에 대한 상세 설명',
    api_effectiveness BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'API 유효성 상태',
    
    -- 공통 필드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Soft Delete를 위한 삭제 플래그',
    
    -- 인덱스
    INDEX idx_api_domain (api_domain),
    INDEX idx_api_owner (api_owner),
    INDEX idx_api_keyword (api_keyword),
    INDEX idx_deleted (deleted),
    INDEX idx_api_key_id (api_key_id),
    INDEX idx_api_issuer (api_issuer),
    INDEX idx_effectiveness (api_effectiveness),
    
    -- MSA 환경에서 외래키 제약조건 제거 (애플리케이션 레벨에서 일관성 관리)
    -- FOREIGN KEY (api_key_id) REFERENCES api_keys(key_id) -- 제거됨
    
) COMMENT = '외부 API 메타데이터 테이블 - API 등록, 분류, 관리 정보를 저장';

-- ============================================
-- 3. AI 분류 결과 테이블
-- ============================================
CREATE TABLE ai_classifications (
    -- 기본 정보
    classification_id VARCHAR(36) PRIMARY KEY COMMENT 'Classification ID',
    api_id VARCHAR(36) NOT NULL COMMENT 'API ID',
    
    -- 분류 결과
    classified_domain ENUM('finance', 'weather', 'news', 'transportation', 'commerce', 'government', 
                          'entertainment', 'sports', 'healthcare', 'education', 'realestate', 
                          'travel', 'technology', 'lifestyle', 'others') COMMENT '분류된 도메인',
    classified_keyword ENUM('stock_price', 'stock_index', 'exchange_rate', 'cryptocurrency', 'interest_rate', 'economic_indicator',
                           'current_weather', 'weather_forecast', 'air_quality', 'temperature', 'precipitation', 'uv_index',
                           'breaking_news', 'politics', 'economy_news', 'tech_news', 'sports_news', 'social_trend',
                           'subway_info', 'bus_info', 'traffic_condition', 'parking_info', 'taxi_fare',
                           'product_info', 'price_comparison', 'product_review', 'coupon_discount', 'shopping_rank',
                           'public_data', 'legal_info', 'statistics', 'public_service', 'policy_info',
                           'movie_info', 'music_chart', 'tv_schedule', 'celebrity_news', 'game_info',
                           'soccer_result', 'baseball_result', 'basketball_result', 'sports_schedule', 'player_stats', 'team_ranking',
                           'hospital_info', 'health_tip', 'medicine_info', 'fitness_data',
                           'exam_schedule', 'course_info', 'scholarship', 'school_info',
                           'house_price', 'rent_info', 'real_estate_trend',
                           'flight_info', 'hotel_info', 'tourist_spot', 'restaurant_info',
                           'api_document', 'tech_trend', 'developer_tool',
                           'fashion_trend', 'beauty_tip', 'recipe', 'interior_tip') COMMENT '분류된 키워드',
    
    -- 메타 정보
    classified_at TIMESTAMP COMMENT '분류 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Soft Delete를 위한 삭제 플래그',
    
    -- 인덱스
    INDEX idx_api_id (api_id),
    INDEX idx_domain (classified_domain),
    INDEX idx_keyword (classified_keyword),
    INDEX idx_classified_at (classified_at),
    
    -- MSA 환경에서 외래키 제약조건 제거 (애플리케이션 레벨에서 일관성 관리)
    -- FOREIGN KEY (api_id) REFERENCES external_api(api_id) -- 제거됨
    
) COMMENT = 'AI 분류 결과 테이블 - AI 자동 분류 결과를 저장';

-- ============================================
-- 4. API 파라미터 정보 테이블
-- ============================================
CREATE TABLE api_parameter (
    -- 기본 정보
    parameter_id VARCHAR(36) PRIMARY KEY COMMENT '파라미터 정보 고유 식별자',
    api_id VARCHAR(36) NOT NULL COMMENT '이 파라미터가 속한 API의 ID',
    
    -- 파라미터 정보
    param_name VARCHAR(255) NOT NULL COMMENT '파라미터 이름',
    param_type VARCHAR(50) NOT NULL COMMENT '파라미터의 데이터 타입',
    is_required BOOLEAN NOT NULL DEFAULT FALSE COMMENT '해당 파라미터가 필수인지 여부',
    default_value TEXT COMMENT '파라미터의 기본값',
    param_description TEXT COMMENT '파라미터에 대한 설명',
    
    -- 동적 필드 (JSON)
    additional_fields JSON COMMENT '동적 추가 필드를 JSON으로 저장 - API마다 다른 파라미터 구조를 유연하게 저장',
    
    -- 공통 필드
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정 일시',
    deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT '삭제 여부 (Soft Delete)',
    
    -- 인덱스
    INDEX idx_api_id (api_id),
    INDEX idx_deleted (deleted),
    INDEX idx_created_at (created_at),
    INDEX idx_param_name (param_name),
    INDEX idx_is_required (is_required),
    
    -- MSA 환경에서 외래키 제약조건 제거 (애플리케이션 레벨에서 일관성 관리)
    -- FOREIGN KEY (api_id) REFERENCES external_api(api_id) -- 제거됨
    
) COMMENT = 'API 파라미터 정보 테이블 - API별 파라미터 구조 및 메타데이터를 저장';

-- ============================================
-- 유용한 뷰 생성
-- ============================================

-- 1. 활성 API 키와 연결된 API 목록 뷰
CREATE VIEW v_active_apis AS
SELECT 
    ea.api_id,
    ea.api_name,
    ea.api_url,
    ea.api_domain,
    ea.api_keyword,
    ak.key_id,
    ak.organization_name,
    ak.api_service_name,
    ak.status as api_key_status,
    ea.api_effectiveness,
    ea.created_at
FROM external_api ea
LEFT JOIN api_keys ak ON ea.api_key_id = ak.key_id
WHERE ea.deleted = FALSE 
  AND ea.api_effectiveness = TRUE
  AND (ak.status = 'ACTIVE' OR ak.status IS NULL);

-- 2. API 도메인별 통계 뷰
CREATE VIEW v_api_domain_stats AS
SELECT 
    api_domain,
    COUNT(*) as total_apis,
    COUNT(CASE WHEN api_effectiveness = TRUE THEN 1 END) as active_apis,
    COUNT(CASE WHEN deleted = TRUE THEN 1 END) as deleted_apis
FROM external_api
GROUP BY api_domain;

-- 3. API 키 사용량 통계 뷰
CREATE VIEW v_api_key_usage_stats AS
SELECT 
    key_id,
    organization_name,
    api_service_name,
    daily_limit,
    current_daily_usage,
    monthly_limit,
    current_monthly_usage,
    CASE 
        WHEN daily_limit IS NOT NULL THEN ROUND((current_daily_usage / daily_limit) * 100, 2)
        ELSE NULL 
    END as daily_usage_percent,
    CASE 
        WHEN monthly_limit IS NOT NULL THEN ROUND((current_monthly_usage / monthly_limit) * 100, 2)
        ELSE NULL 
    END as monthly_usage_percent,
    status,
    expires_at,
    last_used_at
FROM api_keys
WHERE deleted = FALSE;

-- ============================================
-- 초기 데이터 (예시)
-- ============================================

-- API 키 샘플 데이터
INSERT INTO api_keys (
    key_id, organization_name, organization_code, contact_email, 
    api_service_name, api_key, daily_limit, monthly_limit,
    issued_at, status, description
) VALUES 
(
    'SGIS_001', '통계청', 'KOSTAT001', 'admin@kostat.go.kr',
    'SGIS', 'sample_sgis_api_key_12345', 1000, 30000,
    NOW(), 'ACTIVE', 'SGIS API 마스터 키'
),
(
    'KAKAO_001', '카카오 개발팀', 'KAKAO001', 'dev@kakao.com',
    'KAKAO', 'sample_kakao_api_key_67890', 5000, 150000,
    NOW(), 'ACTIVE', 'Kakao Map API 키'
);

-- ============================================
-- 인덱스 추가 최적화
-- ============================================

-- 복합 인덱스 생성
CREATE INDEX idx_external_api_domain_keyword ON external_api (api_domain, api_keyword);
CREATE INDEX idx_external_api_effectiveness_deleted ON external_api (api_effectiveness, deleted);
CREATE INDEX idx_api_keys_service_status ON api_keys (api_service_name, status);
CREATE INDEX idx_api_keys_usage_limits ON api_keys (current_daily_usage, daily_limit);

-- ============================================
-- 트리거 생성 (사용량 리셋 등)
-- ============================================

-- 일일 사용량 리셋 트리거 (예시 - 스케줄러에서 처리하는 것을 권장)
-- CREATE EVENT daily_usage_reset
-- ON SCHEDULE EVERY 1 DAY
-- STARTS (TIMESTAMP(CURDATE()) + INTERVAL 1 DAY)
-- DO
--   UPDATE api_keys SET current_daily_usage = 0 WHERE deleted = FALSE;

-- 월간 사용량 리셋 트리거 (예시 - 스케줄러에서 처리하는 것을 권장)  
-- CREATE EVENT monthly_usage_reset
-- ON SCHEDULE EVERY 1 MONTH
-- STARTS (TIMESTAMP(DATE_FORMAT(NOW() ,'%Y-%m-01')) + INTERVAL 1 MONTH)
-- DO
--   UPDATE api_keys SET current_monthly_usage = 0 WHERE deleted = FALSE;