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