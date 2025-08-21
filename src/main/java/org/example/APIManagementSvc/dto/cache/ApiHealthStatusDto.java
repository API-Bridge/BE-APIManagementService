package org.example.APIManagementSvc.dto.cache;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * API 헬스체크 상태 DTO
 * 캐시에 저장되는 API 헬스 상태 정보
 */
@Getter
@Setter
@Builder
public class ApiHealthStatusDto {

    /** API ID */
    private String apiId;

    /** 헬스 상태 (HEALTHY, UNHEALTHY, UNREACHABLE, ERROR, FAILED) */
    private String status;

    /** 응답 시간 (밀리초) */
    private long responseTime;

    /** HTTP 상태 코드 */
    private Integer httpStatus;

    /** 오류 메시지 */
    private String errorMessage;

    /** 헬스체크 수행 시간 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime checkedAt;

    /**
     * 헬스체크가 성공했는지 확인
     */
    public boolean isHealthy() {
        return "HEALTHY".equals(status);
    }

    /**
     * 헬스체크가 실패했는지 확인
     */
    public boolean isUnhealthy() {
        return !isHealthy();
    }

    /**
     * 연결이 불가능한 상태인지 확인
     */
    public boolean isUnreachable() {
        return "UNREACHABLE".equals(status);
    }

    /**
     * 오류가 발생한 상태인지 확인
     */
    public boolean hasError() {
        return "ERROR".equals(status) || "FAILED".equals(status);
    }

    /**
     * 응답 시간이 정상 범위인지 확인 (1초 이하)
     */
    public boolean hasGoodResponseTime() {
        return responseTime > 0 && responseTime <= 1000;
    }

    /**
     * 응답 시간이 느린지 확인 (3초 초과)
     */
    public boolean hasSlowResponseTime() {
        return responseTime > 3000;
    }

    /**
     * 상태 요약 반환
     */
    public String getStatusSummary() {
        if (isHealthy()) {
            return String.format("정상 (응답시간: %dms)", responseTime);
        } else if (isUnreachable()) {
            return "연결 불가";
        } else if (hasError()) {
            return String.format("오류: %s", errorMessage != null ? errorMessage : "알 수 없는 오류");
        } else {
            return String.format("비정상 (HTTP %d)", httpStatus != null ? httpStatus : 0);
        }
    }
}
