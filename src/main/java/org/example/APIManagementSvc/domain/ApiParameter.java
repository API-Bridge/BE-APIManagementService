package org.example.APIManagementSvc.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API 파라미터 정보 엔티티
 * api_parameter 테이블에 매핑
 */
@Entity
@Table(name = "api_parameter", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id")
})
@Getter
@Setter
public class ApiParameter {

    /** 파라미터 정보 고유 식별자 */
    @Id
    @Column(name = "parameter_id", nullable = false, length = 36)
    private String parameterId;

    /** 이 파라미터가 속한 API의 ID */
    @Column(name = "api_id", nullable = false, length = 36)
    private String apiId;

    /** 파라미터 이름 */
    @Column(name = "param_name", nullable = false, length = 255)
    private String paramName;

    /** 파라미터의 데이터 타입 */
    @Column(name = "param_type", nullable = false, length = 50)
    private String paramType;

    /** 해당 파라미터가 필수인지 여부 */
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    /** 파라미터의 기본값 */
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    /** 생성 일시 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Soft Delete를 위한 삭제 플래그 */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    // === 연관 관계 ===

    /** 외부 API */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", insertable = false, updatable = false)
    private ExternalApi externalApi;

    // === 비즈니스 로직 메서드 ===

    /**
     * 파라미터 정보 업데이트
     */
    public void updateParameterInfo(String paramName, String paramType, Boolean isRequired, String defaultValue) {
        this.paramName = paramName;
        this.paramType = paramType;
        this.isRequired = isRequired;
        this.defaultValue = defaultValue;
    }

    /**
     * 필수 파라미터인지 확인
     */
    public boolean isRequired() {
        return isRequired != null && isRequired;
    }

    /**
     * 기본값이 있는지 확인
     */
    public boolean hasDefaultValue() {
        return defaultValue != null && !defaultValue.trim().isEmpty();
    }

    /**
     * 파라미터 정보 요약
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(paramName).append(" (").append(paramType).append(")");
        
        if (isRequired()) {
            summary.append(" [필수]");
        }
        
        if (hasDefaultValue()) {
            summary.append(" 기본값: ").append(defaultValue);
        }
        
        return summary.toString();
    }

    /**
     * 파라미터 타입 검증
     */
    public boolean isValidType() {
        if (paramType == null) return false;
        
        String type = paramType.toLowerCase();
        return type.equals("string") || 
               type.equals("integer") || 
               type.equals("number") || 
               type.equals("boolean") || 
               type.equals("array") || 
               type.equals("object");
    }

    /**
     * 파라미터 값 검증
     */
    public boolean validateValue(String value) {
        if (value == null) {
            return !isRequired(); // 필수가 아니면 null 허용
        }
        
        if (paramType == null) return true;
        
        String type = paramType.toLowerCase();
        switch (type) {
            case "string":
                return true; // 문자열은 모든 값 허용
            case "integer":
                try {
                    Integer.parseInt(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "number":
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            case "boolean":
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
            default:
                return true; // 기타 타입은 일단 허용
        }
    }
}
