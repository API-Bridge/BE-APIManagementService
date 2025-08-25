package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "external_api_specs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalApiSpec {

    @Id
    @Column(name = "api_id", length = 36)
    private String apiId;

    @Column(name = "api_name", nullable = false)
    private String apiName;

    @Lob
    @Column(name = "api_description")
    private String apiDescription;

    @Column(name = "api_issuer", nullable = false)
    private String apiIssuer;

    @Column(name = "api_url", nullable = false, length = 500)
    private String apiUrl;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    private ApiCredential credential;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private ApiDomain domain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private ApiKeyword keyword;

    @OneToMany(mappedBy = "apiSpec", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiParameter> parameters;

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