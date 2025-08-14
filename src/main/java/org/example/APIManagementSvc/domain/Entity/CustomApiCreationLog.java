package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 커스텀 API 생성 로그 엔티티
 * 사용자가 커스텀 API를 생성할 때의 정보를 기록합니다.
 */
@Entity
@Table(name = "custom_api_creation_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomApiCreationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "custom_api_name", nullable = false, length = 255)
    private String customApiName;

    @Column(name = "combined_apis", nullable = false, columnDefinition = "TEXT")
    private String combinedApis; // JSON 형태로 저장

    @Column(name = "byok_used", nullable = false)
    private Boolean byokUsed;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        deleted = false;
    }
}
