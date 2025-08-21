package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * API 키 관리 엔티티
 * 
 * 이 엔티티는 외부 API 서비스의 API 키 정보를 체계적으로 관리하는 핵심 엔티티입니다.
 * 
 * 주요 기능:
 * - 기관별 API 키 발급 및 관리
 * - 일일/월간 사용량 제한 및 모니터링
 * - API 키 만료일 관리
 * - 사용량 통계 및 이력 추적
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Entity
@Table(name = "api_keys")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EntityListeners(AuditingEntityListener.class)
public class ApiKey extends BaseEntity {

    /**
     * 고유 API 키 ID
     * 예: "ORG_001", "ORG_002"
     */
    @Id
    @Column(name = "key_id", length = 100)
    private String keyId;

    /**
     * 기관명 (필수)
     * 예: "서울시청", "부산대학교", "삼성전자"
     */
    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;

    /**
     * 기관 코드 (선택)
     * 예: "SEOUL001", "PNU001", "SAMSUNG001"
     */
    @Column(name = "organization_code", length = 100)
    private String organizationCode;

    /**
     * 연락처 이메일 (필수)
     */
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    /**
     * 연락처 전화번호 (선택)
     */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /**
     * API 서비스명 (필수)
     * 예: "SGIS", "KAKAO", "NAVER", "GOOGLE"
     */
    @Column(name = "api_service_name", nullable = false, length = 100)
    private String apiServiceName;

    /**
     * API 서비스 URL (선택)
     * 예: "https://sgisapi.kostat.go.kr", "https://api.kakao.com"
     */
    @Column(name = "api_service_url", length = 500)
    private String apiServiceUrl;

    /**
     * 발급받은 실제 API 키 (필수)
     */
    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    /**
     * 발급받은 시크릿 키 (선택)
     */
    @Column(name = "secret_key", length = 500)
    private String secretKey;

    /**
     * 일일 API 호출 제한 횟수 (선택)
     */
    @Column(name = "daily_limit")
    private Integer dailyLimit;

    /**
     * 월간 API 호출 제한 횟수 (선택)
     */
    @Column(name = "monthly_limit")
    private Integer monthlyLimit;

    /**
     * 현재 일일 사용량
     */
    @Column(name = "current_daily_usage")
    private Integer currentDailyUsage;

    /**
     * 현재 월간 사용량
     */
    @Column(name = "current_monthly_usage")
    private Integer currentMonthlyUsage;

    /**
     * API 키 발급일시
     */
    @Column(name = "issued_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    /**
     * API 키 만료일시
     */
    @Column(name = "expires_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 마지막 사용일시
     */
    @Column(name = "last_used_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;

    /**
     * API 키 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApiKeyStatus status;

    /**
     * API 키 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 요청한 API 목록 (JSON 형태)
     */
    @Column(name = "requested_apis", columnDefinition = "TEXT")
    private String requestedApis;

    /**
     * 엔티티 저장 전 자동 실행되는 메서드
     */
    @PrePersist
    protected void onCreate() {
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ApiKeyStatus.ACTIVE;
        }
        if (currentDailyUsage == null) {
            currentDailyUsage = 0;
        }
        if (currentMonthlyUsage == null) {
            currentMonthlyUsage = 0;
        }
    }

    /**
     * API 키가 만료되었는지 확인
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 일일 사용량 제한 확인
     */
    public boolean isDailyLimitExceeded() {
        return dailyLimit != null && currentDailyUsage >= dailyLimit;
    }

    /**
     * 월간 사용량 제한 확인
     */
    public boolean isMonthlyLimitExceeded() {
        return monthlyLimit != null && currentMonthlyUsage >= monthlyLimit;
    }

    /**
     * API 키가 활성 상태인지 확인
     */
    public boolean isActive() {
        return ApiKeyStatus.ACTIVE.equals(status) && !isExpired();
    }

    /**
     * 사용량 제한이 설정되어 있는지 확인
     */
    public boolean hasUsageLimits() {
        return dailyLimit != null || monthlyLimit != null;
    }

    /**
     * 특정 API 서비스인지 확인
     */
    public boolean isService(String serviceName) {
        return apiServiceName != null && apiServiceName.equalsIgnoreCase(serviceName);
    }
}
