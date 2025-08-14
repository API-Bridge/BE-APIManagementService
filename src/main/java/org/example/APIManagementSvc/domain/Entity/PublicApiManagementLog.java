package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 공공데이터 API 관리 로그 엔티티
 * 관리자가 공공데이터 API를 관리할 때의 정보를 기록합니다.
 */
@Entity
@Table(name = "public_api_management_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicApiManagementLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "api_name", nullable = false, length = 255)
    private String apiName;

    @Column(name = "operation_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;

    @Column(name = "admin_id", nullable = false, length = 36)
    private String adminId;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType; // CREATE, UPDATE, DELETE

    @Column(name = "operation_reason", columnDefinition = "TEXT")
    private String operationReason;

    @Column(name = "health_check_passed")
    private Boolean healthCheckPassed; // CREATE/UPDATE 시에만

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        operationTime = LocalDateTime.now();
        deleted = false;
    }
}
