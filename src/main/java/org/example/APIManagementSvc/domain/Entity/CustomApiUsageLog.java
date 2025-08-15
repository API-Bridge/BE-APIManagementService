package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 커스텀 API 사용 로그 엔티티
 * 사용자가 커스텀 API를 사용할 때의 정보를 기록합니다.
 */
@Entity
@Table(name = "custom_api_usage_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomApiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "request_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime requestTime;

    @Column(name = "custom_api_name", nullable = false, length = 255)
    private String customApiName;

    @Column(name = "used_public_apis", nullable = false, columnDefinition = "TEXT")
    private String usedPublicApis; // JSON 형태로 저장

    @Column(name = "total_response_time", nullable = false)
    private Long totalResponseTime; // milliseconds

    @Column(name = "response_data_size")
    private Long responseDataSize; // bytes

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        requestTime = LocalDateTime.now();
        deleted = false;
    }
}
