package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "api_credentials")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiCredential {

    @Id
    @Column(name = "credential_id", length = 100)
    private String credentialId;

    @Column(name = "organization_name", nullable = false)
    private String organizationName;

    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    @Column(name = "secret_key", length = 500)
    private String secretKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CredentialStatus status = CredentialStatus.ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExternalApiSpec> apiSpecs;

    @OneToOne(mappedBy = "credential", cascade = CascadeType.ALL)
    private ApiToken token;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum CredentialStatus {
        ACTIVE, INACTIVE, EXPIRED
    }
}