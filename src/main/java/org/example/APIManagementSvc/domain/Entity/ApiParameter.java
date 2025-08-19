package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * API 파라미터 정보 엔티티
 * api_parameter 테이블에 매핑
 * 
 * 중요: MSA 환경에서 외래키 제약조건을 사용하지 않음
 * shared_schema.sql의 정책에 따라 애플리케이션 레벨에서 일관성 관리
 */
@Entity
@Table(name = "api_parameter", indexes = {
    @Index(name = "idx_api_id", columnList = "api_id"),
    @Index(name = "idx_deleted", columnList = "deleted"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
public class ApiParameter {

    /** 파라미터 정보 고유 식별자 */
    @Id
    @Column(name = "parameter_id", nullable = false, length = 36)
    private String parameterId;

    /** 
     * 이 파라미터가 속한 API의 ID 
     * 주의: MSA 환경에서 외래키 제약조건 없이 관리
     */
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

    /** 파라미터에 대한 설명 */
    @Column(name = "param_description", columnDefinition = "TEXT")
    private String paramDescription;

    /** 
     * 🔥 동적 추가 필드를 JSON으로 저장
     * API마다 다른 파라미터 구조를 유연하게 저장 가능
     */
    @Column(name = "additional_fields", columnDefinition = "JSON")
    private String additionalFields;

    /** 생성 일시 */
    @Column(name = "created_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /** 수정 일시 */
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /** 삭제 여부 (Soft Delete) */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    // === JSON 필드 처리 메서드 ===

    /**
     * JSON을 Map으로 변환하여 동적 추가 필드를 조회합니다.
     * @return Map<String, Object> 형태의 추가 필드 정보
     */
    @Transient
    public Map<String, Object> getAdditionalFieldsMap() {
        if (additionalFields == null || additionalFields.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(additionalFields, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /**
     * Map을 JSON 문자열로 변환하여 additionalFields에 저장합니다.
     * @param fields 저장할 추가 필드 Map
     */
    public void setAdditionalFieldsMap(Map<String, Object> fields) {
        if (fields == null || fields.isEmpty()) {
            this.additionalFields = null;
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            this.additionalFields = mapper.writeValueAsString(fields);
        } catch (Exception e) {
            this.additionalFields = "{}";
        }
    }

    /**
     * 특정 추가 필드 값을 조회합니다.
     * @param key 필드 키
     * @return 필드 값 또는 null
     */
    @Transient
    public Object getAdditionalField(String key) {
        return getAdditionalFieldsMap().get(key);
    }

    /**
     * 특정 추가 필드를 설정합니다.
     * @param key 필드 키
     * @param value 필드 값
     */
    @Transient
    public void setAdditionalField(String key, Object value) {
        Map<String, Object> fields = getAdditionalFieldsMap();
        fields.put(key, value);
        setAdditionalFieldsMap(fields);
    }

    /**
     * 엔티티 저장 전 호출
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.deleted == null) {
            this.deleted = false;
        }
        if (this.isRequired == null) {
            this.isRequired = false;
        }
    }

    /**
     * 엔티티 업데이트 전 호출
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
