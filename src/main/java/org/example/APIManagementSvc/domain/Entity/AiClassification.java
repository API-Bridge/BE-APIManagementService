package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * AI 분류 결과 엔티티
 * AI 자동 분류 결과를 저장하는 엔티티
 */
@Entity
@Table(name = "ai_classifications", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_domain", columnList = "classified_domain"),
    @Index(name = "idx_keyword", columnList = "classified_keyword"),
    @Index(name = "idx_classified_at", columnList = "classified_at")
})
@Getter
@Setter
public class AiClassification {

    /** Classification ID */
    @Id
    @Column(name = "classification_id", nullable = false, length = 36)
    private String classificationId;

    /** API ID */
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** 분류된 도메인 */
    @Enumerated(EnumType.STRING)
    @Column(name = "classified_domain", length = 50)
    private ApiDomain classifiedDomain;

    /** 분류된 키워드 */
    @Enumerated(EnumType.STRING)
    @Column(name = "classified_keyword", length = 50)
    private ApiKeyword classifiedKeyword;

    /** 분류 일시 */
    @Column(name = "classified_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime classifiedAt;

    /** Soft Delete를 위한 삭제 플래그 */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    // === 비즈니스 로직 메서드 ===

    /**
     * 분류 완료 처리
     */
    public void completeClassification(ApiDomain domain, ApiKeyword keyword) {
        this.classifiedDomain = domain;
        this.classifiedKeyword = keyword;
        this.classifiedAt = LocalDateTime.now();
    }

    /**
     * 분류 유효성 확인
     */
    public boolean isValid() {
        return classifiedDomain != null && 
               classifiedKeyword != null && 
               classifiedAt != null;
    }
}
