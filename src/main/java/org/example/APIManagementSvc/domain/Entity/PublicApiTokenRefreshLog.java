package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.APIManagementSvc.domain.enums.RefreshStatus;

import java.time.LocalDateTime;

/**
 * 공공데이터 API 토큰 갱신 로그 엔티티
 * 공공데이터 API의 토큰 갱신 정보를 기록합니다.
 */
@Entity
@Table(name = "public_api_token_refresh_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PublicApiTokenRefreshLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "provider_name", nullable = false, length = 255)
    private String providerName;

    @Column(name = "refresh_time", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refreshTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "refresh_status", nullable = false, length = 50)
    private RefreshStatus refreshStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        refreshTime = LocalDateTime.now();
        deleted = false;
    }
}
