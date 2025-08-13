package org.example.APIManagementSvc.domain.enums;

/**
 * 성능 메트릭 타입 Enum
 */
public enum MetricType {
    RESPONSE_TIME("응답 시간"),
    CPU_USAGE("CPU 사용률"),
    MEMORY_USAGE("메모리 사용량"),
    DISK_IO("디스크 I/O"),
    SUCCESS_RATE("성공률"),
    THROUGHPUT("처리량"),
    ERROR_RATE("오류율"),
    AVAILABILITY("가용성");

    private final String description;

    MetricType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 코드로 메트릭 타입 찾기
     */
    public static MetricType fromDescription(String description) {
        for (MetricType type : values()) {
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
