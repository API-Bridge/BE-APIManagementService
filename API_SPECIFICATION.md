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

### 1.4 API 키 목록 조회 (페이지네이션)
**GET** `/api-keys`

API 키 목록을 페이지네이션으로 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호 (기본값: 0)
- `size` (optional): 페이지 크기 (기본값: 20)
- `status` (optional): 상태 필터 (ACTIVE, INACTIVE, EXPIRED)
- `organizationName` (optional): 조직명 필터

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
        "status": "ACTIVE",
        "createdAt": "2025-01-01T00:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 1.5 API 키 수정
**PUT** `/api-keys/{keyId}`

API 키 정보를 수정합니다.

**Request Body:**
```json
{
  "organizationName": "수정된_조직명",
  "contactEmail": "new@example.com",
  "dailyLimit": 2000,
  "monthlyLimit": 60000,
  "description": "수정된_설명"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "keyId": "키_ID",
    "organizationName": "수정된_조직명",
    "status": "ACTIVE",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "message": "API 키가 성공적으로 수정되었습니다."
}
```

### 1.6 API 키 상태 변경
**PATCH** `/api-keys/{keyId}/status`

API 키의 상태를 변경합니다.

**Request Body:**
```json
{
  "status": "INACTIVE"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "keyId": "키_ID",
    "status": "INACTIVE",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "message": "API 키 상태가 성공적으로 변경되었습니다."
}
```

### 1.7 API 키 삭제
**DELETE** `/api-keys/{keyId}`

API 키를 삭제합니다 (소프트 삭제).

**Response:**
```json
{
  "success": true,
  "message": "API 키가 성공적으로 삭제되었습니다."
}
```

### 1.8 활성 API 키 조회
**GET** `/api-keys/active`

활성 상태인 API 키 목록을 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "keyId": "키_ID",
      "organizationName": "조직명",
      "apiServiceName": "SGIS",
      "status": "ACTIVE"
    }
  ]
}
```

---

## 2. External API 관리 (파라미터 관리 기능 포함)

### 2.1 API 등록
**POST** `/external-apis/register`

새로운 외부 API를 등록합니다. 등록 시 자동으로 AI 분류가 수행됩니다.

**Request Body:**
```json
{
  "apiName": "API_이름",
  "apiUrl": "https://api.example.com/endpoint",
  "apiIssuer": "API_제공자",
  "apiOwner": "API_소유자",
  "httpMethod": "GET",
  "apiDescription": "API_설명",
  "autoTokenRefresh": true,
  "parameters": [
    {
      "paramName": "파라미터명",
      "paramType": "STRING",
      "paramDescription": "파라미터_설명",
      "required": true,
      "defaultValue": "기본값"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "apiId": "생성된_API_ID",
    "apiName": "API_이름",
    "apiUrl": "https://api.example.com/endpoint",
    "status": "REGISTERED",
    "createdAt": "2025-01-01T00:00:00"
  },
  "message": "API가 성공적으로 등록되었습니다."
}
```

### 2.2 API 상세 조회

#### 2.2.1 ID 기반 상세 조회
**GET** `/external-apis/detail/{apiId}`

API ID를 기반으로 상세 정보를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "apiId": "API_ID",
    "apiName": "API_이름",
    "apiUrl": "https://api.example.com/endpoint",
    "apiIssuer": "API_제공자",
    "apiOwner": "API_소유자",
    "apiDomain": "GOVERNMENT",
    "apiKeyword": "API_DOCUMENT",
    "httpMethod": "GET",
    "apiDescription": "API_설명",
    "autoTokenRefresh": true,
    "apiEffectiveness": true,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

#### 2.2.2 이름 기반 상세 조회
**GET** `/external-apis/detail/name/{apiName}`

API 이름을 기반으로 상세 정보를 조회합니다.

**Response:** ID 기반 조회와 동일한 형식

### 2.3 API 목록 조회
**GET** `/external-apis/list`

등록된 API 목록을 페이지네이션으로 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호 (기본값: 0)
- `size` (optional): 페이지 크기 (기본값: 20)
- `domain` (optional): 도메인 필터
- `keyword` (optional): 키워드 필터
- `status` (optional): 상태 필터

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "apiId": "API_ID",
        "apiName": "API_이름",
        "apiUrl": "https://api.example.com/endpoint",
        "apiDomain": "GOVERNMENT",
        "apiKeyword": "API_DOCUMENT",
        "status": "ACTIVE"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

### 2.4 API 검색 (통합 검색)
**GET** `/external-apis/search`

API를 검색합니다. 이름, 설명, URL, 도메인, 키워드 등을 포함한 통합 검색이 가능합니다.

**Query Parameters:**
- `domain` (optional): 도메인 필터 (예: GOVERNMENT, WEATHER, FINANCE)
- `keyword` (optional): 키워드 필터 (예: API_DOCUMENT, DATA_ANALYSIS)
- `searchTerm` (optional): 일반 검색어 (이름, 설명, URL 등)

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "api": {
        "apiId": "API_ID",
        "apiName": "API_이름",
        "apiUrl": "https://api.example.com/endpoint",
        "apiDomain": "GOVERNMENT",
        "apiKeyword": "API_DOCUMENT"
      },
      "parameters": [
        {
          "paramName": "파라미터명",
          "paramType": "STRING",
          "isRequired": true,
          "defaultValue": "기본값"
        }
      ]
    }
  ],
  "message": "검색 결과 10개를 찾았습니다."
}
```

### 2.5 벌크 검색 (Bulk Search)
**POST** `/external-apis/bulk-search`

다중 도메인과 키워드를 이용한 벌크 검색을 수행합니다. 복잡한 검색 조건을 한 번에 처리할 수 있습니다.

**Request Body:**
```json
{
  "domains": ["GOVERNMENT", "WEATHER", "FINANCE"],
  "keywords": ["API_DOCUMENT", "DATA_ANALYSIS", "REAL_TIME"]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "totalCount": 25,
    "summary": {
      "requestedDomains": 3,
      "requestedKeywords": 3,
      "matchedCombinations": 9,
      "totalApis": 25
    },
    "results": {
      "GOVERNMENT-API_DOCUMENT": [
        {
          "api": {
            "apiId": "API_001",
            "apiName": "정부 API 문서",
            "apiDomain": "GOVERNMENT",
            "apiKeyword": "API_DOCUMENT"
          },
          "parameters": [
            {
              "paramName": "serviceKey",
              "paramType": "STRING",
              "isRequired": true
            }
          ]
        }
      ],
      "WEATHER-DATA_ANALYSIS": [
        {
          "api": {
            "apiId": "API_002",
            "apiName": "날씨 데이터 분석",
            "apiDomain": "WEATHER",
            "apiKeyword": "DATA_ANALYSIS"
          },
          "parameters": [
            {
              "paramName": "location",
              "paramType": "STRING",
              "isRequired": true
            }
          ]
        }
      ]
    }
  },
  "message": "벌크 검색 완료: 9개 조합에서 총 25개의 API를 찾았습니다."
}
```

**Rate Limit:** 1시간에 최대 500회 (벌크 검색은 제한적)

### 2.6 API 수정
**PUT** `/external-apis/update/{apiId}`

API 정보를 수정합니다.

**Request Body:**
```json
{
  "apiName": "수정된_API_이름",
  "apiDescription": "수정된_설명",
  "autoTokenRefresh": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "apiId": "API_ID",
    "apiName": "수정된_API_이름",
    "updatedAt": "2025-01-01T00:00:00"
  },
  "message": "API가 성공적으로 수정되었습니다."
}
```

### 2.7 API 삭제
**DELETE** `/external-apis/delete/{apiId}`

API를 삭제합니다 (소프트 삭제).

**Response:**
```json
{
  "success": true,
  "message": "API가 성공적으로 삭제되었습니다."
}
```

---

## 3. API 파라미터 관리 (ExternalApiController에 통합)

### 3.1 파라미터 조회

#### 3.1.1 API의 모든 파라미터 조회
**GET** `/external-apis/{apiId}/parameters`

특정 API의 모든 파라미터를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "parameterId": "파라미터_ID",
      "paramName": "파라미터명",
      "paramType": "STRING",
      "paramDescription": "파라미터_설명",
      "required": true,
      "defaultValue": "기본값"
    }
  ]
}
```

### 3.2 파라미터 수정 또는 추가
**PUT** `/external-apis/{apiId}/parameters/{parameterId}`

파라미터를 수정하거나 새로 추가합니다.

**Request Body:**
```json
{
  "paramName": "파라미터명",
  "paramType": "STRING",
  "paramDescription": "파라미터_설명",
  "required": true,
  "defaultValue": "기본값"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "parameterId": "파라미터_ID",
    "paramName": "파라미터명",
    "status": "UPDATED"
  },
  "message": "파라미터가 성공적으로 수정되었습니다."
}
```

### 3.3 파라미터 삭제
**DELETE** `/external-apis/{apiId}/parameters/{parameterId}`

파라미터를 삭제합니다 (소프트 삭제).

**Response:**
```json
{
  "success": true,
  "message": "파라미터가 성공적으로 삭제되었습니다."
}
```

---

## 4. API 헬스체크

**⚠️ 주의: 헬스체크는 스케줄러에 의해 자동으로 수행됩니다.**

### 4.1 전체 API 헬스체크 결과 조회 (페이징)
**GET** `/api-health/health/status/all`

모든 API의 헬스체크 상태를 페이징으로 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호 (기본값: 0)
- `size` (optional): 페이지 크기 (기본값: 20)

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "apiId": "API_ID",
        "apiName": "API_이름",
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

### 4.2 사용 불가능한 API 목록 조회
**GET** `/api-health/health/unavailable`

사용 불가능한 API ID 목록을 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": ["API_ID_1", "API_ID_2", "API_ID_3"]
}
```

### 4.3 특정 API 사용 불가능 여부 확인
**GET** `/api-health/health/unavailable/{apiId}`

특정 API가 사용 불가능한지 확인합니다.

**Response:**
```json
{
  "success": true,
  "data": true
}
```

---

## 5. Rate Limiting 테스트

### 5.1 IP 기반 Rate Limiting 테스트
**GET** `/api/v1/rate-limit-test/ip-based`

IP 기반 Rate Limiting을 테스트합니다 (1분에 최대 5회).

**Response:**
```json
{
  "success": true,
  "message": "IP 기반 Rate Limiting 테스트 성공",
  "data": "현재 시간: 2025-01-01T00:00:00"
}
```

### 5.2 API 키 기반 Rate Limiting 테스트
**GET** `/api/v1/rate-limit-test/api-key-based`

API 키 기반 Rate Limiting을 테스트합니다 (1시간에 최대 100회).

**Headers:**
- `X-API-Key`: API 키

**Response:**
```json
{
  "success": true,
  "message": "API 키 기반 Rate Limiting 테스트 성공",
  "data": "API Key: test_key, 현재 시간: 2025-01-01T00:00:00"
}
```

### 5.3 사용자 기반 Rate Limiting 테스트
**GET** `/api/v1/rate-limit-test/user-based`

사용자 기반 Rate Limiting을 테스트합니다 (1일에 최대 1000회).

**Headers:**
- `X-User-ID`: 사용자 ID

**Response:**
```json
{
  "success": true,
  "message": "사용자 기반 Rate Limiting 테스트 성공",
  "data": "User ID: user123, 현재 시간: 2025-01-01T00:00:00"
}
```

### 5.4 세션 기반 Rate Limiting 테스트
**GET** `/api/v1/rate-limit-test/session-based`

세션 기반 Rate Limiting을 테스트합니다 (1분에 최대 3회).

**Response:**
```json
{
  "success": true,
  "message": "세션 기반 Rate Limiting 테스트 성공",
  "data": "현재 시간: 2025-01-01T00:00:00"
}
```

### 5.5 Rate Limit 상태 조회
**GET** `/api/v1/rate-limit-test/status`

현재 Rate Limiting 상태를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalRequests": 150,
    "rateLimitedRequests": 5,
    "activeKeys": 25
  }
}
```

### 5.6 Rate Limit 캐시 정리
**POST** `/api/v1/rate-limit-test/cleanup`

Rate Limiting 캐시를 정리합니다.

**Response:**
```json
{
  "success": true,
  "message": "Rate Limiting 캐시가 성공적으로 정리되었습니다."
}
```

---

## 6. 로그 수집 (ELK Stack 연동)

### 6.1 로그 수집
**POST** `/api/v1/logs/collect`

단일 로그를 수집합니다.

**Request Body:**
```json
{
  "serviceName": "API_Management_Service",
  "level": "INFO",
  "message": "로그_메시지",
  "timestamp": "2025-01-01T00:00:00",
  "metadata": {
    "userId": "user123",
    "requestId": "req456"
  }
}
```

**Response:**
```json
{
  "success": true,
  "message": "로그가 성공적으로 수집되었습니다."
}
```

### 6.2 배치 로그 수집
**POST** `/api/v1/logs/collect-batch`

여러 로그를 배치로 수집합니다.

**Request Body:**
```json
{
  "logs": [
    {
      "serviceName": "Service1",
      "level": "INFO",
      "message": "로그1"
    },
    {
      "serviceName": "Service2",
      "level": "ERROR",
      "message": "로그2"
    }
  ]
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "collectedCount": 2,
    "failedCount": 0
  },
  "message": "배치 로그 수집이 완료되었습니다."
}
```

### 6.3 로그 수집 서비스 헬스체크
**GET** `/api/v1/logs/health`

로그 수집 서비스의 상태를 확인합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "status": "HEALTHY",
    "timestamp": "2025-01-01T00:00:00"
  }
}
```

### 6.4 로그 수집 통계
**GET** `/api/v1/logs/stats`

로그 수집 통계를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "totalLogs": 15000,
    "todayLogs": 500,
    "serviceCount": 5
  }
}
```

### 6.5 로그 수집 가이드
**GET** `/api/v1/logs/guide`

로그 수집 가이드를 조회합니다.

**Response:**
```json
{
  "success": true,
  "data": {
    "format": "JSON",
    "requiredFields": ["serviceName", "level", "message"],
    "examples": ["예시_로그_형식"]
  }
}
```

---

## 7. 공통 응답 형식

### 7.1 성공 응답
```json
{
  "success": true,
  "data": "응답_데이터",
  "message": "성공_메시지"
}
```

### 7.2 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러_메시지",
    "details": "상세_에러_정보"
  }
}
```

### 7.3 페이지네이션 응답
```json
{
  "success": true,
  "data": {
    "content": ["데이터_목록"],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

## 8. 에러 코드

| 코드 | 설명 |
|------|------|
| `INVALID_REQUEST` | 잘못된 요청 |
| `RESOURCE_NOT_FOUND` | 리소스를 찾을 수 없음 |
| `UNAUTHORIZED` | 인증되지 않음 |
| `FORBIDDEN` | 접근 권한 없음 |
| `RATE_LIMIT_EXCEEDED` | 요청 한도 초과 |
| `INTERNAL_SERVER_ERROR` | 내부 서버 오류 |

---

## 9. 데이터 타입

### 9.1 API 도메인 (ApiDomain)
- `GOVERNMENT`: 정부
- `FINANCE`: 금융
- `HEALTHCARE`: 의료
- `EDUCATION`: 교육
- `TRANSPORTATION`: 교통
- `COMMERCE`: 상거래
- `ENTERTAINMENT`: 엔터테인먼트
- `TECHNOLOGY`: 기술
- `OTHERS`: 기타

### 9.2 API 키워드 (ApiKeyword)
- `API_DOCUMENT`: API 문서
- `DATA_ANALYSIS`: 데이터 분석
- `PAYMENT`: 결제
- `AUTHENTICATION`: 인증
- `NOTIFICATION`: 알림
- `SEARCH`: 검색
- `FILE_UPLOAD`: 파일 업로드
- `REPORTING`: 리포트
- `OTHERS`: 기타

### 9.3 API 키 상태 (ApiKeyStatus)
- `ACTIVE`: 활성
- `INACTIVE`: 비활성
- `EXPIRED`: 만료됨
- `SUSPENDED`: 일시정지

---

## 10. 인증 및 보안

### 10.1 Rate Limiting
- 모든 API 엔드포인트에 Rate Limiting 적용
- IP 주소, API 키, 사용자 ID, 세션 기반 제한 지원
- 설정 가능한 시간 단위 (초, 분, 시간, 일)

### 10.2 API 키 인증
- API 키를 통한 서비스 인증
- 조직별 API 키 관리
- 사용량 제한 및 모니터링

---

## 11. 모니터링 및 헬스체크

### 11.1 자동화된 기능
- **API 헬스체크**: 5분(활성), 30분(중요), 일일(전체) 간격으로 자동 실행
- **토큰 갱신**: 4시간마다 자동으로 API 토큰 갱신
- **AI 분류**: API 등록 시 자동으로 도메인과 키워드 분류

### 11.2 모니터링 엔드포인트
- 헬스체크 결과 조회
- 사용 불가능한 API 목록 조회
- API 상태 모니터링

---

## 12. 요약

**총 엔드포인트 수: 36개**

- **API 키 관리**: 9개
- **External API 관리**: 12개 (벌크 검색 추가)
- **API 헬스체크**: 3개
- **Rate Limiting 테스트**: 6개
- **로그 수집**: 6개

**자동화된 기능**:
- 스케줄러 기반 API 헬스체크
- 자동 토큰 갱신
- AI 기반 API 분류

**테스트 기능**:
- Rate Limiting 동작 검증
- 다양한 제한 시나리오 테스트



