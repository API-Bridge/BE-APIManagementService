package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_parameters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiParameter {

    @Id
    @Column(name = "parameter_id", length = 36)
    private String parameterId;

    @Column(name = "param_name", nullable = false)
    private String paramName;

    @Column(name = "param_type", nullable = false, length = 50)
    private String paramType;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = false;

    @Lob
    @Column(name = "param_description")
    private String paramDescription;

    @Lob
    @Column(name = "default_value")
    private String defaultValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_fields", columnDefinition = "JSON")
    private String additionalFields;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id", nullable = false)
    private ExternalApiSpec apiSpec;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}