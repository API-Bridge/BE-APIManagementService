package org.example.APIManagementSvc.domain.enums;

/**
 * 토큰 갱신 상태 Enum
 */
public enum RefreshStatus {
    SUCCESS("성공"),
    FAILED("실패"),
    IN_PROGRESS("진행중"),
    EXPIRED("만료됨"),
    MANUAL_REFRESH("수동갱신");

    private final String description;

    RefreshStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드로 상태 찾기
     */
    public static RefreshStatus fromDescription(String description) {
        for (RefreshStatus status : values()) {
            if (status.description.equals(description)) {
                return status;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
