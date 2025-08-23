# API Management Service 데이터베이스 설계 문서

## 개요

API Management Service는 외부 API 키 관리, API 메타데이터 관리, AI 기반 API 분류, 그리고 API 파라미터 관리를 위한 시스템입니다. 이 문서는 서비스에서 사용하는 모든 데이터베이스 테이블의 구조와 목적을 설명합니다.

## 시스템 아키텍처 특징

- **MSA(Microservice Architecture) 지원**: 외래키 제약조건을 제거하여 서비스 간 느슨한 결합 유지
- **Soft Delete 패턴**: 모든 테이블에서 `deleted` 플래그를 사용하여 데이터 복구 가능
- **JPA Auditing**: 생성/수정 일시 자동 관리
- **AI 분류 시스템**: 15개 도메인, 66개 키워드를 통한 자동 API 분류

---

## 테이블 구조

### 1. api_keys - API 키 관리 테이블

**목적**: 외부 API 서비스의 API 키 정보를 체계적으로 관리하는 핵심 테이블

**주요 기능**:
- 기관별 API 키 발급 및 관리
- 일일/월간 사용량 제한 및 모니터링  
- API 키 만료일 관리
- 사용량 통계 및 이력 추적

#### 테이블 구조
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `key_id` | VARCHAR(100) | PRIMARY KEY | 고유 API 키 ID (예: ORG_001, ORG_002) |
| `organization_name` | VARCHAR(255) | NOT NULL | 기관명 (예: 서울시청, 부산대학교) |
| `organization_code` | VARCHAR(100) | | 기관 코드 (예: SEOUL001, PNU001) |
| `contact_email` | VARCHAR(255) | NOT NULL | 연락처 이메일 |
| `contact_phone` | VARCHAR(50) | | 연락처 전화번호 |
| `api_service_name` | VARCHAR(100) | NOT NULL | API 서비스명 (예: SGIS, KAKAO, NAVER) |
| `api_service_url` | VARCHAR(500) | | API 서비스 URL |
| `api_key` | VARCHAR(500) | NOT NULL | 발급받은 실제 API 키 |
| `secret_key` | VARCHAR(500) | | 발급받은 시크릿 키 |
| `daily_limit` | INT | | 일일 API 호출 제한 횟수 |
| `monthly_limit` | INT | | 월간 API 호출 제한 횟수 |
| `current_daily_usage` | INT | DEFAULT 0 | 현재 일일 사용량 |
| `current_monthly_usage` | INT | DEFAULT 0 | 현재 월간 사용량 |
| `issued_at` | TIMESTAMP | NOT NULL | API 키 발급일시 |
| `expires_at` | TIMESTAMP | | API 키 만료일시 |
| `last_used_at` | TIMESTAMP | | 마지막 사용일시 |
| `status` | ENUM | NOT NULL | API 키 상태 (ACTIVE, INACTIVE, EXPIRED, SUSPENDED, REVOKED, PENDING) |
| `description` | TEXT | | API 키 설명 |
| `requested_apis` | TEXT | | 요청한 API 목록 (JSON 형태) |

#### API Key Status 상세
- `ACTIVE`: 정상적으로 사용 가능한 상태
- `INACTIVE`: 일시적으로 사용이 중단된 상태
- `EXPIRED`: 사용 기간이 만료된 상태
- `SUSPENDED`: 정책 위반 등으로 정지된 상태
- `REVOKED`: 사용 권한이 취소된 상태
- `PENDING`: 승인 대기 중인 상태

---

### 2. external_api - 외부 API 메타데이터 테이블

**목적**: 등록된 외부 API의 메타데이터와 AI 분류 결과를 저장하는 테이블

**주요 기능**:
- API 등록 및 메타데이터 관리
- AI 기반 자동 도메인/키워드 분류
- API 인증 정보 및 토큰 관리
- API 유효성 상태 추적

#### 테이블 구조
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `api_id` | VARCHAR(36) | PRIMARY KEY | 외부 API의 고유 식별자 |
| `api_name` | VARCHAR(255) | NOT NULL | API의 이름 |
| `api_url` | VARCHAR(500) | NOT NULL | API의 URL 주소 |
| `api_issuer` | VARCHAR(255) | NOT NULL | API 발급처 |
| `api_owner` | VARCHAR(36) | | API를 추가한 사용자 ID |
| `api_domain` | ENUM | NOT NULL | AI 분류 도메인 (15개 중 1개) |
| `api_keyword` | ENUM | NOT NULL | AI 분류 키워드 (66개 중 1개) |
| `api_key_id` | VARCHAR(100) | | API 키 참조 (FK) |
| `api_token` | TEXT | | API 토큰 (인증용) |
| `token_expires_at` | TIMESTAMP | | 토큰 만료 시간 |
| `auto_token_refresh` | BOOLEAN | DEFAULT TRUE | 토큰 자동 갱신 여부 |
| `http_method` | VARCHAR(10) | NOT NULL | API의 HTTP 메소드 |
| `api_description` | TEXT | | API에 대한 상세 설명 |
| `api_effectiveness` | BOOLEAN | DEFAULT TRUE | API 유효성 상태 |

#### API Domain 분류 (15개)
- `finance`: 금융 (주식, 환율, 암호화폐, 금리 등)
- `weather`: 날씨 (기상정보, 미세먼지, 환경 데이터)
- `news`: 뉴스 (뉴스, 소셜미디어, 트렌드 정보)
- `transportation`: 교통 (대중교통, 교통정보, 지도, 위치)
- `commerce`: 쇼핑 (상품정보, 가격비교, 리뷰, 쿠폰)
- `government`: 정부 (공공데이터, 통계청, 법령 정보)
- `entertainment`: 엔터테인먼트 (영화, 음악, 게임, 방송)
- `sports`: 스포츠 (경기결과, 선수정보, 리그 데이터)
- `healthcare`: 헬스케어 (건강정보, 병원, 의료 데이터)
- `education`: 교육 (학습자료, 강의, 시험정보)
- `realestate`: 부동산 (매매, 전세, 월세, 시세 정보)
- `travel`: 여행 (항공, 숙박, 관광지, 맛집 정보)
- `technology`: 기술 (개발자 정보, 기술 트렌드, API 문서)
- `lifestyle`: 라이프스타일 (패션, 뷰티, 인테리어, 요리)
- `others`: 기타 (분류되지 않은 데이터)

#### API Keyword 분류 (66개)
각 도메인별로 상세 키워드가 정의되어 있습니다:

**금융 (6개)**: stock_price, stock_index, exchange_rate, cryptocurrency, interest_rate, economic_indicator

**날씨 (6개)**: current_weather, weather_forecast, air_quality, temperature, precipitation, uv_index

**뉴스 (6개)**: breaking_news, politics, economy_news, tech_news, sports_news, social_trend

**교통 (5개)**: subway_info, bus_info, traffic_condition, parking_info, taxi_fare

**쇼핑 (5개)**: product_info, price_comparison, product_review, coupon_discount, shopping_rank

**정부 (5개)**: public_data, legal_info, statistics, public_service, policy_info

**엔터테인먼트 (5개)**: movie_info, music_chart, tv_schedule, celebrity_news, game_info

**스포츠 (6개)**: soccer_result, baseball_result, basketball_result, sports_schedule, player_stats, team_ranking

**헬스케어 (4개)**: hospital_info, health_tip, medicine_info, fitness_data

**교육 (4개)**: exam_schedule, course_info, scholarship, school_info

**부동산 (3개)**: house_price, rent_info, real_estate_trend

**여행 (4개)**: flight_info, hotel_info, tourist_spot, restaurant_info

**기술 (3개)**: api_document, tech_trend, developer_tool

**라이프스타일 (4개)**: fashion_trend, beauty_tip, recipe, interior_tip

---

### 3. ai_classifications - AI 분류 결과 테이블

**목적**: AI 자동 분류 과정과 결과를 별도로 추적하기 위한 이력 테이블

**주요 기능**:
- AI 분류 이력 추적
- 분류 정확도 모니터링
- 분류 결과 검증 및 개선

#### 테이블 구조
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `classification_id` | VARCHAR(36) | PRIMARY KEY | Classification ID |
| `api_id` | VARCHAR(36) | NOT NULL | API ID |
| `classified_domain` | ENUM | | 분류된 도메인 |
| `classified_keyword` | ENUM | | 분류된 키워드 |
| `classified_at` | TIMESTAMP | | 분류 일시 |

---

### 4. api_parameter - API 파라미터 정보 테이블

**목적**: API별 파라미터 구조 및 메타데이터를 저장하는 테이블

**주요 기능**:
- API 파라미터 스키마 정의
- 필수/선택 파라미터 구분
- 동적 파라미터 구조 지원 (JSON)
- API 문서화 자동화 지원

#### 테이블 구조
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `parameter_id` | VARCHAR(36) | PRIMARY KEY | 파라미터 정보 고유 식별자 |
| `api_id` | VARCHAR(36) | NOT NULL | 이 파라미터가 속한 API의 ID |
| `param_name` | VARCHAR(255) | NOT NULL | 파라미터 이름 |
| `param_type` | VARCHAR(50) | NOT NULL | 파라미터의 데이터 타입 |
| `is_required` | BOOLEAN | DEFAULT FALSE | 해당 파라미터가 필수인지 여부 |
| `default_value` | TEXT | | 파라미터의 기본값 |
| `param_description` | TEXT | | 파라미터에 대한 설명 |
| `additional_fields` | JSON | | 동적 추가 필드 (API마다 다른 구조 지원) |

#### 동적 필드 활용
`additional_fields` JSON 컬럼을 통해 API마다 다른 파라미터 구조를 유연하게 저장할 수 있습니다:

```json
{
  "validation": {
    "min_length": 5,
    "max_length": 100,
    "pattern": "^[a-zA-Z0-9]+$"
  },
  "examples": ["example1", "example2"],
  "enum_values": ["option1", "option2", "option3"]
}
```

---

## 공통 필드 (BaseEntity 상속)

모든 테이블은 다음 공통 필드를 포함합니다:

| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| `created_at` | TIMESTAMP | NOT NULL | 엔티티 생성 일시 (JPA Auditing) |
| `updated_at` | TIMESTAMP | | 엔티티 마지막 수정 일시 (JPA Auditing) |
| `deleted` | BOOLEAN | DEFAULT FALSE | Soft Delete를 위한 삭제 플래그 |

---

## 유용한 뷰 (Views)

### 1. v_active_apis - 활성 API 키와 연결된 API 목록
```sql
CREATE VIEW v_active_apis AS
SELECT 
    ea.api_id, ea.api_name, ea.api_url, ea.api_domain, ea.api_keyword,
    ak.key_id, ak.organization_name, ak.api_service_name,
    ak.status as api_key_status, ea.api_effectiveness, ea.created_at
FROM external_api ea
LEFT JOIN api_keys ak ON ea.api_key_id = ak.key_id
WHERE ea.deleted = FALSE AND ea.api_effectiveness = TRUE
  AND (ak.status = 'ACTIVE' OR ak.status IS NULL);
```

### 2. v_api_domain_stats - API 도메인별 통계
```sql
CREATE VIEW v_api_domain_stats AS
SELECT 
    api_domain,
    COUNT(*) as total_apis,
    COUNT(CASE WHEN api_effectiveness = TRUE THEN 1 END) as active_apis,
    COUNT(CASE WHEN deleted = TRUE THEN 1 END) as deleted_apis
FROM external_api
GROUP BY api_domain;
```

### 3. v_api_key_usage_stats - API 키 사용량 통계
```sql
CREATE VIEW v_api_key_usage_stats AS
SELECT 
    key_id, organization_name, api_service_name,
    daily_limit, current_daily_usage, monthly_limit, current_monthly_usage,
    ROUND((current_daily_usage / daily_limit) * 100, 2) as daily_usage_percent,
    ROUND((current_monthly_usage / monthly_limit) * 100, 2) as monthly_usage_percent,
    status, expires_at, last_used_at
FROM api_keys
WHERE deleted = FALSE;
```

---

## 인덱스 전략

### 기본 인덱스
- 모든 Primary Key에 자동 인덱스
- `deleted` 플래그에 인덱스 (Soft Delete 성능 최적화)
- `created_at`, `updated_at`에 인덱스 (시간순 정렬 최적화)

### 비즈니스 로직 인덱스
- `api_keys.status`: API 키 상태별 조회 최적화
- `api_keys.expires_at`: 만료 예정 키 조회 최적화
- `external_api.api_domain`, `api_keyword`: AI 분류 기반 검색 최적화
- `external_api.api_effectiveness`: 유효한 API 조회 최적화

### 복합 인덱스
- `(api_domain, api_keyword)`: 도메인+키워드 조합 검색
- `(api_effectiveness, deleted)`: 활성 API 조회
- `(api_service_name, status)`: 서비스별 상태 조회

---

## MSA 설계 고려사항

### 외래키 제약조건 제거
- 서비스 간 느슨한 결합을 위해 데이터베이스 레벨의 외래키 제약조건 제거
- 애플리케이션 레벨에서 참조 무결성 관리
- 각 서비스가 독립적으로 스케일링 가능

### 데이터 일관성 관리
- API 레벨에서 트랜잭션 관리
- 최종 일관성(Eventual Consistency) 모델 적용
- 서비스 간 이벤트 기반 통신으로 데이터 동기화

---

## 성능 최적화 전략

### 쿼리 최적화
- 복합 인덱스를 활용한 조건절 최적화
- 커버링 인덱스를 통한 I/O 최소화
- 뷰를 활용한 복잡한 조인 쿼리 단순화

### 캐싱 전략
- API 메타데이터는 Redis 캐시 활용
- AI 분류 결과는 메모리 캐시 적용
- API 키 상태는 TTL 기반 캐시 운영

### 데이터 아카이빙
- 삭제된 데이터의 정기적 아카이빙
- 오래된 AI 분류 이력 데이터 별도 보관
- 사용량 통계 데이터의 주기적 집계 테이블 생성

---

## 보안 고려사항

### 민감 데이터 보호
- `api_key`, `secret_key`: 암호화 저장 권장
- `api_token`: 별도 보안 스토리지 고려
- 접근 로그 및 감사 추적 구현

### 접근 권한 관리
- 테이블별 세분화된 권한 설정
- API 키별 접근 IP 제한 기능
- 사용량 기반 Rate Limiting 구현

이 문서는 API Management Service의 데이터베이스 설계에 대한 전체적인 이해를 제공하며, 시스템 유지보수 및 확장 시 참고 자료로 활용할 수 있습니다.