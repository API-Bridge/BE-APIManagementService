package org.example.APIManagementSvc.domain.enums;

import lombok.Getter;

/**
 * Rate Limit 상태를 나타내는 Enum
 */
@Getter
public enum RateLimitStatus {
    
    ALLOWED("ALLOWED", "허용됨", "API 호출이 허용됨"),
    WARNING("WARNING", "경고", "API 호출 제한에 근접함"),
    EXCEEDED("EXCEEDED", "초과", "API 호출 제한을 초과함"),
    BLOCKED("BLOCKED", "차단됨", "API 호출이 차단됨"),
    RESET("RESET", "리셋", "Rate limit이 리셋됨");

    private final String code;
    private final String displayName;
    private final String description;

    RateLimitStatus(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * 코드로부터 RateLimitStatus 찾기
     */
    public static RateLimitStatus fromCode(String code) {
        for (RateLimitStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RateLimitStatus code: " + code);
    }

    /**
     * 표시명으로부터 RateLimitStatus 찾기
     */
    public static RateLimitStatus fromDisplayName(String displayName) {
        for (RateLimitStatus status : values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RateLimitStatus display name: " + displayName);
    }

    /**
     * 한국어 이름으로부터 RateLimitStatus 찾기
     */
    public static RateLimitStatus fromKoreanName(String koreanName) {
        for (RateLimitStatus status : values()) {
            if (status.getDisplayName().equals(koreanName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown RateLimitStatus Korean name: " + koreanName);
    }

    /**
     * 모든 코드 목록 반환
     */
    public static String[] getAllCodes() {
        RateLimitStatus[] values = values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].getCode();
        }
        return codes;
    }

    /**
     * 모든 표시명 목록 반환
     */
    public static String[] getAllDisplayNames() {
        RateLimitStatus[] values = values();
        String[] displayNames = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            displayNames[i] = values[i].getDisplayName();
        }
        return displayNames;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", code, displayName);
    }
}
