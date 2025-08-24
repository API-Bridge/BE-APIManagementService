# API Management Service API 명세서

## 개요
API Management Service는 외부 API의 등록, 관리, 모니터링, 인증 등을 제공하는 서비스입니다.

**Base URL**: `/api/v1`  
**Context Path**: `/api/v1`

---

## 1. API 키 관리 (ApiKey)

### 1.1 API 키 등록
**POST** `/api-keys/register`

API 키를 등록합니다.

**Request Body:**
```json
{
  "organizationName": "조직명",
  "organizationCode": "ORG001",
  "contactEmail": "contact@example.com",
  "contactPhone": "010-1234-5678",
  "apiServiceName": "SGIS",
  "apiServiceUrl": "https://api.example.com",
  "apiKey": "실제_API_키_값",
  "secretKey": "비밀키_값",
  "dailyLimit": 1000,
  "monthlyLimit": 30000,
  "expiresAt": "2025-12-31T23:59:59",
  "description": "API 키 설명",
  "requestedApis": "요청된,API,목록"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "keyId": "생성된_키_ID",
    "organizationName": "조직명",
    "apiServiceName": "SGIS",
    "status": "ACTIVE",
    "createdAt": "2025-01-01T00:00:00"
  },
  "message": "API 키가 성공적으로 등록되었습니다."
}
```

### 1.2 API 키 조회
**GET** `/api-keys/{keyId}`

특정 API 키 정보를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "keyId": "키_ID",
    "organizationName": "조직명",
    "apiServiceName": "SGIS",
    "status": "ACTIVE",
    "dailyLimit": 1000,
    "currentDailyUsage": 150,
    "monthlyLimit": 30000,
    "currentMonthlyUsage": 2500
  }
}
```

### 1.3 조직별 API 키 조회
**GET** `/api-keys/organization/{organizationName}`

특정 조직의 모든 API 키를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "keyId": "키_ID_1",
      "apiServiceName": "SGIS",
      "status": "ACTIVE"
    },
    {
      "keyId": "키_ID_2",
      "apiServiceName": "KAKAO",
      "status": "ACTIVE"
    }
  ]
}
```

### 1.4 서비스별 API 키 조회
**GET** `/api-keys/service/{serviceName}`

특정 서비스의 모든 API 키를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "keyId": "키_ID_1",
      "organizationName": "조직명1",
      "status": "ACTIVE"
    }
  ]
}
```

### 1.5 API 키 목록 조회 (페이지네이션)
**GET** `/api-keys?page=0&size=10`

API 키 목록을 페이지네이션으로 조회합니다.

**Query Parameters:**
- `page`: 페이지 번호 (0부터 시작)
- `size`: 페이지 크기

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "keyId": "키_ID",
        "organizationName": "조직명",
        "apiServiceName": "SGIS",
        "status": "ACTIVE"
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

### 1.6 API 키 수정
**PUT** `/api-keys/{keyId}`

API 키 정보를 수정합니다.

**Request Body:**
```json
{
  "organizationName": "수정된_조직명",
  "dailyLimit": 2000,
  "monthlyLimit": 50000,
  "description": "수정된_설명"
}
```

### 1.7 API 키 상태 변경
**PATCH** `/api-keys/{keyId}/status`

API 키의 상태를 변경합니다.

**Request Body:**
```json
{
  "status": "SUSPENDED"
}
```

**가능한 상태값:**
- `ACTIVE`: 활성
- `INACTIVE`: 비활성
- `SUSPENDED`: 일시정지
- `REVOKED`: 폐기
- `EXPIRED`: 만료
- `PENDING`: 대기

### 1.8 API 키 삭제
**DELETE** `/api-keys/{keyId}`

API 키를 소프트 삭제합니다.

**Response:**
```json
{
  "success": true,
  "message": "API 키가 성공적으로 삭제되었습니다."
}
```

### 1.9 활성 API 키 조회
**GET** `/api-keys/active`

활성 상태인 API 키만 조회합니다.

---

## 2. External API 관리

### 2.1 API 등록
**POST** `/external-apis/register`

API 정보, 파라미터, 토큰을 한 번에 등록합니다.

**Request Body:**
```json
{
  "apiName": "실시간 날씨 API",
  "apiUrl": "https://api.weather.com/v1/current",
  "apiIssuer": "날씨 정보 제공기관",
  "apiOwner": "날씨팀",
  "apiDomain": "WEATHER",
  "apiKeyword": "CURRENT_WEATHER",
  "httpMethod": "GET",
  "apiDescription": "실시간 날씨 정보를 제공하는 API입니다.",
  "apiToken": "weather_token_12345",
  "autoTokenRefresh": true,
  "parameters": [
    {
      "paramName": "city",
      "paramType": "STRING",
      "required": true,
      "defaultValue": "Seoul",
      "description": "도시명"
    },
    {
      "paramName": "units",
      "paramType": "STRING",
      "required": false,
      "defaultValue": "metric",
      "description": "온도 단위 (metric: 섭씨, imperial: 화씨)"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "apiId": "api_7497dd988afa4658",
    "apiName": "실시간 날씨 API",
    "apiUrl": "https://api.weather.com/v1/current",
    "apiIssuer": "날씨 정보 제공기관",
    "apiOwner": "날씨팀",
    "apiDomain": "WEATHER",
    "apiKeyword": "CURRENT_WEATHER",
    "httpMethod": "GET",
    "apiDescription": "실시간 날씨 정보를 제공하는 API입니다.",
    "apiEffectiveness": true,
    "apiToken": "weather_token_12345",
    "autoTokenRefresh": true,
    "tokenExpiresAt": "2024-01-15T18:00:00",
    "createdAt": "2024-01-15T14:00:00",
    "updatedAt": "2024-01-15T14:00:00"
  },
  "message": "API가 성공적으로 등록되었습니다."
}
```

### 2.2 API 상세 조회

#### 2.2.1 ID 기반 상세 조회
**GET** `/external-apis/detail/{apiId}`

특정 API의 상세 정보와 파라미터를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "api": {
      "apiId": "api_7497dd988afa4658",
      "apiName": "실시간 날씨 API",
      "apiUrl": "https://api.weather.com/v1/current",
      "apiIssuer": "날씨 정보 제공기관",
      "apiOwner": "날씨팀",
      "apiDomain": "WEATHER",
      "apiKeyword": "CURRENT_WEATHER",
      "apiKeyEntity": null,
      "apiToken": "weather_token_12345",
      "autoTokenRefresh": true,
      "tokenExpiresAt": "2024-01-15T18:00:00",
      "httpMethod": "GET",
      "apiDescription": "실시간 날씨 정보를 제공하는 API입니다.",
      "apiEffectiveness": true,
      "createdAt": "2024-01-15T14:00:00",
      "updatedAt": "2024-01-15T14:00:00"
    },
    "parameters": [
      {
        "parameterId": "param_001",
        "apiId": "api_7497dd988afa4658",
        "paramName": "city",
        "paramType": "STRING",
        "isRequired": true,
        "defaultValue": "Seoul",
        "description": "도시명",
        "createdAt": "2024-01-15T14:00:00",
        "updatedAt": "2024-01-15T14:00:00"
      },
      {
        "parameterId": "param_002",
        "apiId": "api_7497dd988afa4658",
        "paramName": "units",
        "paramType": "STRING",
        "isRequired": false,
        "defaultValue": "metric",
        "description": "온도 단위 (metric: 섭씨, imperial: 화씨)",
        "createdAt": "2024-01-15T14:00:00",
        "updatedAt": "2024-01-15T14:00:00"
      }
    ]
  },
  "message": "API 상세정보를 성공적으로 조회했습니다."
}
```

#### 2.2.2 이름 기반 상세 조회
**GET** `/external-apis/detail/name/{apiName}`

API 이름으로 상세 정보와 파라미터를 조회합니다.

**Response:** (위와 동일한 구조)

### 2.3 API 목록 조회
**GET** `/external-apis/list`

페이징을 지원하는 API 목록을 조회합니다.

**Query Parameters:**
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 10): 페이지 크기
- `sort` (기본값: createdAt): 정렬 기준
- `direction` (기본값: desc): 정렬 방향

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "apiId": "api_7497dd988afa4658",
        "apiName": "실시간 날씨 API",
        "apiUrl": "https://api.weather.com/v1/current",
        "apiIssuer": "날씨 정보 제공기관",
        "apiOwner": "날씨팀",
        "apiDomain": "WEATHER",
        "apiKeyword": "CURRENT_WEATHER",
        "httpMethod": "GET",
        "apiDescription": "실시간 날씨 정보를 제공하는 API입니다.",
        "apiEffectiveness": true,
        "createdAt": "2024-01-15T14:00:00",
        "updatedAt": "2024-01-15T14:00:00"
      }
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  },
  "message": "API 목록을 성공적으로 조회했습니다."
}
```

### 2.4 API 검색 (통합 검색)
**GET** `/external-apis/search`

도메인, 키워드, 검색어를 조합하여 API를 검색합니다.

**Query Parameters:**
- `domain` (선택): API 도메인 (GOVERNMENT, WEATHER, FINANCE, TECHNOLOGY 등)
- `keyword` (선택): API 키워드 (STATISTICS, CURRENT_WEATHER, STOCK_PRICE 등)
- `searchTerm` (선택): API 이름, 설명, 발급처에서 검색할 텍스트

**사용 예시:**
- `GET /external-apis/search?domain=WEATHER` - 날씨 도메인의 모든 API
- `GET /external-apis/search?keyword=CURRENT_WEATHER` - 현재 날씨 키워드의 모든 API
- `GET /external-apis/search?searchTerm=날씨` - "날씨"가 포함된 모든 API
- `GET /external-apis/search?domain=WEATHER&keyword=CURRENT_WEATHER` - 날씨 도메인 + 현재 날씨 키워드
- `GET /external-apis/search?domain=WEATHER&searchTerm=실시간` - 날씨 도메인에서 "실시간"이 포함된 API

**반환되는 정보:**
- **API 기본 정보**: apiId, apiName, apiUrl, apiIssuer, apiOwner, apiDomain, apiKeyword, httpMethod, apiDescription
- **API 상태 정보**: apiEffectiveness, apiToken, autoTokenRefresh, tokenExpiresAt
- **시간 정보**: createdAt, updatedAt
- **파라미터 정보**: parameterId, paramName, paramType, isRequired, defaultValue, description, createdAt, updatedAt

**검색 동작 방식:**
1. **도메인 + 키워드 조합**: 정확한 도메인과 키워드가 일치하는 API만 반환
2. **도메인만**: 해당 도메인의 모든 API 반환
3. **키워드만**: 해당 키워드의 모든 API 반환
4. **검색어만**: API 이름, 설명, 발급처에서 검색어가 포함된 모든 API 반환
5. **복합 검색**: 위 조건들을 조합하여 필터링된 결과 반환

### 2.5 API 통계 조회
**GET** `/external-apis/statistics`

API 통계 정보를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalApis": 50,
    "activeApis": 45,
    "domainStats": {
      "GOVERNMENT": 20,
      "WEATHER": 15,
      "FINANCE": 10,
      "TECHNOLOGY": 5
    },
    "keywordStats": {
      "STATISTICS": 25,
      "CURRENT_WEATHER": 15,
      "STOCK_PRICE": 10
    }
  }
}
```

### 2.6 활성 API 목록 조회
**GET** `/external-apis/active`

활성 상태인 API들만 조회합니다.

### 2.7 API 수정
**PUT** `/external-apis/update/{apiId}`

API 정보를 수정합니다.

**Request Body:**
```json
{
  "apiName": "수정된_API_이름",
  "apiDescription": "수정된_설명",
  "apiEffectiveness": true,
  "autoTokenRefresh": false
}
```

### 2.8 API 삭제
**DELETE** `/external-apis/delete/{apiId}`

API를 소프트 삭제합니다.

**Response:**
```json
{
  "success": true,
  "message": "API가 성공적으로 삭제되었습니다."
}
```

### 2.9 API 하드 삭제
**DELETE** `/external-apis/delete/{apiId}/hard`

API를 완전히 삭제합니다. (복구 불가)

### 2.10 API 효과성 업데이트
**PATCH** `/external-apis/update/{apiId}/effectiveness`

API의 효과성을 업데이트합니다.

**Request Body:**
```json
{
  "apiEffectiveness": false
}
```

### 2.11 API 키 연결
**POST** `/external-apis/link/{apiId}/api-key`

API에 API 키를 연결합니다.

**Request Body:**
```json
{
  "apiKeyId": "연결할_API_키_ID"
}
```

### 2.12 API 키 연결 해제
**DELETE** `/external-apis/unlink/{apiId}/api-key`

API에서 API 키 연결을 해제합니다.

### 2.13 API 복사
**POST** `/external-apis/copy/{apiId}`

기존 API를 복사하여 새로운 API를 생성합니다.

### 2.14 API 유효성 검증
**POST** `/external-apis/validate/{apiId}`

API의 유효성을 검증합니다.

---

## 3. API 파라미터 관리 (ApiParameter)

### 3.1 파라미터 조회

#### 3.1.1 특정 파라미터 조회
**GET** `/external-apis/{apiId}/parameters/{parameterId}`

특정 파라미터의 상세 정보를 조회합니다.

#### 3.1.2 API의 모든 파라미터 조회
**GET** `/external-apis/{apiId}/parameters`

특정 API의 모든 파라미터를 조회합니다.

#### 3.1.3 필수 파라미터만 조회
**GET** `/external-apis/{apiId}/parameters/required`

특정 API의 필수 파라미터만 조회합니다.

### 3.2 파라미터 수정 또는 추가
**PUT** `/external-apis/{apiId}/parameters/{parameterId}`

**기존 파라미터가 있으면 수정, 없으면 새로 추가합니다.**

**Request Body:**
```json
{
  "paramName": "city",
  "paramType": "STRING",
  "isRequired": true,
  "defaultValue": "Seoul",
  "description": "도시명"
}
```

**동작 방식:**
- **기존 파라미터 존재**: 파라미터 정보 수정
- **기존 파라미터 없음**: 새로운 파라미터 생성

**참고**: 파라미터 등록은 API 등록 시 함께 처리되며, 이 엔드포인트로 추가 파라미터를 생성하거나 기존 파라미터를 수정할 수 있습니다.

### 3.3 파라미터 삭제

#### 3.3.1 파라미터 소프트 삭제
**DELETE** `/external-apis/{apiId}/parameters/{parameterId}`

파라미터를 소프트 삭제합니다.

#### 3.3.2 파라미터 하드 삭제
**DELETE** `/external-apis/{apiId}/parameters/{parameterId}/hard`

파라미터를 완전히 삭제합니다. (복구 불가)

---

## 4. API 헬스체크

**자동 스케줄링 헬스체크:**
- **5분마다**: 활성 API들의 헬스체크 수행
- **30분마다**: 중요 API들(정부/금융/교통 도메인)의 상세 헬스체크 수행  
- **매일 새벽 2시**: 전체 API 헬스체크 수행
- **사용 불가능한 API 목록**: Redis 캐시에 자동 저장 및 2시간 TTL 관리

### 4.1 API 헬스체크 실행
**POST** `/api-health/check/{apiId}`

특정 API의 헬스체크를 실행합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "apiId": "API_ID",
    "status": "HEALTHY",
    "lastCheckedAt": "2025-01-01T00:00:00",
    "responseTime": 150,
    "message": "API가 정상적으로 응답합니다."
  }
}
```

### 4.2 API 헬스체크 상태 조회
**GET** `/api-health/status/{apiId}`

특정 API의 헬스체크 상태를 조회합니다.

### 4.3 API 헬스체크 강제 실행
**POST** `/api-health/check/{apiId}/force`

캐시를 무시하고 API 헬스체크를 강제로 실행합니다.

### 4.4 배치 헬스체크
**POST** `/api-health/check/batch`

여러 API의 헬스체크를 일괄 실행합니다.

**Request Body:**
```json
{
  "apiIds": ["API_ID_1", "API_ID_2", "API_ID_3"]
}
```

### 4.5 전체 API 헬스체크 결과 조회 (페이징)
**GET** `/api-health/health/status/all?page={page}&size={size}`

전체 API의 헬스체크 결과를 페이징하여 조회합니다.

**Query Parameters:**
- `page` (기본값: 0): 페이지 번호
- `size` (기본값: 20): 페이지 크기

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "apiId": "API_ID_1",
        "status": "HEALTHY",
        "responseTime": 150,
        "checkedAt": "2025-01-01T00:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 4.6 사용 불가능한 API 목록 조회
**GET** `/api-health/health/unavailable`

사용 불가능한 API 목록을 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": ["API_ID_1", "API_ID_2", "API_ID_3"]
}
```

### 4.7 사용 불가능한 API 개수 조회
**GET** `/api-health/health/unavailable/count`

사용 불가능한 API 개수를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": 3
}
```

### 4.8 특정 API 사용 불가능 여부 확인
**GET** `/api-health/health/unavailable/{apiId}`

특정 API가 사용 불가능한지 확인합니다.

**Response:**
```json
{
  "success": true,
  "data": true
}
```

### 4.9 사용 불가능한 API 목록 캐시 강제 갱신
**POST** `/api-health/health/unavailable/refresh`

사용 불가능한 API 목록 캐시를 강제로 갱신합니다.

**Response:**
```json
{
  "success": true,
  "data": "사용 불가능한 API 목록 캐시가 성공적으로 갱신되었습니다."
}
```

---

## 5. 토큰 관리

### 5.1 토큰 갱신 실행
**POST** `/tokens/refresh/{apiId}`

특정 API의 토큰을 수동으로 갱신합니다.

### 5.2 토큰 갱신 상태 조회
**GET** `/tokens/refresh/status`

토큰 갱신 작업의 상태를 조회합니다.

### 5.3 SGIS API 정보 조회
**GET** `/tokens/sgis-info`

SGIS API 관련 정보를 조회합니다.

---

## 6. AI 분류

### 6.1 API 자동 분류
**POST** `/ai-classification/classify`

API를 AI를 사용하여 자동으로 분류합니다.

**Request Body:**
```json
{
  "apiId": "API_ID",
  "apiName": "API_이름",
  "apiDescription": "API_설명",
  "apiUrl": "API_URL",
  "classificationPrompt": "분류_프롬프트"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "classifiedDomain": "FINANCE",
    "classifiedKeyword": "STOCK_PRICE",
    "confidence": 0.95,
    "classificationPrompt": "사용된_프롬프트"
  }
}
```

### 6.2 분류 통계 조회
**GET** `/ai-classification/stats/domain`

도메인별 분류 통계를 조회합니다.

**GET** `/ai-classification/stats/keyword`

키워드별 분류 통계를 조회합니다.

### 6.3 분류 결과 검색
**GET** `/ai-classification/search?query=주식`

분류 결과를 검색합니다.

---

## 7. Rate Limiting

### 7.1 Rate Limit 상태 조회
**GET** `/rate-limit/status/{key}`

특정 키의 Rate Limit 상태를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "currentCount": 5,
    "limit": 10,
    "remaining": 5,
    "allowed": true
  }
}
```

---

## 8. 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... },
  "message": "작업이 성공적으로 완료되었습니다."
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지",
    "details": "상세 에러 정보"
  }
}
```

### 페이지네이션 응답
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 100,
    "totalPages": 10,
    "currentPage": 0,
    "pageSize": 10,
    "first": true,
    "last": false
  }
}
```

---

## 9. 에러 코드

| 코드 | 설명 |
|------|------|
| `API_NOT_FOUND` | API를 찾을 수 없음 |
| `API_KEY_NOT_FOUND` | API 키를 찾을 수 없음 |
| `INVALID_REQUEST` | 잘못된 요청 |
| `UNAUTHORIZED` | 인증 실패 |
| `FORBIDDEN` | 권한 없음 |
| `RATE_LIMIT_EXCEEDED` | Rate Limit 초과 |
| `INTERNAL_SERVER_ERROR` | 내부 서버 오류 |

---

## 10. 데이터 타입

### ApiDomain
- `COMMERCE`: 상거래
- `EDUCATION`: 교육
- `ENTERTAINMENT`: 엔터테인먼트
- `FINANCE`: 금융
- `GOVERNMENT`: 정부
- `HEALTHCARE`: 의료
- `LIFESTYLE`: 라이프스타일
- `NEWS`: 뉴스
- `OTHERS`: 기타
- `REALESTATE`: 부동산
- `SPORTS`: 스포츠
- `TECHNOLOGY`: 기술
- `TRANSPORTATION`: 교통
- `TRAVEL`: 여행
- `WEATHER`: 날씨

### ApiKeyword
- `AIR_QUALITY`: 대기질
- `API_DOCUMENT`: API 문서
- `BASEBALL_RESULT`: 야구 결과
- `BASKETBALL_RESULT`: 농구 결과
- `BEAUTY_TIP`: 뷰티 팁
- `BREAKING_NEWS`: 속보
- `BUS_INFO`: 버스 정보
- `CELEBRITY_NEWS`: 연예인 뉴스
- `COUPON_DISCOUNT`: 쿠폰 할인
- `COURSE_INFO`: 과정 정보
- `CRYPTOCURRENCY`: 암호화폐
- `CURRENT_WEATHER`: 현재 날씨
- `DEVELOPER_TOOL`: 개발자 도구
- `ECONOMIC_INDICATOR`: 경제 지표
- `ECONOMY_NEWS`: 경제 뉴스
- `EXAM_SCHEDULE`: 시험 일정
- `EXCHANGE_RATE`: 환율
- `FASHION_TREND`: 패션 트렌드
- `FITNESS_DATA`: 피트니스 데이터
- `FLIGHT_INFO`: 항공 정보
- `GAME_INFO`: 게임 정보
- `HEALTH_TIP`: 건강 팁
- `HOSPITAL_INFO`: 병원 정보
- `HOTEL_INFO`: 호텔 정보
- `HOUSE_PRICE`: 집값
- `INTEREST_RATE`: 이자율
- `INTERIOR_TIP`: 인테리어 팁
- `LEGAL_INFO`: 법률 정보
- `MEDICINE_INFO`: 의약품 정보
- `MOVIE_INFO`: 영화 정보
- `MUSIC_CHART`: 음악 차트
- `PARKING_INFO`: 주차 정보
- `PLAYER_STATS`: 선수 통계
- `POLICY_INFO`: 정책 정보
- `POLITICS`: 정치
- `PRECIPITATION`: 강수량
- `PRICE_COMPARISON`: 가격 비교
- `PRODUCT_INFO`: 상품 정보
- `PRODUCT_REVIEW`: 상품 리뷰
- `PUBLIC_DATA`: 공공 데이터
- `PUBLIC_SERVICE`: 공공 서비스
- `REAL_ESTATE_TREND`: 부동산 트렌드
- `RECIPE`: 레시피
- `RENT_INFO`: 임대 정보
- `RESTAURANT_INFO`: 식당 정보
- `SCHOLARSHIP`: 장학금
- `SCHOOL_INFO`: 학교 정보
- `SHOPPING_RANK`: 쇼핑 순위
- `SOCCER_RESULT`: 축구 결과
- `SOCIAL_TREND`: 소셜 트렌드
- `SPORTS_NEWS`: 스포츠 뉴스
- `SPORTS_SCHEDULE`: 스포츠 일정
- `STATISTICS`: 통계
- `STOCK_INDEX`: 주가 지수
- `STOCK_PRICE`: 주가
- `SUBWAY_INFO`: 지하철 정보
- `TAXI_FARE`: 택시 요금
- `TEAM_RANKING`: 팀 순위
- `TECHNOLOGY_NEWS`: 기술 뉴스
- `TECH_TREND`: 기술 트렌드
- `TEMPERATURE`: 온도
- `TOURIST_SPOT`: 관광지
- `TRAFFIC_CONDITION`: 교통 상황
- `TV_SCHEDULE`: TV 일정
- `UV_INDEX`: 자외선 지수
- `WEATHER_FORECAST`: 날씨 예보

### ApiKeyStatus
- `ACTIVE`: 활성
- `INACTIVE`: 비활성
- `PENDING`: 대기
- `EXPIRED`: 만료
- `REVOKED`: 폐기
- `SUSPENDED`: 일시정지

---

## 11. 인증 및 보안

### Rate Limiting
- IP 기반: `@RateLimit(keyType = "IP", limit = 100, timeUnit = TimeUnit.MINUTES)`
- 사용자 기반: `@RateLimit(keyType = "USER", limit = 50, timeUnit = TimeUnit.MINUTES)`
- API 기반: `@RateLimit(keyType = "API", limit = 1000, timeUnit = TimeUnit.DAYS)`

### 로깅
- 모든 API 호출은 `LoggingAspect`를 통해 로깅됩니다
- 실행 시간, 요청/응답 정보가 기록됩니다

---

## 12. 모니터링 및 헬스체크

### Actuator 엔드포인트
- `/actuator/health`: 애플리케이션 상태
- `/actuator/info`: 애플리케이션 정보
- `/actuator/metrics`: 메트릭 정보

### 스케줄링
- API 토큰 자동 갱신: 4시간마다 실행
- API 헬스체크: 설정 가능한 주기로 실행
- Rate Limit 캐시 정리: 주기적으로 실행

---

## 13. 개발 및 테스트

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests RateLimitServiceTest

# 특정 테스트 메서드 실행
./gradlew test --tests RateLimitServiceTest.isAllowed_NewKey_ShouldAllow
```

### 테스트 프로파일
- `test`: H2 인메모리 데이터베이스 사용
- `dev`: 로컬 MySQL 데이터베이스 사용
- `prod`: 프로덕션 환경 설정

---

## 14. 배포 및 운영

### Docker
```bash
# 이미지 빌드
docker build -t api-management-service .

# 컨테이너 실행
docker run -p 8080:8080 api-management-service
```

### Kubernetes
```bash
# Helm 차트 설치
helm install api-management-service ./chart

# Helm 차트 업그레이드
helm upgrade api-management-service ./chart

# Helm 차트 제거
helm uninstall api-management-service
```

---

## 15. 변경 이력

### v1.0.0 (2025-01-01)
- 초기 API 명세서 작성
- API 키 관리 기능 추가
- API 헬스체크 기능 추가
- 토큰 자동 갱신 기능 추가
- AI 분류 기능 추가
- Rate Limiting 기능 추가

---

## 16. 연락처 및 지원

**개발팀**: API Management Service Team  
**이메일**: dev@example.com  
**문서 버전**: 1.0.0  
**최종 업데이트**: 2025-01-01

