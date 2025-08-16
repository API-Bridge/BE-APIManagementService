package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;

import java.time.LocalDateTime;

/**
 * SGIS API 키 관리 엔티티
 * 
 * 이 엔티티는 SGIS(Statistical Geographic Information Service) 공공데이터포털의 
 * API 키 정보를 관리하는 핵심 엔티티입니다.
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
@Table(name = "sgis_api_keys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SgisApiKey extends BaseEntity {


    /**
     * SGIS에서 발급한 고유 API 키 ID
     * 예: "SGIS_ORG_001", "SGIS_ORG_002"
     * 이 값은 SGIS 시스템과의 연동에서 사용됩니다.
     */
    @Column(name = "key_id", length = 100, unique = true)
    private String keyId;

    /**
     * 기관명 (필수)
     * 예: "서울시청", "부산대학교", "삼성전자"
     * API 키 발급 시 기관을 식별하는 주요 정보입니다.
     */
    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName;

    /**
     * 기관 코드 (선택)
     * 예: "SEOUL001", "PNU001", "SAMSUNG001"
     * 기관을 체계적으로 분류하기 위한 코드입니다.
     */
    @Column(name = "organization_code", length = 100)
    private String organizationCode;

    /**
     * 연락처 이메일 (필수)
     * API 키 발급 및 관리 관련 공지사항을 받을 이메일 주소입니다.
     * 예: "admin@seoul.go.kr", "it@pnus.ac.kr"
     */
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    /**
     * 연락처 전화번호 (선택)
     * 긴급한 상황 발생 시 연락할 수 있는 전화번호입니다.
     * 예: "02-1234-5678", "051-123-4567"
     */
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    /**
     * SGIS에서 발급한 실제 API 키 (필수)
     * HTTP 요청 시 Authorization 헤더에 포함되어 사용됩니다.
     * 예: "Bearer SGIS_API_KEY_12345ABCDEF67890"
     * 
     * 보안 주의사항:
     * - 이 값은 절대 로그에 출력하지 않습니다.
     * - 데이터베이스에 암호화하여 저장하는 것을 권장합니다.
     */
    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    /**
     * SGIS에서 발급한 시크릿 키 (필수)
     * API 키와 함께 사용되어 요청의 무결성을 검증합니다.
     * 예: "SGIS_SECRET_98765FEDCBA01234"
     * 
     * 보안 주의사항:
     * - 이 값은 절대 클라이언트에게 노출하지 않습니다.
     * - 서버 내부에서만 사용되어야 합니다.
     */
    @Column(name = "secret_key", nullable = false, length = 500)
    private String secretKey;

    /**
     * 일일 API 호출 제한 횟수 (필수)
     * 24시간(00:00 ~ 23:59) 동안 사용할 수 있는 최대 API 호출 횟수입니다.
     * 예: 1000 (하루에 최대 1000번의 API 호출 가능)
     * 
     * 제한 도달 시:
     * - API 호출이 차단됩니다.
   * - 다음 날 00:00에 자동으로 초기화됩니다.
     */
    @Column(name = "daily_limit", nullable = false)
    private Integer dailyLimit;

    /**
     * 월간 API 호출 제한 횟수 (필수)
     * 한 달(1일 ~ 말일) 동안 사용할 수 있는 최대 API 호출 횟수입니다.
     * 예: 30000 (한 달에 최대 30000번의 API 호출 가능)
     * 
     * 제한 도달 시:
     * - API 호출이 차단됩니다.
     * - 다음 달 1일에 자동으로 초기화됩니다.
     */
    @Column(name = "monthly_limit", nullable = false)
    private Integer monthlyLimit;

    /**
     * 현재 일일 사용량 (자동 관리)
     * 오늘 00:00부터 현재까지 사용한 API 호출 횟수입니다.
     * 
     * 자동 업데이트:
     * - API 호출 시마다 +1 증가
     * - 매일 00:00에 자동으로 0으로 초기화
     * - dailyLimit에 도달하면 더 이상 증가하지 않음
     */
    @Column(name = "current_daily_usage")
    private Integer currentDailyUsage;

    /**
     * 현재 월간 사용량 (자동 관리)
     * 이번 달 1일부터 현재까지 사용한 API 호출 횟수입니다.
     * 
     * 자동 업데이트:
     * - API 호출 시마다 +1 증가
     * - 매월 1일에 자동으로 0으로 초기화
     * - monthlyLimit에 도달하면 더 이상 증가하지 않음
     */
    @Column(name = "current_monthly_usage")
    private Integer currentMonthlyUsage;

    /**
     * API 키 발급일시 (필수, 자동 설정)
     * SGIS에서 API 키를 발급받은 정확한 날짜와 시간입니다.
     * 
     * 자동 설정:
     * - 엔티티 생성 시 현재 시간으로 자동 설정
     * - @PrePersist 어노테이션으로 관리
     */
    @Column(name = "issued_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime issuedAt;

    /**
     * API 키 만료일시 (필수)
     * 이 API 키가 더 이상 사용할 수 없는 날짜와 시간입니다.
     * 
     * 만료 시:
     * - API 호출이 차단됩니다.
     * - 자동으로 EXPIRED 상태로 변경됩니다.
     * - 사용자는 키 갱신을 요청해야 합니다.
     * 
     * 예: 발급일로부터 1년 후, 2025-12-31 23:59:59
     */
    @Column(name = "expires_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 마지막 사용일시 (자동 업데이트)
     * 이 API 키로 마지막으로 API를 호출한 날짜와 시간입니다.
     * 
     * 자동 업데이트:
     * - API 호출 시마다 현재 시간으로 업데이트
     * - 사용량 통계 및 모니터링에 활용
     * - 비활성 키 식별에 사용
     */
    @Column(name = "last_used_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastUsedAt;

    /**
     * API 키 상태 (필수)
     * 현재 API 키의 사용 가능 여부를 나타내는 상태값입니다.
     * 
     * 가능한 상태:
     * - ACTIVE: 정상 사용 가능
     * - INACTIVE: 일시적으로 사용 중단
     * - EXPIRED: 만료됨
     * - SUSPENDED: 정책 위반으로 정지
     * - REVOKED: 사용 권한 취소
     * - PENDING: 승인 대기 중
     * 
     * 상태 변경은 관리자만 가능하며, 자동 상태 변경도 있습니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApiKeyStatus status;

    /**
     * API 키 설명 (선택)
     * 이 API 키의 용도나 특별한 사항을 기록하는 메모 필드입니다.
     * 
     * 예시:
     * - "서울시 인구통계 데이터 수집용"
     * - "부산대학교 연구 프로젝트용"
     * - "테스트 환경용 (2024년 3월까지)"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 요청한 API 목록 (JSON 형태)
     * 이 기관이 사용하고자 하는 SGIS API 서비스들의 목록입니다.
     * 
     * 저장 형식: JSON 배열 문자열
     * 예: "[\"인구통계\", \"경제통계\", \"지리정보\"]"
     * 
     * 용도:
     * - API 키 발급 시 요청 사유로 활용
     * - 사용량 분석 및 통계에 활용
     * - API별 사용 패턴 분석에 활용
     */
    @Column(name = "requested_apis", columnDefinition = "TEXT")
    private String requestedApis;

    /**
     * 엔티티 저장 전 자동 실행되는 메서드
     * 
     * 이 메서드는 JPA 엔티티가 데이터베이스에 저장되기 전에
     * 자동으로 호출되어 기본값들을 설정합니다.
     * 
     * 설정되는 기본값들:
     * - issuedAt: 현재 시간 (API 키 발급 시점)
     * - status: ACTIVE (활성 상태로 시작)
     * - currentDailyUsage: 0 (일일 사용량 초기화)
     * - currentMonthlyUsage: 0 (월간 사용량 초기화)
     * 
     * @PrePersist 어노테이션으로 JPA가 자동 호출
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
     * 
     * 현재 시간과 만료일시를 비교하여 API 키가 만료되었는지 판단합니다.
     * 
     * @return true: 만료됨, false: 아직 유효함
     * 
     * 사용 예시:
     * if (apiKey.isExpired()) {
     *     // 만료된 키 처리 로직
     *     sendExpirationNotification(apiKey);
     * }
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 일일 사용량 제한 확인
     * 
     * 현재 일일 사용량이 설정된 일일 제한에 도달했는지 확인합니다.
     * 
     * @return true: 일일 제한 도달, false: 아직 여유 있음
     * 
     * 사용 예시:
     * if (apiKey.isDailyLimitExceeded()) {
     *     // 일일 제한 도달 시 처리 로직
     *     throw new DailyLimitExceededException("일일 API 호출 제한에 도달했습니다.");
     * }
     * 
     * 비즈니스 로직:
     * - API 호출 전에 항상 확인해야 함
     * - 제한 도달 시 API 호출을 차단
     * - 다음 날 00:00에 자동으로 초기화
     */
    public boolean isDailyLimitExceeded() {
        return currentDailyUsage >= dailyLimit;
    }

    /**
     * 월간 사용량 제한 확인
     * 
     * 현재 월간 사용량이 설정된 월간 제한에 도달했는지 확인합니다.
     * 
     * @return true: 월간 제한 도달, false: 아직 여유 있음
     * 
     * 사용 예시:
     * if (apiKey.isMonthlyLimitExceeded()) {
     *     // 월간 제한 도달 시 처리 로직
     *     throw new MonthlyLimitExceededException("월간 API 호출 제한에 도달했습니다.");
     * }
     * 
     * 비즈니스 로직:
     * - API 호출 전에 항상 확인해야 함
     * - 제한 도달 시 API 호출을 차단
     * - 다음 달 1일에 자동으로 초기화
     * 
     * 주의사항:
     * - 일일 제한과 월간 제한은 독립적으로 동작
     * - 두 제한 중 하나라도 도달하면 API 호출 불가
     */
    public boolean isMonthlyLimitExceeded() {
        return currentMonthlyUsage >= monthlyLimit;
    }

    /**
     * API 키가 활성 상태인지 확인
     * 
     * API 키가 실제로 사용 가능한 상태인지 종합적으로 판단합니다.
     * 
     * 활성 상태 조건:
     * 1. status가 ACTIVE여야 함
     * 2. 만료일이 지나지 않아야 함
     * 
     * @return true: 활성 상태 (사용 가능), false: 비활성 상태 (사용 불가)
     * 
     * 사용 예시:
     * if (apiKey.isActive()) {
     *     // API 호출 허용
     *     processApiRequest(apiKey);
     * } else {
     *     // API 호출 거부
     *     throw new InactiveApiKeyException("비활성 API 키입니다.");
     * }
     * 
     * 비즈니스 로직:
     * - 모든 API 호출 전에 반드시 확인해야 함
     * - 활성 상태가 아니면 API 호출을 차단
     * - 사용자에게 적절한 오류 메시지 제공
     */
    public boolean isActive() {
        return ApiKeyStatus.ACTIVE.equals(status) && !isExpired();
    }
}
