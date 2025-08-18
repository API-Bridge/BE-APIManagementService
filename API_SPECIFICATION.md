# API Management Service API 명세서

## 📋 개요

**API Management Service**는 MSA(마이크로서비스 아키텍처) 환경에서 **'API 등기소'**와 같은 역할을 하는 서비스입니다. 시스템에 등록된 모든 외부 데이터 API의 명세(메타데이터)를 중앙에서 관리하고, AI를 통해 지능적으로 분류하여 다른 서비스가 이를 효율적으로 활용할 수 있도록 돕는 것이 핵심 목표입니다.

- **Base URL**: `http://localhost:8080/api/v1`
- **API 버전**: v1
- **문서 버전**: 1.0.0
- **최종 업데이트**: 2024-01-01

## 🔐 인증 및 보안

### Rate Limiting 정책
모든 API는 Rate Limiting이 적용되어 있습니다:

| **API 유형** | **Rate Limit** | **대상 사용자** |
|-------------|----------------|----------------|
| **관리자 작업** | 1시간에 50회 | API 등록/수정/삭제/복사 |
| **일반 조회** | 1시간에 1000회 | API 정보 조회/검색 |
| **통계/분석** | 1시간에 500회 | 데이터 분석 및 통계 |
| **개발 도구** | 1시간에 200회 | API 유효성 검증 |

### 인증 방식
- **API Key**: `X-API-Key` 헤더 또는 `apiKey` 쿼리 파라미터
- **사용자 ID**: `X-User-ID` 헤더 또는 `userId` 쿼리 파라미터
- **세션**: `JSESSIONID` 쿠키

## 📚 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "message": "작업이 성공적으로 완료되었습니다",
  "data": { ... },
  "timestamp": "2024-01-01T00:00:00"
}
```

### 에러 응답
```json
{
  "success": false,
  "message": "오류가 발생했습니다",
  "error": "상세 오류 메시지",
  "timestamp": "2024-01-01T00:00:00"
}
```

### 페이징 응답
```json
{
  "success": true,
  "message": "데이터를 성공적으로 조회했습니다",
  "data": {
    "content": [ ... ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 100,
    "totalPages": 10
  }
}
```

## 🚀 API 엔드포인트

### 📋 **API 탐색 구조 개선**

**API Management Service**는 체계적이고 직관적인 API 탐색을 위해 다음과 같은 구조를 제공합니다:

#### **탐색 방식**
1. **전체 목록 조회**: 모든 API의 개요 파악
2. **도메인별 조회**: 업무 영역별 체계적 탐색
3. **카테고리별 조회**: 기능별 세밀한 분류
4. **상세 조회**: 선택한 API의 완전한 정보

#### **사용자 탐색 플로우**
```
1. 전체 API 목록 조회 → 관심 있는 API 발견
   GET /external-apis?page=0&size=20

2. 도메인별 조회 → 업무 영역별 탐색
   GET /external-apis/domain/GOVERNMENT

3. 카테고리별 조회 → 구체적인 기능별 탐색
   GET /external-apis/category/STATISTICS

4. 상세 조회 → 선택한 API의 완전한 정보
   GET /external-apis/{apiId}
```

#### **도메인과 키워드의 차이점**
- **도메인 (Domain)**: API 제공 기관의 업무 영역
  - `FINANCE`: 금융 (주식, 환율, 암호화폐, 금리 등)
  - `WEATHER`: 날씨 (기상정보, 미세먼지, 환경 데이터)
  - `NEWS`: 뉴스 (뉴스, 소셜미디어, 트렌드 정보)
  - `TRANSPORTATION`: 교통 (대중교통, 교통정보, 지도, 위치 서비스)
  - `COMMERCE`: 쇼핑 (상품정보, 가격비교, 리뷰, 쿠폰)
  - `GOVERNMENT`: 정부 (정부 공공데이터, 통계청, 법령 정보)
  - `ENTERTAINMENT`: 엔터테인먼트 (영화, 음악, 게임, 방송 정보)
  - `SPORTS`: 스포츠 (경기결과, 선수정보, 리그 데이터)
  - `HEALTHCARE`: 헬스케어 (건강정보, 병원, 의료 데이터)
  - `EDUCATION`: 교육 (학습자료, 강의, 시험정보)
  - `REALESTATE`: 부동산 (매매, 전세, 월세, 시세 정보)
  - `TRAVEL`: 여행 (항공, 숙박, 관광지, 맛집 정보)
  - `TECHNOLOGY`: 기술 (개발자 정보, 기술 트렌드, API 문서)
  - `LIFESTYLE`: 라이프스타일 (패션, 뷰티, 인테리어, 요리)
  - `OTHERS`: 기타 (기타 분류되지 않은 데이터)

- **키워드 (Keyword)**: API의 세부 기능/주제 (총 66개)
  - `STOCK_PRICE`: 주가 (개별 주식 가격 정보)
  - `EXCHANGE_RATE`: 환율 (통화 환율 정보)
  - `CURRENT_WEATHER`: 현재날씨 (실시간 날씨 정보)
  - `WEATHER_FORECAST`: 날씨예보 (미래 날씨 예측)
  - `SUBWAY_INFO`: 지하철정보 (지하철 노선, 시간표)
  - `BUS_INFO`: 버스정보 (버스 위치, 도착시간)
  - `STATISTICS`: 통계 (인구, 경제 통계)
  - `PUBLIC_DATA`: 공공데이터 (정부 공개 데이터)
  - 기타 58개 키워드...

### 1. 외부 데이터 API 관리 (`/external-apis`)

#### 1.1 API 등록
- **POST** `/external-apis`
- **설명**: 새로운 외부 데이터 API를 시스템에 등록
- **Rate Limit**: 1시간에 50회
- **요청 본문**:
```json
{
  "apiName": "공공데이터 API",
  "apiUrl": "https://api.example.com/data",
  "apiIssuer": "정부기관",
  "apiOwner": "담당부서",
  "apiDomain": "GOVERNMENT",
  "apiKeyword": "STATISTICS",
  "httpMethod": "GET",
  "apiDescription": "공공데이터 제공 API",
  "parameters": [
    {
      "paramName": "date",
      "paramType": "STRING",
      "isRequired": true,
      "defaultValue": null
    }
  ]
}
```

#### 1.2 API 상세 조회
- **GET** `/external-apis/{apiId}`
- **설명**: 특정 API의 상세 정보와 파라미터 조회
- **Rate Limit**: 1시간에 1000회

#### 1.3 API 목록 조회
- **GET** `/external-apis`
- **설명**: 페이징을 지원하는 API 목록 조회
- **Rate Limit**: 1시간에 1000회
- **쿼리 파라미터**:
  - `page`: 페이지 번호 (기본값: 0)
  - `size`: 페이지 크기 (기본값: 10)
  - `sort`: 정렬 필드 (기본값: createdAt)
  - `direction`: 정렬 방향 (기본값: desc)

#### 1.4 API 수정
- **PUT** `/external-apis/{apiId}`
- **설명**: 기존 API 정보 수정
- **Rate Limit**: 1시간에 50회

#### 1.5 API 삭제
- **DELETE** `/external-apis/{apiId}`
- **설명**: API 삭제 (소프트 삭제)
- **Rate Limit**: 1시간에 20회

#### 1.6 도메인별 API 조회
- **GET** `/external-apis/domain/{domain}`
- **설명**: 특정 도메인의 API 목록 조회
- **Rate Limit**: 1시간에 1000회
- **도메인 값**: FINANCE, WEATHER, NEWS, TRANSPORTATION, COMMERCE, GOVERNMENT, ENTERTAINMENT, SPORTS, HEALTHCARE, EDUCATION, REALESTATE, TRAVEL, TECHNOLOGY, LIFESTYLE, OTHERS
- **사용 예시**: 금융 API, 날씨 API, 뉴스 API 등 업무 영역별 탐색

#### 1.7 키워드별 API 조회
- **GET** `/external-apis/keyword/{keyword}`
- **설명**: 특정 키워드의 API 목록 조회
- **Rate Limit**: 1시간에 1000회
- **키워드 값**: STOCK_PRICE, EXCHANGE_RATE, CURRENT_WEATHER, WEATHER_FORECAST, SUBWAY_INFO, BUS_INFO, STATISTICS, PUBLIC_DATA 등 (총 66개)
- **사용 예시**: 주가 API, 환율 API, 날씨예보 API 등 세부 기능별 탐색

#### 1.8 API 통계 조회
- **GET** `/external-apis/statistics`
- **설명**: API 사용 통계 및 분석 데이터
- **Rate Limit**: 1시간에 500회

#### 1.9 API 유효성 검증
- **POST** `/external-apis/{apiId}/validate`
- **설명**: API 엔드포인트 유효성 검증
- **Rate Limit**: 1시간에 200회

#### 1.10 API 복사
- **POST** `/external-apis/{apiId}/copy?newName={새이름}`
- **설명**: 기존 API를 복사하여 새로운 API 생성
- **Rate Limit**: 1시간에 50회

### 2. API 파라미터 관리 (`/external-apis/{apiId}/parameters`)

#### 2.1 파라미터 등록
- **POST** `/external-apis/{apiId}/parameters`
- **설명**: 특정 API에 새로운 파라미터 등록

#### 2.2 파라미터 조회
- **GET** `/external-apis/{apiId}/parameters/{parameterId}`
- **설명**: 특정 파라미터 정보 조회

#### 2.3 파라미터 목록 조회
- **GET** `/external-apis/{apiId}/parameters`
- **설명**: API의 모든 파라미터 조회

#### 2.4 필수 파라미터 조회
- **GET** `/external-apis/{apiId}/parameters/required`
- **설명**: API의 필수 파라미터만 조회

#### 2.5 파라미터 수정
- **PUT** `/external-apis/{apiId}/parameters/{parameterId}`
- **설명**: 파라미터 정보 수정

#### 2.6 파라미터 삭제
- **DELETE** `/external-apis/{apiId}/parameters/{parameterId}`
- **설명**: 파라미터 삭제

### 3. SGIS API 키 관리 (`/sgis/keys`)

#### 3.1 API 키 등록
- **POST** `/sgis/keys`
- **설명**: SGIS 공공데이터포털 API 키 등록
- **요청 본문**:
```json
{
  "organizationName": "기관명",
  "organizationCode": "기관코드",
  "contactEmail": "contact@example.com",
  "contactPhone": "02-1234-5678",
  "apiKey": "실제_API_키",
  "secretKey": "실제_시크릿_키",
  "dailyLimit": 10000,
  "monthlyLimit": 100000,
  "expiresAt": "2024-12-31T23:59:59",
  "description": "API 키 설명",
  "requestedApis": ["API1", "API2"]
}
```

#### 3.2 API 키 조회
- **GET** `/sgis/keys/{keyId}`
- **설명**: 특정 API 키 정보 조회

#### 3.3 API 키 목록 조회
- **GET** `/sgis/keys`
- **설명**: 페이징을 지원하는 API 키 목록 조회

#### 3.4 API 키 수정
- **PUT** `/sgis/keys/{keyId}`
- **설명**: API 키 정보 수정

#### 3.5 API 키 상태 변경
- **PATCH** `/sgis/keys/{keyId}/status`
- **설명**: API 키 활성/비활성 상태 변경

#### 3.6 API 키 폐기
- **DELETE** `/sgis/keys/{keyId}`
- **설명**: API 키 폐기

#### 3.7 API 키 통계
- **GET** `/sgis/keys/statistics`
- **설명**: API 키 사용 통계

### 4. AI 분류 관리 (`/api-management/ai/classifications`)

#### 4.1 API 자동 분류
- **POST** `/api-management/ai/classifications/classify`
- **설명**: AI를 통한 API 자동 분류 실행
- **요청 본문**:
```json
{
  "apiId": "api-123",
  "apiName": "API 이름",
  "apiDescription": "API 설명",
  "apiUrl": "https://api.example.com"
}
```

#### 4.2 분류 결과 조회
- **GET** `/api-management/ai/classifications/{classificationId}`
- **설명**: 특정 분류 결과 조회

#### 4.3 API별 분류 결과 조회
- **GET** `/api-management/ai/classifications/api/{apiId}`
- **설명**: 특정 API의 분류 결과 조회

#### 4.4 분류 결과 목록 조회
- **GET** `/api-management/ai/classifications`
- **설명**: 모든 분류 결과 조회

### 5. 헬스 체크 (`/health`)

#### 5.1 기본 헬스 체크
- **GET** `/health`
- **설명**: 서비스 기본 상태 확인

#### 5.2 상세 헬스 체크
- **GET** `/health/detailed`
- **설명**: 서비스 상세 상태 및 의존성 확인

### 6. Rate Limiting 테스트 (`/api/v1/rate-limit-test`)

#### 6.1 IP 기반 Rate Limiting 테스트
- **GET** `/api/v1/rate-limit-test/ip-based`
- **설명**: IP 기반 Rate Limiting 테스트 (1분에 5회)

#### 6.2 API 키 기반 Rate Limiting 테스트
- **GET** `/api/v1/rate-limit-test/api-key-based`
- **설명**: API 키 기반 Rate Limiting 테스트 (1시간에 100회)

#### 6.3 사용자 기반 Rate Limiting 테스트
- **GET** `/api/v1/rate-limit-test/user-based`
- **설명**: 사용자 기반 Rate Limiting 테스트 (1일에 1000회)

#### 6.4 세션 기반 Rate Limiting 테스트
- **GET** `/api/v1/rate-limit-test/session-based`
- **설명**: 세션 기반 Rate Limiting 테스트 (1분에 3회)

#### 6.5 Rate Limiting 상태 조회
- **GET** `/api/v1/rate-limit-test/status?key={키}`
- **설명**: Rate Limiting 상태 정보 조회

#### 6.6 Rate Limiting 캐시 정리
- **POST** `/api/v1/rate-limit-test/cleanup`
- **설명**: Rate Limiting 캐시 수동 정리

### 7. Redis 캐시 테스트 (`/api/v1/redis-cache-test`)

#### 7.1 API 상태 정보 캐싱
- **POST** `/api/v1/redis-cache-test/cache-api-status/{apiId}`
- **설명**: API 상태 정보를 Redis에 캐싱

#### 7.2 캐시된 API 상태 조회
- **GET** `/api/v1/redis-cache-test/get-cached-api-status/{apiId}`
- **설명**: 캐시된 API 상태 정보 조회

#### 7.3 API 사용량 통계 캐싱
- **POST** `/api/v1/redis-cache-test/cache-usage-statistics/{apiId}`
- **설명**: API 사용량 통계를 Redis에 캐싱

#### 7.4 캐시된 사용량 통계 조회
- **GET** `/api/v1/redis-cache-test/get-cached-usage-statistics/{apiId}`
- **설명**: 캐시된 API 사용량 통계 조회

#### 7.5 캐시 무효화
- **POST** `/api/v1/redis-cache-test/invalidate-cache/{apiId}`
- **설명**: 특정 API의 캐시 무효화

#### 7.6 캐시 통계 조회
- **GET** `/api/v1/redis-cache-test/cache-statistics`
- **설명**: Redis 캐시 통계 정보 조회

#### 7.7 만료된 캐시 정리
- **POST** `/api/v1/redis-cache-test/cleanup-expired-cache`
- **설명**: 만료된 캐시 자동 정리

#### 7.8 캐시 TTL 연장
- **POST** `/api/v1/redis-cache-test/extend-cache-ttl/{apiId}?additionalSeconds={초}`
- **설명**: 캐시 TTL 연장

## 📊 데이터 모델

### API 도메인 (ApiDomain)
- `GOVERNMENT`: 정부/공공기관
- `BUSINESS`: 기업/비즈니스
- `EDUCATION`: 교육/연구
- `HEALTHCARE`: 의료/건강
- `TRANSPORTATION`: 교통/운송

### API 키워드 (ApiKeyword)
- `STATISTICS`: 통계
- `GEOGRAPHY`: 지리
- `WEATHER`: 날씨
- `TRAFFIC`: 교통
- `POPULATION`: 인구

### API 키 상태 (ApiKeyStatus)
- `ACTIVE`: 활성
- `INACTIVE`: 비활성
- `EXPIRED`: 만료
- `REVOKED`: 폐기

### Rate Limiting 키 타입 (KeyType)
- `IP_ADDRESS`: IP 주소 기반
- `API_KEY`: API 키 기반
- `USER_ID`: 사용자 ID 기반
- `SESSION`: 세션 기반

## 🚨 에러 코드

### HTTP 상태 코드
- `200 OK`: 요청 성공
- `201 Created`: 리소스 생성 성공
- `400 Bad Request`: 잘못된 요청
- `401 Unauthorized`: 인증 필요
- `403 Forbidden`: 접근 권한 없음
- `404 Not Found`: 리소스 없음
- `429 Too Many Requests`: Rate Limit 초과
- `500 Internal Server Error`: 서버 내부 오류

### 비즈니스 에러 코드
- `API_NOT_FOUND`: API를 찾을 수 없음
- `INVALID_API_KEY`: 유효하지 않은 API 키
- `RATE_LIMIT_EXCEEDED`: 요청 제한 초과
- `VALIDATION_ERROR`: 입력값 검증 실패
- `DUPLICATE_API`: 중복된 API


```


