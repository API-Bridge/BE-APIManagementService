package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 외부 API 메타데이터 엔티티
 * external_api 테이블에 매핑
 */
@Entity
@Table(name = "external_api", indexes = {
    @Index(name = "idx_api_domain", columnList = "api_domain"),
    @Index(name = "idx_api_owner", columnList = "api_owner"),
    @Index(name = "idx_api_keyword", columnList = "api_keyword"),
    @Index(name = "idx_deleted", columnList = "deleted")
})
@Getter
@Setter
public class ExternalApi {

    /** 외부 API의 고유 식별자 */
    @Id
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** API의 이름 */
    @Column(name = "api_name", nullable = false, length = 255)
    private String apiName;

    /** API의 URL 주소 */
    @Column(name = "api_url", nullable = false, length = 500)
    private String apiUrl;

    /** API 발급처 */
    @Column(name = "api_issuer", nullable = false, length = 255)
    private String apiIssuer;

    /** API를 추가한 사용자 ID */
    @Column(name = "api_owner", length = 36)
    private String apiOwner;

    /** API 분류 도메인 (AI 자동 분류 결과) */
    @Enumerated(EnumType.STRING)
    @Column(name = "api_domain", nullable = false, length = 255)
    private ApiDomain apiDomain;

    /** API 세부분류용 키워드 (AI 자동 분류 결과) */
    @Enumerated(EnumType.STRING)
    @Column(name = "api_keyword", nullable = false, length = 255)
    private ApiKeyword apiKeyword;

    /** API의 HTTP 메소드 */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /** API에 대한 상세 설명 */
    @Column(name = "api_description", columnDefinition = "TEXT")
    private String apiDescription;

    /** API 유효성 상태 */
    @Column(name = "api_effectiveness", nullable = false)
    private Boolean apiEffectiveness = true;

    /** 생성 일시 */
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** Soft Delete를 위한 삭제 플래그 */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    // === 연관 관계 ===
    // MSA 환경에서 외래키 제약조건 및 연관관계 제거
    // API 파라미터는 별도 서비스 레이어에서 apiId로 조회

    // === 비즈니스 로직 메서드 ===

    /**
     * API 등록 시 기본 정보 설정
     */
    public void initializeApi(String apiId, String apiName, String apiUrl, String apiIssuer, 
                            String apiOwner, String httpMethod, String apiDescription) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.apiUrl = apiUrl;
        this.apiIssuer = apiIssuer;
        this.apiOwner = apiOwner;
        this.httpMethod = httpMethod;
        this.apiDescription = apiDescription;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.apiEffectiveness = true;
        this.deleted = false;
    }

    /**
     * AI 분류 결과 설정
     */
    public void setAiClassification(ApiDomain domain, ApiKeyword keyword) {
        this.apiDomain = domain;
        this.apiKeyword = keyword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * API 유효성 상태 업데이트
     */
    public void updateEffectiveness(Boolean effectiveness) {
        this.apiEffectiveness = effectiveness;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * API 정보 업데이트
     */
    public void updateApiInfo(String apiName, String apiUrl, String apiDescription) {
        this.apiName = apiName;
        this.apiUrl = apiUrl;
        this.apiDescription = apiDescription;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * API 분류 업데이트
     */
    public void updateClassification(ApiDomain domain, ApiKeyword keyword) {
        this.apiDomain = domain;
        this.apiKeyword = keyword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * API가 유효한지 확인
     */
    public boolean isValid() {
        return apiEffectiveness && !deleted;
    }

    /**
     * API 소유자 확인
     */
    public boolean isOwnedBy(String userId) {
        return apiOwner != null && apiOwner.equals(userId);
    }

    /**
     * AI 분류 결과가 신뢰할 수 있는지 확인
     */
    public boolean isClassificationReliable() {
        return true; // Removed classificationConfidence and classificationPrompt
    }

    /**
     * API 정보 요약
     */
    public String getSummary() {
        return String.format("%s (%s) - %s", apiName, apiIssuer, apiDomain);
    }
}
