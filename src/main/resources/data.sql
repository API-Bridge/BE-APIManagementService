-- ============================================
-- 1. API Domains 데이터 추가
-- ============================================
INSERT INTO api_domains (domain_id, domain_name, description) VALUES
                                                                  (1, 'finance', '금융 (주식, 환율, 금리 등)'),
                                                                  (2, 'weather', '날씨 (현재 날씨, 예보, 대기질 등)'),
                                                                  (3, 'news', '뉴스 (속보, 정치, 경제, 사회 등)'),
                                                                  (4, 'transportation', '교통 (지하철, 버스, 교통상황 등)'),
                                                                  (5, 'commerce', '커머스 (상품 정보, 가격 비교, 쇼핑 등)'),
                                                                  (6, 'government', '공공/정부 (공공데이터, 법률, 통계 등)'),
                                                                  (7, 'entertainment', '엔터테인먼트 (영화, 음악, TV 등)'),
                                                                  (8, 'sports', '스포츠 (경기 결과, 선수 정보 등)'),
                                                                  (9, 'healthcare', '건강/의료 (병원, 의약품, 건강 정보 등)'),
                                                                  (10, 'education', '교육 (시험 일정, 강좌, 장학금 등)'),
                                                                  (11, 'realestate', '부동산 (주택 시세, 임대 정보 등)'),
                                                                  (12, 'travel', '여행 (항공, 숙박, 관광지 등)'),
                                                                  (13, 'technology', '기술 (API 문서, 기술 트렌드 등)'),
                                                                  (14, 'lifestyle', '생활/문화 (패션, 뷰티, 요리법 등)'),
                                                                  (15, 'others', '기타');

-- ============================================
-- 2. API Keywords 데이터 추가
-- ============================================
INSERT INTO api_keywords (domain_id, keyword_name) VALUES
-- 금융 (domain_id: 1)
(1, 'stock_price'), (1, 'stock_index'), (1, 'exchange_rate'), (1, 'cryptocurrency'), (1, 'interest_rate'), (1, 'economic_indicator'),

-- 날씨 (domain_id: 2)
(2, 'current_weather'), (2, 'weather_forecast'), (2, 'air_quality'), (2, 'temperature'), (2, 'precipitation'), (2, 'uv_index'),

-- 뉴스 (domain_id: 3)
(3, 'breaking_news'), (3, 'politics'), (3, 'economy_news'), (3, 'tech_news'), (3, 'sports_news'), (3, 'social_trend'),

-- 교통 (domain_id: 4)
(4, 'subway_info'), (4, 'bus_info'), (4, 'traffic_condition'), (4, 'parking_info'), (4, 'taxi_fare'),

-- 커머스 (domain_id: 5)
(5, 'product_info'), (5, 'price_comparison'), (5, 'product_review'), (5, 'coupon_discount'), (5, 'shopping_rank'),

-- 공공/정부 (domain_id: 6)
(6, 'public_data'), (6, 'legal_info'), (6, 'statistics'), (6, 'public_service'), (6, 'policy_info'),

-- 엔터테인먼트 (domain_id: 7)
(7, 'movie_info'), (7, 'music_chart'), (7, 'tv_schedule'), (7, 'celebrity_news'), (7, 'game_info'),

-- 스포츠 (domain_id: 8)
(8, 'soccer_result'), (8, 'baseball_result'), (8, 'basketball_result'), (8, 'sports_schedule'), (8, 'player_stats'), (8, 'team_ranking'),

-- 건강/의료 (domain_id: 9)
(9, 'hospital_info'), (9, 'health_tip'), (9, 'medicine_info'), (9, 'fitness_data'),

-- 교육 (domain_id: 10)
(10, 'exam_schedule'), (10, 'course_info'), (10, 'scholarship'), (10, 'school_info'),

-- 부동산 (domain_id: 11)
(11, 'house_price'), (11, 'rent_info'), (11, 'real_estate_trend'),

-- 여행 (domain_id: 12)
(12, 'flight_info'), (12, 'hotel_info'), (12, 'tourist_spot'), (12, 'restaurant_info'),

-- 기술 (domain_id: 13)
(13, 'api_document'), (13, 'tech_trend'), (13, 'developer_tool'),

-- 생활/문화 (domain_id: 14)
(14, 'fashion_trend'), (14, 'beauty_tip'), (14, 'recipe'), (14, 'interior_tip');

-- ============================================
-- 3. API Credentials 데이터 추가
-- ============================================
INSERT INTO api_credentials (credential_id, organization_name, api_key, secret_key, status, issued_at, contact_email, created_at, updated_at) VALUES
('weather-service-001', 'OpenWeatherMap', 'openweather_api_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'admin@openweathermap.org', NOW(), NOW()),
('finance-service-001', 'Alpha Vantage', 'alphavantage_api_key_12345', 'alphavantage_secret_12345', 'ACTIVE', '2024-01-01 00:00:00', 'support@alphavantage.co', NOW(), NOW()),
('news-service-001', 'NewsAPI', 'newsapi_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'contact@newsapi.org', NOW(), NOW()),
('transport-service-001', '서울시 열린데이터광장', 'seoul_transport_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'data@seoul.go.kr', NOW(), NOW()),
('government-service-001', '공공데이터포털', 'data_go_kr_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'help@data.go.kr', NOW(), NOW());

-- ============================================
-- 4. External API Specs 데이터 추가
-- ============================================
INSERT INTO external_api_specs (api_id, api_name, api_description, api_issuer, api_url, http_method, is_active, health_status, health_check_path, credential_id, domain_id, keyword_id, created_at, updated_at) VALUES
-- 날씨 관련 API
('weather-current-001', '현재 날씨 조회', '지역별 현재 날씨 정보를 제공하는 API', 'OpenWeatherMap', 'https://api.openweathermap.org/data/2.5/weather', 'GET', true, 'HEALTHY', '/health', 'weather-service-001', 2, 7, NOW(), NOW()),
('weather-forecast-001', '날씨 예보 조회', '5일간 3시간 단위 날씨 예보를 제공하는 API', 'OpenWeatherMap', 'https://api.openweathermap.org/data/2.5/forecast', 'GET', true, 'HEALTHY', '/health', 'weather-service-001', 2, 8, NOW(), NOW()),
('weather-air-quality-001', '대기질 조회', '현재 대기질 지수를 제공하는 API', 'OpenWeatherMap', 'https://api.openweathermap.org/data/2.5/air_pollution', 'GET', true, 'UNKNOWN', '/health', 'weather-service-001', 2, 9, NOW(), NOW()),

-- 금융 관련 API
('finance-stock-001', '실시간 주식 가격', '실시간 주식 가격 정보를 제공하는 API', 'Alpha Vantage', 'https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY', 'GET', true, 'HEALTHY', '/status', 'finance-service-001', 1, 1, NOW(), NOW()),
('finance-exchange-001', '환율 정보 조회', '실시간 환율 정보를 제공하는 API', 'Alpha Vantage', 'https://www.alphavantage.co/query?function=FX_INTRADAY', 'GET', true, 'HEALTHY', '/status', 'finance-service-001', 1, 3, NOW(), NOW()),
('finance-crypto-001', '암호화폐 가격 조회', '주요 암호화폐 실시간 가격을 제공하는 API', 'Alpha Vantage', 'https://www.alphavantage.co/query?function=CRYPTO_INTRADAY', 'GET', true, 'UNHEALTHY', '/status', 'finance-service-001', 1, 4, NOW(), NOW()),

-- 뉴스 관련 API
('news-breaking-001', '속보 뉴스 조회', '최신 속보 뉴스를 제공하는 API', 'NewsAPI', 'https://newsapi.org/v2/top-headlines', 'GET', true, 'HEALTHY', '/ping', 'news-service-001', 3, 13, NOW(), NOW()),
('news-tech-001', '기술 뉴스 조회', '기술 관련 최신 뉴스를 제공하는 API', 'NewsAPI', 'https://newsapi.org/v2/everything', 'GET', true, 'HEALTHY', '/ping', 'news-service-001', 3, 15, NOW(), NOW()),

-- 교통 관련 API
('transport-subway-001', '지하철 실시간 정보', '서울 지하철 실시간 도착 정보를 제공하는 API', '서울시 열린데이터광장', 'http://swopenapi.seoul.go.kr/api/subway', 'GET', true, 'HEALTHY', '/health', 'transport-service-001', 4, 19, NOW(), NOW()),
('transport-bus-001', '버스 실시간 정보', '서울 버스 실시간 위치 정보를 제공하는 API', '서울시 열린데이터광장', 'http://ws.bus.go.kr/api/rest/buspos', 'GET', true, 'UNKNOWN', '/health', 'transport-service-001', 4, 20, NOW(), NOW()),
('transport-traffic-001', '실시간 교통정보', '서울시 실시간 교통 상황을 제공하는 API', '서울시 열린데이터광장', 'http://openapi.seoul.go.kr/api/traffic', 'GET', true, 'HEALTHY', '/health', 'transport-service-001', 4, 21, NOW(), NOW()),

-- 공공/정부 관련 API
('gov-public-data-001', '공공데이터 조회', '정부 공공데이터를 제공하는 통합 API', '공공데이터포털', 'https://www.data.go.kr/api/15000581', 'GET', true, 'HEALTHY', '/status', 'government-service-001', 6, 26, NOW(), NOW()),
('gov-statistics-001', '국가통계 조회', '국가 주요 통계 데이터를 제공하는 API', '공공데이터포털', 'https://www.data.go.kr/api/15000582', 'GET', true, 'HEALTHY', '/status', 'government-service-001', 6, 28, NOW(), NOW());

-- ============================================
-- 5. API Parameters 데이터 추가
-- ============================================
INSERT INTO api_parameters (parameter_id, param_name, param_type, is_required, param_description, default_value, additional_fields, api_id, created_at, updated_at) VALUES
-- weather-current-001 파라미터들
('param-weather-current-001', 'lat', 'double', true, '위도', NULL, NULL, 'weather-current-001', NOW(), NOW()),
('param-weather-current-002', 'lon', 'double', true, '경도', NULL, NULL, 'weather-current-001', NOW(), NOW()),
('param-weather-current-003', 'appid', 'string', true, 'API 키', NULL, NULL, 'weather-current-001', NOW(), NOW()),
('param-weather-current-004', 'units', 'string', false, '단위 (metric, imperial, kelvin)', 'metric', NULL, 'weather-current-001', NOW(), NOW()),
('param-weather-current-005', 'lang', 'string', false, '언어 코드', 'kr', NULL, 'weather-current-001', NOW(), NOW()),

-- weather-forecast-001 파라미터들
('param-weather-forecast-001', 'lat', 'double', true, '위도', NULL, NULL, 'weather-forecast-001', NOW(), NOW()),
('param-weather-forecast-002', 'lon', 'double', true, '경도', NULL, NULL, 'weather-forecast-001', NOW(), NOW()),
('param-weather-forecast-003', 'appid', 'string', true, 'API 키', NULL, NULL, 'weather-forecast-001', NOW(), NOW()),
('param-weather-forecast-004', 'units', 'string', false, '단위 (metric, imperial, kelvin)', 'metric', NULL, 'weather-forecast-001', NOW(), NOW()),
('param-weather-forecast-005', 'cnt', 'integer', false, '예보 개수 (최대 40)', '40', NULL, 'weather-forecast-001', NOW(), NOW()),

-- finance-stock-001 파라미터들
('param-finance-stock-001', 'function', 'string', true, 'API 기능', 'TIME_SERIES_INTRADAY', NULL, 'finance-stock-001', NOW(), NOW()),
('param-finance-stock-002', 'symbol', 'string', true, '주식 심볼 (예: IBM, AAPL)', NULL, NULL, 'finance-stock-001', NOW(), NOW()),
('param-finance-stock-003', 'interval', 'string', true, '시간 간격 (1min, 5min, 15min, 30min, 60min)', '1min', NULL, 'finance-stock-001', NOW(), NOW()),
('param-finance-stock-004', 'apikey', 'string', true, 'API 키', NULL, NULL, 'finance-stock-001', NOW(), NOW()),

-- news-breaking-001 파라미터들
('param-news-breaking-001', 'country', 'string', false, '국가 코드 (kr, us, gb 등)', 'kr', NULL, 'news-breaking-001', NOW(), NOW()),
('param-news-breaking-002', 'category', 'string', false, '뉴스 카테고리', 'general', NULL, 'news-breaking-001', NOW(), NOW()),
('param-news-breaking-003', 'pageSize', 'integer', false, '페이지당 기사 수 (최대 100)', '20', NULL, 'news-breaking-001', NOW(), NOW()),
('param-news-breaking-004', 'apiKey', 'string', true, 'API 키', NULL, NULL, 'news-breaking-001', NOW(), NOW()),

-- transport-subway-001 파라미터들
('param-transport-subway-001', 'key', 'string', true, 'API 키', NULL, NULL, 'transport-subway-001', NOW(), NOW()),
('param-transport-subway-002', 'type', 'string', true, '응답 형식 (json, xml)', 'json', NULL, 'transport-subway-001', NOW(), NOW()),
('param-transport-subway-003', 'service', 'string', true, '서비스명', 'SearchArrivalInfoBySubwayInfo', NULL, 'transport-subway-001', NOW(), NOW()),
('param-transport-subway-004', 'start_index', 'integer', false, '시작 인덱스', '1', NULL, 'transport-subway-001', NOW(), NOW()),
('param-transport-subway-005', 'end_index', 'integer', false, '끝 인덱스', '5', NULL, 'transport-subway-001', NOW(), NOW()),
('param-transport-subway-006', 'station_name', 'string', true, '지하철역명', NULL, NULL, 'transport-subway-001', NOW(), NOW()),

-- gov-public-data-001 파라미터들
('param-gov-data-001', 'serviceKey', 'string', true, '공공데이터포털 API 키', NULL, NULL, 'gov-public-data-001', NOW(), NOW()),
('param-gov-data-002', 'numOfRows', 'integer', false, '한 페이지 결과 수', '10', NULL, 'gov-public-data-001', NOW(), NOW()),
('param-gov-data-003', 'pageNo', 'integer', false, '페이지번호', '1', NULL, 'gov-public-data-001', NOW(), NOW()),
('param-gov-data-004', 'MobileOS', 'string', true, 'OS 구분', 'ETC', NULL, 'gov-public-data-001', NOW(), NOW()),
('param-gov-data-005', 'MobileApp', 'string', true, '서비스명', 'APIBridge', NULL, 'gov-public-data-001', NOW(), NOW());