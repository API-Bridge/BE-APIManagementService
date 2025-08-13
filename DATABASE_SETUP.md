# API 관리 서비스 데이터베이스 설정 가이드

## 📋 개요

이 문서는 API 관리 서비스의 데이터베이스 설정 방법을 설명합니다.

## 🏗️ 테이블 생성 방식

### 1. 공유 스키마 (SQL로 수동 생성)
- **`external_api`**: 외부 API 메타데이터 (팀장 공유)
- **`api_parameter`**: API 파라미터 정보 (팀장 공유)

### 2. 로그/메트릭 테이블 (엔티티로 자동 생성)
- **`custom_api_creation_log`**: 커스텀API 생성 로그
- **`custom_api_usage_log`**: 커스텀API 사용 로그
- **`public_api_token_refresh_log`**: 토큰 갱신 로그
- **`public_api_management_log`**: API 관리 로그
- **`public_api_rate_limit_log`**: 요청 제한 로그
- **`performance_metrics`**: 성능 메트릭
- **`ai_classification`**: AI 분류 결과

## 🚀 설정 단계

### 1단계: 데이터베이스 생성
```bash
# MySQL에 접속
mysql -u root -p

# 데이터베이스 생성
CREATE DATABASE api_management_service_db;
USE api_management_service_db;
```

### 2단계: 공유 스키마 테이블 생성
```bash
# SQL 스크립트 실행
mysql -u root -p api_management_service_db < src/main/resources/sql/init_database.sql
```

또는

```bash
# MySQL에 직접 접속하여 실행
mysql -u root -p api_management_service_db
source src/main/resources/sql/init_database.sql
```

### 3단계: 애플리케이션 실행
```bash
# Spring Boot 애플리케이션 실행
./gradlew bootRun
```

Spring Boot가 `ddl-auto: update` 설정으로 로그/메트릭 테이블을 자동 생성합니다.

## 📊 테이블 구조

### 공유 스키마 테이블
| 테이블명 | 설명 | 생성 방식 |
|----------|------|-----------|
| `external_api` | 외부 API 메타데이터 | SQL 수동 생성 |
| `api_parameter` | API 파라미터 정보 | SQL 수동 생성 |

### 로그/메트릭 테이블
| 테이블명 | 설명 | 생성 방식 |
|----------|------|-----------|
| `custom_api_creation_log` | 커스텀API 생성 로그 | 엔티티 자동 생성 |
| `custom_api_usage_log` | 커스텀API 사용 로그 | 엔티티 자동 생성 |
| `public_api_token_refresh_log` | 토큰 갱신 로그 | 엔티티 자동 생성 |
| `public_api_management_log` | API 관리 로그 | 엔티티 자동 생성 |
| `public_api_rate_limit_log` | 요청 제한 로그 | 엔티티 자동 생성 |
| `performance_metrics` | 성능 메트릭 | 엔티티 자동 생성 |
| `ai_classification` | AI 분류 결과 | 엔티티 자동 생성 |

## ⚙️ 설정 파일

### application.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update  # 로그/메트릭 테이블 자동 생성
```

### application-test.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop  # 테스트 시 모든 테이블 자동 생성/삭제
```

## 🔍 확인 방법

### 1. 테이블 생성 확인
```sql
USE api_management_service_db;
SHOW TABLES;
```

### 2. 테이블 구조 확인
```sql
DESCRIBE external_api;
DESCRIBE custom_api_creation_log;
```

### 3. 샘플 데이터 확인
```sql
SELECT * FROM external_api;
SELECT * FROM api_parameter;
```

## 🚨 주의사항

1. **공유 스키마**: 팀장이 공유한 구조를 정확히 유지
2. **로그 테이블**: API 관리 서비스 전용으로 독립적 관리
3. **데이터 백업**: 프로덕션 환경에서는 정기적인 백업 필요
4. **권한 관리**: 적절한 데이터베이스 사용자 권한 설정

## 🆘 문제 해결

### 테이블이 생성되지 않는 경우
1. `ddl-auto: update` 설정 확인
2. 데이터베이스 연결 상태 확인
3. 엔티티 클래스의 `@Table` 어노테이션 확인

### 권한 오류가 발생하는 경우
1. 데이터베이스 사용자 권한 확인
2. `GRANT` 명령어로 적절한 권한 부여

## 📞 지원

문제가 발생하면 개발팀에 문의하세요.
