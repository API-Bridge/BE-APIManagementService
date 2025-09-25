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
(2, 'current_weather'), (2, 'weather_forecast'), (2, 'air_quality'), (2, 'temperature'), (2, 'precipitation'), (2, 'uv_index'), (2, 'typhoon'),

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
(14, 'fashion_trend'), (14, 'beauty_tip'), (14, 'recipe'), (14, 'interior_tip'),

-- 날씨 추가 키워드 (domain_id: 2)
(2, 'heatwave_report');

-- ============================================
-- 3. API Credentials 데이터 추가
-- ============================================
INSERT INTO api_credentials (credential_id, organization_name, api_key, secret_key, status, issued_at, contact_email, created_at, updated_at) VALUES
('weather-service-001', 'OpenWeatherMap', 'openweather_api_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'admin@openweathermap.org', NOW(), NOW()),
('finance-service-001', 'Alpha Vantage', 'alphavantage_api_key_12345', 'alphavantage_secret_12345', 'ACTIVE', '2024-01-01 00:00:00', 'support@alphavantage.co', NOW(), NOW()),
('news-service-001', 'NewsAPI', 'newsapi_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'contact@newsapi.org', NOW(), NOW()),
('transport-service-001', '서울시 열린데이터광장', 'seoul_transport_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'data@seoul.go.kr', NOW(), NOW()),
('SGIS', '지리정보서비스', '9801df6404684fdabe3d', '31e0df80d73b4e8b9862', 'ACTIVE', '2025-09-03 00:00:00', '02)2012-9114', NOW(), NOW()),
('government-service-001', '공공데이터포털', 'data_go_kr_key_12345', NULL, 'ACTIVE', '2024-01-01 00:00:00', 'help@data.go.kr', NOW(), NOW());

-- ============================================
-- 4. External API Specs 데이터 추가
-- ============================================
INSERT INTO external_api_specs (api_id, api_name, api_description, api_issuer, api_url, http_method, is_active, health_status, health_check_path, credential_id, domain_id, keyword_id, created_at, updated_at) VALUES
('3d0ad254-a587-47f1-9627-8c5a75fdb53f', '과거 폭염특보 목록', '과거 폭염특보 리스트를 반환하는 API', 'SGIS', 'https://sgisapi.kostat.go.kr/OpenAPI3/ndsm/prevHwSpcnwsList.json', 'GET', true, 'UNKNOWN', NULL, 'SGIS', 2, 68, NOW(), NOW()),
('housing-stats-001', '주택통계', '(인구주택총조사) 주택 통계 제공 API', 'SGIS', 'https://sgisapi.kostat.go.kr/OpenAPI3/stats/house.json', 'GET', true, 'UNKNOWN', NULL, 'SGIS', 11, 56, NOW(), NOW()),
('typhoon-information-of-year','년도별 태풍정보 목록', '년도별 발생한 태풍정보 목록을 반환하는 API', 'SGIS', 'https://sgisapi.kostat.go.kr/OpenAPI3/ndsm/typInfoYearList.json', 'GET', true, 'UNKNOWN', '/health','SGIS', 2,13, NOW(), NOW());


-- ============================================
-- 5. API Parameters 데이터 추가
-- ============================================
INSERT INTO api_parameters (parameter_id, param_name, param_type, is_required, param_description, default_value, additional_fields, api_id, created_at, updated_at) VALUES
-- 과거 폭염특보 목록 파라미터들
('Heat-wave-warning-001', 'accessToken', 'String', TRUE, '액세스키', NULL, NULL, '3d0ad254-a587-47f1-9627-8c5a75fdb53f', NOW(), NOW()),
('Heat-wave-warning-002', 'searchYear', 'String', TRUE, '폭염 발생 년도', '2023', NULL, '3d0ad254-a587-47f1-9627-8c5a75fdb53f', NOW(), NOW()),
('Heat-wave-warning-003', 'searchMonth', 'String', TRUE, '폭염 발생 월', NULL, NULL, '3d0ad254-a587-47f1-9627-8c5a75fdb53f', NOW(), NOW()),

-- 주택통계 파라미터들
('housing-stats-param-001', 'accessToken', 'string', TRUE, '액세스키', '', NULL, 'housing-stats-001', NOW(), NOW()),
('housing-stats-param-002', 'year', 'string', TRUE, '조회연도', '', NULL, 'housing-stats-001', NOW(), NOW()),

-- 발생한 태풍기록 파라미터들
('Typhoon-record-of-year-001', 'accessToken', 'String', TRUE, '액세스키', '', NULL, 'typhoon-information-of-year', NOW(), NOW()),
('Typhoon-record-of-year-002', 'typnOcrnYr', 'String', TRUE, '태풍발생 년도', '', NULL, 'typhoon-information-of-year', NOW(), NOW());