package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "api_domains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiDomain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "domain_id")
    private Integer domainId;

    @Column(name = "domain_name", nullable = false, unique = true, length = 100)
    private String domainName;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiKeyword> keywords;

    @OneToMany(mappedBy = "domain", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExternalApiSpec> apiSpecs;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}