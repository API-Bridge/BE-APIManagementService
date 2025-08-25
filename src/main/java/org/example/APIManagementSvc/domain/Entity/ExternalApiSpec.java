package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "external_api_specs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiSpec {

    @Id
    @Column(name = "api_id", length = 36)
    private String apiId;

    @Column(name = "api_name", nullable = false)
    private String apiName;

    @Lob
    @Column(name = "api_description")
    private String apiDescription;

    @Column(name = "api_issuer", nullable = false)
    private String apiIssuer;

    @Column(name = "api_url", nullable = false, length = 500)
    private String apiUrl;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** 
     * API 헬스체크 상태
     * HEALTHY: API 정상 동작 (HTTP 2xx 응답)
     * UNHEALTHY: API 비정상 동작 (HTTP 에러 또는 연결 실패)
     * UNKNOWN: 헬스체크 미실행 상태 (초기값)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status")
    @Builder.Default
    private HealthStatus healthStatus = HealthStatus.UNKNOWN;

    /** 
     * 마지막 헬스체크 실행 시각
     * 스케줄러에 의한 자동 체크 또는 수동 체크 시점을 기록
     */
    @Column(name = "last_health_check")
    private LocalDateTime lastHealthCheck;

    /** 
     * 헬스체크 전용 엔드포인트 경로
     * 기본값: "/health", 커스텀 경로 설정 가능
     * 예: "/status", "/ping", "/health-check"
     */
    @Column(name = "health_check_path")
    private String healthCheckPath;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    private ApiCredential credential;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private ApiDomain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private ApiKeyword keyword;

    @OneToMany(mappedBy = "apiSpec", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiParameter> parameters;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * API 헬스체크 상태 열거형
     * 
     * HEALTHY: API가 정상적으로 동작하여 HTTP 2xx 응답을 반환하는 상태
     * UNHEALTHY: API 호출 실패, 타임아웃, 또는 HTTP 에러 응답을 반환하는 상태  
     * UNKNOWN: 헬스체크가 아직 실행되지 않았거나 초기 상태
     */
    public enum HealthStatus {
        /** API 정상 동작 상태 - HTTP 2xx 응답 */
        HEALTHY, 
        
        /** API 비정상 동작 상태 - HTTP 에러, 타임아웃, 연결 실패 */
        UNHEALTHY, 
        
        /** 헬스체크 미실행 상태 - 초기값 또는 체크 전 */
        UNKNOWN
    }
}