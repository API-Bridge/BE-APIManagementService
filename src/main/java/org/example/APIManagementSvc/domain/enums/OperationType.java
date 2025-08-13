package org.example.APIManagementSvc.domain.enums;

/**
 * API 관리 작업 타입 Enum
 */
public enum OperationType {
    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    ACTIVATE("활성화"),
    DEACTIVATE("비활성화"),
    STATUS_CHANGE("상태 변경"),
    CONFIG_CHANGE("설정 변경");

    private final String description;

    OperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드로 작업 타입 찾기
     */
    public static OperationType fromDescription(String description) {
        for (OperationType type : values()) {
            if (type.description.equals(description)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", name(), description);
    }
}
