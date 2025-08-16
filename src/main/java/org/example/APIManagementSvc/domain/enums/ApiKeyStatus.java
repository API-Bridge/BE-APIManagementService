package org.example.APIManagementSvc.domain.enums;

/**
 * API 키 상태 Enum
 * 
 * SGIS API 키의 현재 상태를 나타내는 열거형입니다.
 * 각 상태는 API 키의 사용 가능 여부와 관리 상태를 명확하게 구분합니다.
 * 
 * 상태별 특징:
 * - ACTIVE: 정상적으로 사용 가능한 상태
 * - INACTIVE: 일시적으로 사용이 중단된 상태
 * - EXPIRED: 사용 기간이 만료된 상태
 * - SUSPENDED: 정책 위반 등으로 정지된 상태
 * - REVOKED: 사용 권한이 취소된 상태
 * - PENDING: 승인 대기 중인 상태
 * 
 * @author API Management Service Team
 * @version 1.0.0
 * @since 2024-01-01
 */
public enum ApiKeyStatus {
    /**
     * 활성 상태
     * API 키가 정상적으로 작동하며 모든 API 호출이 허용됩니다.
     * 사용량 제한 내에서 무제한으로 사용 가능합니다.
     */
    ACTIVE("ACTIVE", "활성", "정상적으로 사용 가능한 상태"),
    
    /**
     * 비활성 상태
     * 일시적으로 API 키 사용이 중단된 상태입니다.
     * 관리자가 수동으로 재활성화할 수 있습니다.
     * 일반적으로 정책 위반이나 점검 목적으로 설정됩니다.
     */
    INACTIVE("INACTIVE", "비활성", "일시적으로 사용이 중단된 상태"),
    
    /**
     * 만료 상태
     * API 키의 유효 기간이 만료된 상태입니다.
     * 자동으로 설정되며, 키 갱신이 필요합니다.
     * 만료된 키는 모든 API 호출이 차단됩니다.
     */
    EXPIRED("EXPIRED", "만료", "사용 기간이 만료된 상태"),
    
    /**
     * 정지 상태
     * 정책 위반이나 보안 문제로 API 키가 정지된 상태입니다.
     * 관리자 승인 없이는 재활성화할 수 없습니다.
     * 심각한 위반 시에만 설정됩니다.
     */
    SUSPENDED("SUSPENDED", "정지", "정책 위반 등으로 정지된 상태"),
    
    /**
     * 폐기 상태
     * API 키의 사용 권한이 영구적으로 취소된 상태입니다.
     * 재발급이 필요하며, 기존 키는 복구할 수 없습니다.
     * 보안상의 이유나 정책 변경으로 설정됩니다.
     */
    REVOKED("REVOKED", "폐기", "사용 권한이 취소된 상태"),
    
    /**
     * 대기 상태
     * API 키 발급 요청이 제출되었으나 아직 승인되지 않은 상태입니다.
     * 승인 전까지는 모든 API 호출이 차단됩니다.
     * 관리자 검토 후 승인/거부 결정이 내려집니다.
     */
    PENDING("PENDING", "대기", "승인 대기 중인 상태");

    private final String code;
    private final String displayName;
    private final String description;

    ApiKeyStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드로부터 ApiKeyStatus 찾기
     */
    public static ApiKeyStatus fromCode(String code) {
        for (ApiKeyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ApiKeyStatus code: " + code);
    }

    /**
     * 표시명으로부터 ApiKeyStatus 찾기
     */
    public static ApiKeyStatus fromDisplayName(String displayName) {
        for (ApiKeyStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ApiKeyStatus display name: " + displayName);
    }

    /**
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 사용 가능한 상태인지 확인
     */
    public boolean isUsable() {
        return this == ACTIVE || this == PENDING;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", code, displayName);
    }
}
