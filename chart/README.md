# API Management Service Helm Chart

이 Helm 차트는 API Management Service를 Kubernetes에 배포하기 위한 것입니다.

## 개요

API Management Service는 다음과 같은 기능을 제공하는 Spring Boot 기반 마이크로서비스입니다:

- **API 키 관리**: API 키 생성, 검증, 갱신
- **외부 API 등록**: 외부 API 서비스 등록 및 관리
- **레이트 리미팅**: API 사용량 제한 및 모니터링
- **AI 분류**: Gemini AI를 활용한 API 자동 분류
- **헬스 체크**: Spring Boot Actuator 기반 상태 모니터링
- **로깅 및 모니터링**: ELK 스택 연동 및 Prometheus 메트릭 수집

## 사전 요구사항

- Kubernetes 1.20+
- Helm 3.0+
- Ingress Controller (nginx-ingress 권장)
- cert-manager (TLS 인증서 자동 발급용)
- Prometheus Operator (ServiceMonitor 사용시)

## 설치

### 1. 저장소 추가 및 업데이트

```bash
helm repo add api-management https://your-helm-repo.com
helm repo update
```

### 2. 기본 설치

```bash
helm install api-management-service ./chart \
  --namespace api-management \
  --create-namespace
```

### 3. 커스텀 값으로 설치

```bash
helm install api-management-service ./chart \
  --namespace api-management \
  --create-namespace \
  -f values-custom.yaml
```

## 설정

### 주요 설정 항목

#### 리소스 할당

```yaml
resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: 2000m
    memory: 4Gi
```

#### 자동 확장

```yaml
hpa:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80
```

#### 헬스 체크

```yaml
livenessProbe:
  httpGet:
    path: /api/v1/actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /api/v1/actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
```

#### 환경 변수

```yaml
env:
  SPRING_PROFILES_ACTIVE: "prod"
  DB_URL: "jdbc:mysql://mysql-service:3306/api_management_service_db"
  REDIS_HOST: "redis-service"
  KAFKA_BOOTSTRAP_SERVERS: "kafka-service:9092"
```

### 환경별 설정

#### 개발 환경

```bash
helm install api-management-service ./chart \
  --namespace api-management-dev \
  --create-namespace \
  --set global.environment=dev \
  --set replicaCount=1 \
  --set hpa.enabled=false
```

#### 프로덕션 환경

```bash
helm install api-management-service ./chart \
  --namespace api-management-prod \
  --create-namespace \
  --set global.environment=prod \
  --set replicaCount=3 \
  --set hpa.enabled=true \
  --set ingress.enabled=true
```

## 배포 후 확인

### 1. Pod 상태 확인

```bash
kubectl get pods -n api-management
kubectl describe pod -l app.kubernetes.io/name=api-management-service -n api-management
```

### 2. 서비스 상태 확인

```bash
kubectl get svc -n api-management
kubectl get ingress -n api-management
```

### 3. 로그 확인

```bash
kubectl logs -l app.kubernetes.io/name=api-management-service -n api-management
```

### 4. 헬스 체크

```bash
# Pod 포트포워딩
kubectl port-forward svc/api-management-service 8080:8080 -n api-management

# 헬스 체크
curl http://localhost:8080/api/v1/actuator/health
curl http://localhost:8080/api/v1/actuator/health/liveness
curl http://localhost:8080/api/v1/actuator/health/readiness
```

## 업그레이드

```bash
helm upgrade api-management-service ./chart \
  --namespace api-management \
  -f values-custom.yaml
```

## 롤백

```bash
helm rollback api-management-service 1 -n api-management
```

## 제거

```bash
helm uninstall api-management-service -n api-management
kubectl delete namespace api-management
```

## 모니터링

### Prometheus 메트릭

- **엔드포인트**: `/api/v1/actuator/prometheus`
- **포트**: 8080
- **ServiceMonitor**: 자동 생성 (Prometheus Operator 필요)

### 주요 메트릭

- JVM 메트릭 (메모리, GC, 스레드)
- HTTP 요청/응답 메트릭
- 데이터베이스 커넥션 풀 상태
- Redis 커넥션 상태
- Kafka 프로듀서/컨슈머 상태

## 네트워크 정책

기본적으로 다음 네트워크 정책이 적용됩니다:

- **인그레스**: Ingress Controller에서만 접근 허용
- **이그레스**: 
  - 데이터베이스 (MySQL: 3306)
  - 캐시 (Redis: 6379)
  - 메시징 (Kafka: 9092)
  - 외부 HTTPS (443, 80)

## 보안

- **보안 컨텍스트**: 비루트 사용자로 실행
- **읽기 전용 파일시스템**: 보안 강화
- **권한 제한**: 불필요한 capabilities 제거
- **Secret 관리**: 민감한 정보는 Kubernetes Secret 사용

## 문제 해결

### 일반적인 문제

1. **Pod가 시작되지 않는 경우**
   - 리소스 요구사항 확인
   - 이미지 풀 권한 확인
   - ConfigMap/Secret 존재 여부 확인

2. **헬스 체크 실패**
   - 애플리케이션 로그 확인
   - Actuator 엔드포인트 접근 가능 여부 확인
   - 초기 지연 시간 조정

3. **연결 문제**
   - 서비스 DNS 확인
   - 네트워크 정책 확인
   - 방화벽 설정 확인

### 디버깅 명령어

```bash
# Pod 상세 정보
kubectl describe pod <pod-name> -n api-management

# 컨테이너 로그
kubectl logs <pod-name> -c api-management-service -n api-management

# Pod 내부 접근
kubectl exec -it <pod-name> -n api-management -- /bin/sh

# 서비스 연결 테스트
kubectl run test-connection --image=busybox -it --rm --restart=Never -n api-management -- wget -O- http://api-management-service:8080/api/v1/actuator/health
```

## 지원

문제가 발생하거나 질문이 있으시면 다음 연락처로 문의해주세요:

- **이메일**: team@yourcompany.com
- **GitHub Issues**: [프로젝트 저장소](https://github.com/your-org/BE-APIManagementService)

## 라이선스

이 프로젝트는 Apache 2.0 라이선스 하에 배포됩니다.
