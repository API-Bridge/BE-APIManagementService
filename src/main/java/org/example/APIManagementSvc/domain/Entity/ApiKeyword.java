package org.example.APIManagementSvc.domain.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "api_keywords")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_id")
    private Integer keywordId;

    @Column(name = "keyword_name", nullable = false, unique = true, length = 100)
    private String keywordName;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", nullable = false)
    private ApiDomain domain;

    @OneToMany(mappedBy = "keyword", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExternalApiSpec> apiSpecs;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}