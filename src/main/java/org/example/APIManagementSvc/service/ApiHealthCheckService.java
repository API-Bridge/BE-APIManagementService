package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ExternalApiSpec;
import org.example.APIManagementSvc.event.model.ExternalApiHealthCheckEvent;
import org.example.APIManagementSvc.event.publisher.EventPublisher;
import org.example.APIManagementSvc.repository.ExternalApiSpecRepository;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * API 헬스체크 서비스
 * 
 * 등록된 모든 외부 API의 상태를 주기적으로 모니터링하고 관리하는 서비스
 * 
 * 주요 기능:
 * - 1시간 주기 자동 헬스체크 스케줄링
 * - 비동기 헬스체크 수행으로 성능 최적화
 * - API 키를 활용한 인증된 헬스체크 요청
 * - 헬스체크 결과 DB 저장 및 상태 관리
 * 
 * 헬스체크 로직:
 * - HTTP GET 요청으로 API 응답성 확인
 * - 2xx 응답 시 HEALTHY, 그 외 UNHEALTHY로 판단
 * - 연결 타임아웃: 5초, 읽기 타임아웃: 10초
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiHealthCheckService {

    /** 외부 API 명세 데이터 접근 리포지토리 */
    private final ExternalApiSpecRepository externalApiSpecRepository;
    
    /** HTTP 요청을 위한 REST 클라이언트 */
    private final RestTemplate restTemplate;
    
    /** 헬스체크 실패 API Redis 캐시 관리 서비스 */
    private final ApiHealthCacheService apiHealthCacheService;
    
    /** Kafka 이벤트 발행 서비스 */
    private final EventPublisher eventPublisher;

    /**
     * 전체 API에 대한 정기 헬스체크 스케줄러
     * 
     * 매 1시간(3,600,000ms)마다 자동으로 실행되어 모든 활성화된 API의 상태를 확인
     * 각 API에 대해 비동기적으로 헬스체크를 수행하여 전체 처리 시간을 최적화
     * 
     * 실행 조건:
     * - Spring 애플리케이션이 실행 중일 때
     * - 이전 헬스체크가 완료된 후 1시간 경과
     * 
     * @throws Exception 스케줄링 중 발생하는 예외
     */
    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    @Transactional
    public void performHealthChecks() {
        log.info("Starting scheduled health checks for all external APIs");
        
        // 자격증명 정보와 함께 활성화된 API 목록 조회
        List<ExternalApiSpec> activeApis = externalApiSpecRepository.findActiveApisWithCredentials();
        log.info("Found {} active APIs to health check", activeApis.size());

        // 각 API에 대해 비동기 헬스체크 실행
        for (ExternalApiSpec apiSpec : activeApis) {
            performHealthCheckAsync(apiSpec);
        }
    }

    /**
     * 캐시된 비정상 API들에 대한 재검사 스케줄러
     * 
     * 매 10분(600,000ms)마다 실행되어 Redis 캐시에 저장된 비정상 API들만 재검사
     * TTL 만료 시점과 맞춰서 API 상태 재확인 및 캐시 갱신
     * 
     * 동작 방식:
     * - Redis에서 현재 캐시된 비정상 API 목록 조회
     * - 각 API에 대해 헬스체크 수행
     * - 정상화된 API는 캐시에서 제거, 여전히 비정상인 API는 캐시 TTL 갱신
     * 
     * @throws Exception 스케줄링 중 발생하는 예외
     */
    @Scheduled(fixedRate = 600000) // 10분마다 실행
    @Transactional
    public void performCachedUnhealthyApiRecheck() {
        log.info("Starting recheck for cached unhealthy APIs");
        
        try {
            // Redis에서 현재 캐시된 비정상 API ID 목록 조회
            Set<String> unhealthyApiIds = apiHealthCacheService.getUnhealthyApiIds();
            
            if (unhealthyApiIds.isEmpty()) {
                log.debug("No unhealthy APIs found in cache - skipping recheck");
                return;
            }
            
            log.info("Found {} unhealthy APIs in cache to recheck", unhealthyApiIds.size());
            
            // 각 비정상 API에 대해 재검사 수행
            for (String apiId : unhealthyApiIds) {
                try {
                    // DB에서 API 명세 조회
                    ExternalApiSpec apiSpec = externalApiSpecRepository.findById(apiId).orElse(null);
                    
                    if (apiSpec != null && apiSpec.getIsActive()) {
                        log.debug("Rechecking cached unhealthy API: {} ({})", apiSpec.getApiName(), apiId);
                        
                        // 비동기 헬스체크 수행
                        performHealthCheckAsync(apiSpec);
                    } else {
                        log.warn("API not found or inactive, removing from cache: {}", apiId);
                        // 존재하지 않거나 비활성화된 API는 캐시에서 제거
                        apiHealthCacheService.removeHealthyApi(apiId);
                    }
                    
                } catch (Exception e) {
                    log.error("Error rechecking unhealthy API: {}", apiId, e);
                }
            }
            
        } catch (Exception e) {
            log.error("Error during cached unhealthy API recheck", e);
        }
    }

    /**
     * 단일 API에 대한 비동기 헬스체크 수행
     * 
     * 개별 API에 대해 HTTP 요청을 보내고 응답 상태를 확인하여
     * 헬스 상태를 업데이트하는 비동기 메서드
     * 
     * 처리 과정:
     * 1. API 헬스체크 실행 (HTTP GET 요청)
     * 2. 응답 상태 코드 확인 (2xx = HEALTHY, 기타 = UNHEALTHY)
     * 3. DB에 헬스 상태 및 체크 시간 업데이트
     * 4. 로그 기록
     * 
     * @param apiSpec 헬스체크를 수행할 API 명세
     * @return CompletableFuture<Void> 비동기 처리 결과
     */
    @Async
    @Transactional
    public CompletableFuture<Void> performHealthCheckAsync(ExternalApiSpec apiSpec) {
        try {
            // API 헬스체크 실행
            ExternalApiSpec.HealthStatus healthStatus = checkApiHealth(apiSpec);
            
            // 헬스 상태 DB 업데이트
            updateHealthStatus(apiSpec.getApiId(), healthStatus);
            
            log.info("Health check completed for API: {} - Status: {}", 
                    apiSpec.getApiName(), healthStatus);
        } catch (Exception e) {
            log.error("Error performing health check for API: {}", apiSpec.getApiName(), e);
            
            // 예외 발생 시 UNHEALTHY로 상태 업데이트
            updateHealthStatus(apiSpec.getApiId(), ExternalApiSpec.HealthStatus.UNHEALTHY);
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 실제 API 헬스체크 로직 수행
     * 
     * HTTP GET 요청을 통해 API의 응답성을 확인하고 상태를 판단
     * 
     * 판단 기준:
     * - HTTP 2xx 응답: HEALTHY
     * - HTTP 4xx/5xx 응답: UNHEALTHY  
     * - 연결 실패/타임아웃: UNHEALTHY
     * 
     * @param apiSpec 헬스체크할 API 명세 정보
     * @return HealthStatus 헬스체크 결과 상태
     */
    private ExternalApiSpec.HealthStatus checkApiHealth(ExternalApiSpec apiSpec) {
        long startTime = System.currentTimeMillis();
        String status = "UNKNOWN";
        Integer statusCode = null;
        String errorMessage = null;
        
        try {
            // 헬스체크 URL 구성
            String healthCheckUrl = buildHealthCheckUrl(apiSpec);
            
            // 인증 헤더 구성
            HttpHeaders headers = buildHeaders(apiSpec);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.debug("Performing health check for API: {} at URL: {}", 
                    apiSpec.getApiName(), healthCheckUrl);

            // HTTP GET 요청 실행
            ResponseEntity<String> response = restTemplate.exchange(
                    healthCheckUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            statusCode = response.getStatusCode().value();
            long responseTime = System.currentTimeMillis() - startTime;
            
            // 응답 상태 코드 확인
            if (response.getStatusCode().is2xxSuccessful()) {
                status = "HEALTHY";
                log.debug("Health check successful for API: {}", apiSpec.getApiName());
                
                // 헬스체크 성공 이벤트 발행
                publishHealthCheckEvent(apiSpec, status, (int) responseTime, statusCode, null);
                
                return ExternalApiSpec.HealthStatus.HEALTHY;
            } else {
                status = "UNHEALTHY";
                errorMessage = "HTTP " + statusCode + " response";
                log.warn("Health check failed for API: {} - HTTP Status: {}", 
                        apiSpec.getApiName(), response.getStatusCode());
                
                // 헬스체크 실패 이벤트 발행
                publishHealthCheckEvent(apiSpec, status, (int) responseTime, statusCode, errorMessage);
                
                return ExternalApiSpec.HealthStatus.UNHEALTHY;
            }

        } catch (Exception e) {
            // 연결 실패, 타임아웃 등의 예외 처리
            long responseTime = System.currentTimeMillis() - startTime;
            status = "UNHEALTHY";
            errorMessage = e.getMessage();
            
            log.error("Health check exception for API: {} - Error: {}", 
                    apiSpec.getApiName(), e.getMessage());
            
            // 헬스체크 예외 이벤트 발행
            publishHealthCheckEvent(apiSpec, status, (int) responseTime, statusCode, errorMessage);
            
            return ExternalApiSpec.HealthStatus.UNHEALTHY;
        }
    }

    /**
     * 헬스체크용 URL 구성
     * 
     * API 명세에 등록된 기본 URL과 헬스체크 경로를 결합하여
     * 완전한 헬스체크 URL을 생성
     * 
     * URL 구성 로직:
     * 1. 커스텀 헬스체크 경로가 있으면 사용
     * 2. 없으면 기본 "/health" 경로 사용
     * 3. URL 끝과 경로 시작 부분의 슬래시(/) 중복 제거
     * 
     * @param apiSpec API 명세 정보
     * @return String 완전한 헬스체크 URL
     */
    private String buildHealthCheckUrl(ExternalApiSpec apiSpec) {
        String baseUrl = apiSpec.getApiUrl();
        String healthCheckPath = apiSpec.getHealthCheckPath();
        
        // 커스텀 헬스체크 경로가 지정된 경우
        if (healthCheckPath != null && !healthCheckPath.isEmpty()) {
            return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + 
                   (healthCheckPath.startsWith("/") ? healthCheckPath.substring(1) : healthCheckPath);
        }
        
        // 기본 헬스체크 경로 "/health" 사용
        return baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "health";
    }

    /**
     * HTTP 요청 헤더 구성
     * 
     * API 호출에 필요한 인증 정보와 콘텐츠 타입을 헤더에 설정
     * 
     * 헤더 구성:
     * - Content-Type: application/json (기본)
     * - X-API-Key: API 키 (인증용)
     * - Authorization: Bearer {API 키} (인증용)
     * 
     * @param apiSpec API 명세 정보 (자격증명 포함)
     * @return HttpHeaders 구성된 HTTP 헤더
     */
    private HttpHeaders buildHeaders(ExternalApiSpec apiSpec) {
        HttpHeaders headers = new HttpHeaders();
        
        // 기본 콘텐츠 타입 설정
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // API 키 인증 정보 설정 (있는 경우만)
        if (apiSpec.getCredential() != null && apiSpec.getCredential().getApiKey() != null) {
            String apiKey = apiSpec.getCredential().getApiKey();
            
            // 다양한 인증 방식 지원
            headers.set("X-API-Key", apiKey);           // 헤더 방식 API 키
            headers.set("Authorization", "Bearer " + apiKey); // Bearer 토큰 방식
        }
        
        return headers;
    }

    /**
     * API 헬스 상태 업데이트
     * 
     * 헬스체크 결과를 바탕으로 API의 헬스 상태와 마지막 체크 시간을 DB에 반영
     * 실패한 API는 Redis 캐시에 저장하고, 정상화된 API는 캐시에서 제거
     * 
     * 처리 로직:
     * 1. DB에 헬스 상태 업데이트
     * 2. UNHEALTHY → Redis 캐시에 저장 (TTL: 10분)
     * 3. HEALTHY → Redis 캐시에서 제거
     * 
     * @param apiId 업데이트할 API 식별자
     * @param healthStatus 새로운 헬스 상태
     */
    @Transactional
    public void updateHealthStatus(String apiId, ExternalApiSpec.HealthStatus healthStatus) {
        ExternalApiSpec apiSpec = externalApiSpecRepository.findById(apiId).orElse(null);
        if (apiSpec != null) {
            ExternalApiSpec.HealthStatus previousStatus = apiSpec.getHealthStatus();
            
            // 헬스 상태 및 체크 시간 업데이트
            apiSpec.setHealthStatus(healthStatus);
            apiSpec.setLastHealthCheck(LocalDateTime.now());
            
            // 변경 내용 DB 저장
            externalApiSpecRepository.save(apiSpec);
            
            // Redis 캐시 관리
            log.info("Calling handleHealthStatusCache for API: {} - Previous: {}, Current: {}", 
                    apiSpec.getApiName(), previousStatus, healthStatus);
            handleHealthStatusCache(apiSpec, previousStatus, healthStatus);
        }
    }
    
    /**
     * 헬스 상태 변경에 따른 Redis 캐시 관리
     * 
     * 헬스 상태 변화에 따라 Redis 캐시를 적절히 관리
     * 
     * 캐시 관리 로직:
     * - HEALTHY → UNHEALTHY: Redis에 실패 API 정보 캐시 (TTL: 10분)
     * - UNHEALTHY → HEALTHY: Redis에서 API 정보 제거
     * - UNKNOWN → UNHEALTHY: Redis에 실패 API 정보 캐시
     * - UNKNOWN → HEALTHY: 캐시 작업 없음
     * 
     * @param apiSpec API 명세 정보
     * @param previousStatus 이전 헬스 상태
     * @param currentStatus 현재 헬스 상태
     */
    private void handleHealthStatusCache(ExternalApiSpec apiSpec, 
                                       ExternalApiSpec.HealthStatus previousStatus,
                                       ExternalApiSpec.HealthStatus currentStatus) {
        try {
            log.info("handleHealthStatusCache called - API: {}, Previous: {}, Current: {}", 
                    apiSpec.getApiName(), previousStatus, currentStatus);
                    
            // API가 실패 상태인 경우
            if (currentStatus == ExternalApiSpec.HealthStatus.UNHEALTHY) {
                log.info("Caching unhealthy API: {}", apiSpec.getApiName());
                // 현재 UNHEALTHY 상태인 모든 API를 캐시에 저장
                apiHealthCacheService.cacheUnhealthyApi(apiSpec);
                if (previousStatus != ExternalApiSpec.HealthStatus.UNHEALTHY) {
                    log.warn("API became unhealthy, cached in Redis: {} ({})", 
                            apiSpec.getApiName(), apiSpec.getApiId());
                } else {
                    log.info("API remains unhealthy, updated cache: {} ({})", 
                            apiSpec.getApiName(), apiSpec.getApiId());
                }
            }
            // API가 정상 상태로 복구된 경우
            else if (currentStatus == ExternalApiSpec.HealthStatus.HEALTHY) {
                // 이전에 실패 상태였다면 캐시에서 제거
                if (previousStatus == ExternalApiSpec.HealthStatus.UNHEALTHY) {
                    apiHealthCacheService.removeHealthyApi(apiSpec.getApiId());
                    log.info("API recovered to healthy, removed from Redis cache: {} ({})", 
                            apiSpec.getApiName(), apiSpec.getApiId());
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to manage health status cache for API: {} ({})", 
                     apiSpec.getApiName(), apiSpec.getApiId(), e);
        }
    }

    /**
     * 특정 헬스 상태의 API 목록 조회
     * 
     * 지정된 헬스 상태(HEALTHY/UNHEALTHY/UNKNOWN)에 해당하는
     * 모든 API 목록을 조회하여 반환
     * 
     * 사용 예시:
     * - UNHEALTHY API만 필터링하여 장애 API 목록 확인
     * - HEALTHY API만 필터링하여 정상 동작 API 현황 파악
     * 
     * @param healthStatus 조회할 헬스 상태
     * @return List<ExternalApiSpec> 해당 상태의 API 목록
     */
    public List<ExternalApiSpec> getApisByHealthStatus(ExternalApiSpec.HealthStatus healthStatus) {
        return externalApiSpecRepository.findByHealthStatus(healthStatus);
    }

    /**
     * 모든 활성 API의 헬스 상태 조회
     * 
     * 활성화된(isActive=true) 모든 API의 헬스 상태를 조회
     * 대시보드나 모니터링 화면에서 사용
     * 
     * 반환 데이터:
     * - API 기본 정보 (이름, URL, 설명 등)
     * - 헬스 상태 (HEALTHY/UNHEALTHY/UNKNOWN)
     * - 마지막 체크 시간
     * 
     * @return List<ExternalApiSpec> 모든 활성 API 목록
     */
    public List<ExternalApiSpec> getAllApisWithHealthStatus() {
        return externalApiSpecRepository.findByIsActiveTrue();
    }

    /**
     * 특정 API에 대한 즉시 헬스체크 수행
     * 외부 API 호출 실패 이벤트 발생 시 호출되어 해당 API의 상태를 즉시 확인
     * 
     * @param apiId 헬스체크를 수행할 API 식별자
     */
    public void performImmediateHealthCheck(String apiId) {
        try {
            log.info("Triggering immediate health check for API ID: {}", apiId);
            
            // API 명세 조회
            ExternalApiSpec apiSpec = externalApiSpecRepository.findById(apiId).orElse(null);
            
            if (apiSpec == null) {
                log.warn("API not found for immediate health check: {}", apiId);
                return;
            }
            
            if (!apiSpec.getIsActive()) {
                log.warn("API is inactive, skipping immediate health check: {} ({})", 
                        apiSpec.getApiName(), apiId);
                return;
            }
            
            log.info("Starting immediate health check for API: {} ({})", apiSpec.getApiName(), apiId);
            
            // 기존 performHealthCheckAsync 메소드 재활용
            performHealthCheckAsync(apiSpec);
            
            log.info("Immediate health check triggered for API: {} ({})", apiSpec.getApiName(), apiId);
                    
        } catch (Exception e) {
            log.error("Error triggering immediate health check for API ID: {}", apiId, e);
        }
    }

    /**
     * 헬스체크 이벤트를 Kafka로 발행
     * 
     * @param apiSpec API 명세
     * @param status 헬스 상태
     * @param responseTime 응답 시간
     * @param statusCode HTTP 상태 코드
     * @param errorMessage 오류 메시지
     */
    private void publishHealthCheckEvent(ExternalApiSpec apiSpec, String status, 
                                       Integer responseTime, Integer statusCode, 
                                       String errorMessage) {
        try {
            ExternalApiHealthCheckEvent event = new ExternalApiHealthCheckEvent(
                    Long.parseLong(apiSpec.getApiId()),
                    apiSpec.getApiName(),
                    apiSpec.getApiUrl(),
                    status,
                    responseTime,
                    statusCode,
                    errorMessage,
                    LocalDateTime.now()
            );
            
            eventPublisher.publishEvent("external_api_events", event);
            
            if ("UNHEALTHY".equals(status)) {
                log.info("Published External API Health Check Event (UNHEALTHY) for API: {} - Status: {}, Error: {}",
                        apiSpec.getApiName(), status, errorMessage);
            } else {
                log.debug("Published External API Health Check Event for API: {} - Status: {}", 
                        apiSpec.getApiName(), status);
            }
            
        } catch (Exception e) {
            log.error("Failed to publish health check event for API: {}", apiSpec.getApiName(), e);
        }
    }
}