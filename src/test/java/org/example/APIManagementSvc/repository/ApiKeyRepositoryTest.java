package org.example.APIManagementSvc.repository;

import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ApiKeyRepository 통합 테스트")
class ApiKeyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private ApiKey testApiKey1;
    private ApiKey testApiKey2;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 생성
        testApiKey1 = ApiKey.builder()
                .keyId("test-key-001")
                .organizationName("테스트 조직 1")
                .apiServiceName("TEST_API_1")
                .apiKey("test-api-key-12345")
                .secretKey("test-secret-key-67890")
                .contactEmail("test1@example.com")
                .contactPhone("010-1234-5678")
                .dailyLimit(1000)
                .monthlyLimit(30000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();

        testApiKey2 = ApiKey.builder()
                .keyId("test-key-002")
                .organizationName("테스트 조직 2")
                .apiServiceName("TEST_API_2")
                .apiKey("test-api-key-67890")
                .secretKey("test-secret-key-12345")
                .contactEmail("test2@example.com")
                .contactPhone("010-8765-4321")
                .dailyLimit(2000)
                .monthlyLimit(60000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();

        // 데이터베이스에 저장
        entityManager.persistAndFlush(testApiKey1);
        entityManager.persistAndFlush(testApiKey2);
        entityManager.clear();
    }

    @Test
    @DisplayName("API 키 저장 및 조회")
    void saveAndFindApiKey() {
        // given
        ApiKey newApiKey = ApiKey.builder()
                .keyId("new-key-001")
                .organizationName("새로운 조직")
                .apiServiceName("NEW_API")
                .apiKey("new-api-key")
                .secretKey("new-secret-key")
                .contactEmail("new@example.com")
                .contactPhone("010-1111-1111")
                .dailyLimit(500)
                .monthlyLimit(15000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();

        // when
        ApiKey savedApiKey = apiKeyRepository.save(newApiKey);
        Optional<ApiKey> foundApiKey = apiKeyRepository.findById("new-key-001");

        // then
        assertThat(savedApiKey).isNotNull();
        assertThat(savedApiKey.getKeyId()).isEqualTo("new-key-001");
        assertThat(foundApiKey).isPresent();
        assertThat(foundApiKey.get().getOrganizationName()).isEqualTo("새로운 조직");
    }

    @Test
    @DisplayName("기본 JPA 메서드 테스트")
    void basicJpaMethods() {
        // findAll
        List<ApiKey> allKeys = apiKeyRepository.findAll();
        assertThat(allKeys).hasSize(2);

        // findById
        Optional<ApiKey> foundKey = apiKeyRepository.findById("test-key-001");
        assertThat(foundKey).isPresent();
        assertThat(foundKey.get().getKeyId()).isEqualTo("test-key-001");

        // existsById
        boolean exists = apiKeyRepository.existsById("test-key-001");
        assertThat(exists).isTrue();

        // count
        long count = apiKeyRepository.count();
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("API 키 수정")
    void updateApiKey() {
        // given
        testApiKey1.setDescription("수정된 설명");
        testApiKey1.setDailyLimit(1500);

        // when
        ApiKey updatedApiKey = apiKeyRepository.save(testApiKey1);
        Optional<ApiKey> foundApiKey = apiKeyRepository.findById("test-key-001");

        // then
        assertThat(updatedApiKey).isNotNull();
        assertThat(updatedApiKey.getDescription()).isEqualTo("수정된 설명");
        assertThat(updatedApiKey.getDailyLimit()).isEqualTo(1500);
        assertThat(foundApiKey).isPresent();
        assertThat(foundApiKey.get().getDescription()).isEqualTo("수정된 설명");
    }

    @Test
    @DisplayName("API 키 삭제")
    void deleteApiKey() {
        // given
        String keyId = "test-key-001";

        // when
        apiKeyRepository.deleteById(keyId);
        Optional<ApiKey> foundApiKey = apiKeyRepository.findById(keyId);

        // then
        assertThat(foundApiKey).isEmpty();
    }

    @Test
    @DisplayName("기본 CRUD 작업 테스트")
    void basicCrudOperations() {
        // Create
        ApiKey newApiKey = ApiKey.builder()
                .keyId("crud-test-key")
                .organizationName("CRUD 테스트 조직")
                .apiServiceName("CRUD_TEST_API")
                .apiKey("crud-test-api-key")
                .secretKey("crud-test-secret-key")
                .contactEmail("crud@example.com")
                .contactPhone("010-5555-5555")
                .dailyLimit(500)
                .monthlyLimit(15000)
                .currentDailyUsage(0)
                .currentMonthlyUsage(0)
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1))
                .status(ApiKeyStatus.ACTIVE)
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(newApiKey);
        assertThat(savedApiKey).isNotNull();
        assertThat(savedApiKey.getKeyId()).isEqualTo("crud-test-key");

        // Read
        Optional<ApiKey> foundApiKey = apiKeyRepository.findById("crud-test-key");
        assertThat(foundApiKey).isPresent();
        assertThat(foundApiKey.get().getOrganizationName()).isEqualTo("CRUD 테스트 조직");

        // Update
        savedApiKey.setDescription("수정된 설명");
        ApiKey updatedApiKey = apiKeyRepository.save(savedApiKey);
        assertThat(updatedApiKey.getDescription()).isEqualTo("수정된 설명");

        // Delete
        apiKeyRepository.deleteById("crud-test-key");
        Optional<ApiKey> deletedApiKey = apiKeyRepository.findById("crud-test-key");
        assertThat(deletedApiKey).isEmpty();
    }
}
