-- =====================================================
-- 스프링 부트 자동 실행용 더미 데이터
-- 애플리케이션 시작 시 자동으로 실행됩니다
-- =====================================================

-- 기존 더미 데이터 삭제 (재실행 시 중복 방지)
DELETE FROM ai_classifications WHERE classification_id LIKE 'auto-%';
DELETE FROM api_parameter WHERE parameter_id LIKE 'auto-%';
DELETE FROM external_api WHERE api_id LIKE 'auto-%' OR api_id = 'API_F17550B2';
DELETE FROM api_keys WHERE key_id LIKE 'AUTO_%';

-- =====================================================
-- API 키 더미 데이터
-- =====================================================
INSERT INTO api_keys (
    key_id, organization_name, organization_code, contact_email, contact_phone,
    api_service_name, api_service_url, api_key, secret_key,
    daily_limit, monthly_limit, current_daily_usage, current_monthly_usage,
    issued_at, expires_at, status, description, created_at, updated_at, deleted
) VALUES
-- SGIS 통계청 API 키들
('AUTO_SEOUL_SGIS', '서울시청', 'SEOUL001', 'gis@seoul.go.kr', '02-120',
 'SGIS', 'https://sgisapi.kostat.go.kr', '31e0df80d73b4e8b9862', 'sgis_secret_seoul_67890',
 1000, 30000, 45, 1250,
 '2024-01-01 09:00:00', '2024-12-31 23:59:59', 'ACTIVE', '서울시청 SGIS API 키', NOW(), NOW(), false),

('AUTO_BUSAN_SGIS', '부산광역시청', 'BUSAN001', 'gis@busan.go.kr', '051-120',
 'SGIS', 'https://sgisapi.kostat.go.kr', 'sgis_api_key_busan_12345', 'sgis_secret_busan_67890',
 800, 24000, 32, 890,
 '2024-01-15 10:30:00', '2024-12-31 23:59:59', 'ACTIVE', '부산시청 SGIS API 키', NOW(), NOW(), false),

-- 카카오 API 키들
('AUTO_UNIV_KAKAO', '서울대학교', 'SNU001', 'api@snu.ac.kr', '02-880-5114',
 'KAKAO', 'https://dapi.kakao.com', 'kakao_rest_api_key_snu_abcdef123456', NULL,
 5000, 150000, 234, 7890,
 '2024-02-01 14:20:00', '2025-01-31 23:59:59', 'ACTIVE', '서울대학교 연구용 카카오 API 키', NOW(), NOW(), false),

-- 네이버 API 키들
('AUTO_NEWS_NAVER', '중앙일보', 'JOONGANG001', 'api@joongang.co.kr', '02-751-5114',
 'NAVER', 'https://openapi.naver.com', 'naver_client_id_joongang_123', 'naver_client_secret_joongang_456',
 25000, 750000, 5234, 156780,
 '2024-01-05 16:00:00', '2024-12-31 23:59:59', 'ACTIVE', '중앙일보 뉴스 검색용 네이버 API', NOW(), NOW(), false);

-- =====================================================
-- 외부 API 더미 데이터
-- =====================================================
INSERT INTO external_api (
    api_id, api_name, api_url, api_issuer, api_owner,
    api_domain, api_keyword, api_key_id, api_token, token_expires_at,
    auto_token_refresh, http_method, api_description, api_effectiveness,
    created_at, updated_at, deleted
) VALUES
-- 정부/공공 도메인 API들
('auto-gov-001', 'SGIS 행정구역별 인구밀도 조회', 
 'https://sgisapi.kostat.go.kr/OpenAPI3/population/density.json',
 '통계청', 'admin-seoul',
 'GOVERNMENT', 'PUBLIC_DATA', 'AUTO_SEOUL_SGIS', 'sgis_access_token_seoul_abc123', '2024-12-31 23:59:59',
 true, 'GET', '통계청 SGIS API를 통한 행정구역별 인구밀도 조회', true,
 NOW(), NOW(), false),

('auto-gov-002', 'SGIS 사업체 현황 조회',
 'https://sgisapi.kostat.go.kr/OpenAPI3/business/listTotalBusinessByAdmDiv.json',
 '통계청', 'admin-busan',
 'GOVERNMENT', 'PUBLIC_DATA', 'AUTO_BUSAN_SGIS', 'sgis_access_token_busan_def456', '2024-12-31 23:59:59',
 true, 'GET', '통계청 SGIS API를 통한 사업체 현황 조회', true,
 NOW(), NOW(), false),

-- 기타/기술 도메인 API들
('auto-tech-001', 'REST API 문서화 서비스',
 'https://api.docs.example.com/v1/documentation',
 'TechCorp', 'admin-tech',
 'OTHERS', 'API_DOCUMENT', 'AUTO_UNIV_KAKAO', NULL, NULL,
 false, 'GET', 'API 문서 자동 생성 및 관리 서비스', true,
 NOW(), NOW(), false),

('API_F17550B2', 'Simple API',
 'https://api.simple.example.com/v1/data',
 'SimpleAPI Inc', 'admin-simple',
 'OTHERS', 'API_DOCUMENT', NULL, NULL, NULL,
 false, 'GET', '간단한 데이터 조회 API 서비스', true,
 NOW(), NOW(), false),

-- 뉴스 도메인 API들
('auto-news-001', '네이버 뉴스 검색',
 'https://openapi.naver.com/v1/search/news.json',
 '네이버', 'admin-news',
 'NEWS', 'BREAKING_NEWS', 'AUTO_NEWS_NAVER', NULL, NULL,
 false, 'GET', '네이버 검색 API를 통한 뉴스 검색', true,
 NOW(), NOW(), false),

('auto-news-002', '네이버 블로그 검색',
 'https://openapi.naver.com/v1/search/blog.json',
 '네이버', 'admin-news',
 'NEWS', 'BREAKING_NEWS', 'AUTO_NEWS_NAVER', NULL, NULL,
 false, 'GET', '네이버 검색 API를 통한 블로그 검색', true,
 NOW(), NOW(), false),

-- 날씨 도메인 API들
('auto-weather-001', '기상청 동네예보 조회',
 'http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst',
 '기상청', 'admin-weather',
 'WEATHER', 'CURRENT_WEATHER', NULL, NULL, NULL,
 false, 'GET', '기상청 단기예보 API', true,
 NOW(), NOW(), false),

('auto-weather-002', '대기질 정보 조회',
 'http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/getCtprvnRltmMesureDnsty',
 '환경부', 'admin-env',
 'WEATHER', 'AIR_QUALITY', NULL, NULL, NULL,
 false, 'GET', '전국 실시간 대기질 정보 조회', true,
 NOW(), NOW(), false),

-- 금융 도메인 API들
('auto-finance-001', 'KRX 주가 정보',
 'https://api.finance.example.com/v1/stock/price',
 'KRX', 'admin-finance',
 'FINANCE', 'STOCK_PRICE', NULL, NULL, NULL,
 false, 'GET', 'KRX 실시간 주가 정보 조회', true,
 NOW(), NOW(), false),

('auto-finance-002', '환율 정보 조회',
 'https://api.exchange.example.com/v1/rates',
 '한국은행', 'admin-bank',
 'FINANCE', 'EXCHANGE_RATE', NULL, NULL, NULL,
 false, 'GET', '실시간 환율 정보 조회 서비스', true,
 NOW(), NOW(), false);

-- =====================================================
-- API 파라미터 더미 데이터
-- =====================================================
INSERT INTO api_parameter (
    parameter_id, api_id, param_name, param_type, is_required, 
    default_value, param_description, additional_fields,
    created_at, updated_at, deleted
) VALUES
-- SGIS 인구밀도 API 파라미터들
('auto-param-001', 'auto-gov-001', 'accessToken', 'STRING', true, NULL, 
 'SGIS 접근 토큰', '{"example": "your_sgis_access_token"}',
 NOW(), NOW(), false),
('auto-param-002', 'auto-gov-001', 'year', 'STRING', true, '2020', 
 '조회 연도', '{"format": "YYYY", "range": "2015-2023"}',
 NOW(), NOW(), false),
('auto-param-003', 'auto-gov-001', 'adm_cd', 'STRING', false, NULL, 
 '행정구역 코드', '{"example": "11", "description": "시도코드 또는 시군구코드"}',
 NOW(), NOW(), false),

-- SGIS 사업체 현황 API 파라미터들  
('auto-param-004', 'auto-gov-002', 'accessToken', 'STRING', true, NULL,
 'SGIS 접근 토큰', '{"example": "your_sgis_access_token"}',
 NOW(), NOW(), false),
('auto-param-005', 'auto-gov-002', 'year', 'STRING', true, '2022',
 '조회 연도', '{"format": "YYYY", "range": "2015-2023"}',
 NOW(), NOW(), false),
('auto-param-006', 'auto-gov-002', 'adm_cd', 'STRING', true, NULL,
 '행정구역 코드', '{"example": "11110", "required": true}',
 NOW(), NOW(), false),

-- API 문서 서비스 파라미터들
('auto-param-007', 'auto-tech-001', 'format', 'STRING', false, 'json',
 '응답 형식', '{"options": ["json", "xml", "yaml"], "default": "json"}',
 NOW(), NOW(), false),
('auto-param-008', 'auto-tech-001', 'version', 'STRING', false, 'v1',
 'API 버전', '{"example": "v1", "description": "API 문서 버전"}',
 NOW(), NOW(), false),

-- Simple API 파라미터들
('auto-param-009', 'API_F17550B2', 'limit', 'INTEGER', false, '10',
 '결과 제한 수', '{"range": "1-100", "default": 10}',
 NOW(), NOW(), false),
('auto-param-010', 'API_F17550B2', 'offset', 'INTEGER', false, '0',
 '시작 위치', '{"minimum": 0, "default": 0}',
 NOW(), NOW(), false),

-- 네이버 뉴스 검색 파라미터들
('auto-param-011', 'auto-news-001', 'query', 'STRING', true, NULL,
 '검색어', '{"example": "리액트", "maxLength": 50}',
 NOW(), NOW(), false),
('auto-param-012', 'auto-news-001', 'display', 'INTEGER', false, '10',
 '검색 결과 출력 건수', '{"range": "1-100", "default": 10}',
 NOW(), NOW(), false),
('auto-param-013', 'auto-news-001', 'start', 'INTEGER', false, '1',
 '검색 시작 위치', '{"range": "1-1000", "default": 1}',
 NOW(), NOW(), false),

-- 기상청 동네예보 파라미터들
('auto-param-014', 'auto-weather-001', 'serviceKey', 'STRING', true, NULL,
 '공공데이터포털에서 받은 인증키', '{"security": true}',
 NOW(), NOW(), false),
('auto-param-015', 'auto-weather-001', 'base_date', 'STRING', true, NULL,
 '발표일자', '{"format": "YYYYMMDD", "example": "20231201"}',
 NOW(), NOW(), false),
('auto-param-016', 'auto-weather-001', 'base_time', 'STRING', true, NULL,
 '발표시각', '{"format": "HHMM", "example": "0500"}',
 NOW(), NOW(), false),
('auto-param-017', 'auto-weather-001', 'nx', 'INTEGER', true, NULL,
 '예보지점 X 좌표', '{"example": 55}',
 NOW(), NOW(), false),
('auto-param-018', 'auto-weather-001', 'ny', 'INTEGER', true, NULL,
 '예보지점 Y 좌표', '{"example": 127}',
 NOW(), NOW(), false),

-- 대기질 정보 파라미터들
('auto-param-019', 'auto-weather-002', 'serviceKey', 'STRING', true, NULL,
 '공공데이터포털 인증키', '{"security": true}',
 NOW(), NOW(), false),
('auto-param-020', 'auto-weather-002', 'returnType', 'STRING', false, 'json',
 '리턴타입', '{"options": ["xml", "json"], "default": "json"}',
 NOW(), NOW(), false),
('auto-param-021', 'auto-weather-002', 'sidoName', 'STRING', true, NULL,
 '시도명', '{"example": "서울", "description": "전국, 서울, 부산, 대구 등"}',
 NOW(), NOW(), false),

-- 주가 정보 파라미터들
('auto-param-022', 'auto-finance-001', 'symbol', 'STRING', true, NULL,
 '종목 코드', '{"example": "005930", "description": "6자리 종목코드"}',
 NOW(), NOW(), false),
('auto-param-023', 'auto-finance-001', 'market', 'STRING', false, 'KOSPI',
 '시장 구분', '{"options": ["KOSPI", "KOSDAQ"], "default": "KOSPI"}',
 NOW(), NOW(), false),

-- 환율 정보 파라미터들
('auto-param-024', 'auto-finance-002', 'currency', 'STRING', true, 'USD',
 '통화 코드', '{"example": "USD", "description": "ISO 통화 코드"}',
 NOW(), NOW(), false),
('auto-param-025', 'auto-finance-002', 'date', 'STRING', false, NULL,
 '조회 날짜', '{"format": "YYYY-MM-DD", "description": "미지정시 최신"}',
 NOW(), NOW(), false);

-- =====================================================
-- AI 분류 결과 더미 데이터
-- =====================================================
INSERT INTO ai_classifications (
    classification_id, api_id, classified_domain, classified_keyword, 
    classified_at, deleted
) VALUES
('auto-class-001', 'auto-gov-001', 'GOVERNMENT', 'PUBLIC_DATA', NOW(), false),
('auto-class-002', 'auto-gov-002', 'GOVERNMENT', 'PUBLIC_DATA', NOW(), false),
('auto-class-003', 'auto-tech-001', 'OTHERS', 'API_DOCUMENT', NOW(), false),
('auto-class-004', 'API_F17550B2', 'OTHERS', 'API_DOCUMENT', NOW(), false),
('auto-class-005', 'auto-news-001', 'NEWS', 'BREAKING_NEWS', NOW(), false),
('auto-class-006', 'auto-news-002', 'NEWS', 'BREAKING_NEWS', NOW(), false),
('auto-class-007', 'auto-weather-001', 'WEATHER', 'CURRENT_WEATHER', NOW(), false),
('auto-class-008', 'auto-weather-002', 'WEATHER', 'AIR_QUALITY', NOW(), false),
('auto-class-009', 'auto-finance-001', 'FINANCE', 'STOCK_PRICE', NOW(), false),
('auto-class-010', 'auto-finance-002', 'FINANCE', 'EXCHANGE_RATE', NOW(), false);