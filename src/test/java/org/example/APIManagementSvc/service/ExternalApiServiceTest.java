package org.example.APIManagementSvc.service;

import org.example.APIManagementSvc.domain.Entity.ExternalApi;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;
import org.example.APIManagementSvc.dto.externalapi.ApiStatisticsResponse;
import org.example.APIManagementSvc.repository.ExternalApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiService 테스트")
class ExternalApiServiceTest {

    @Mock
    private ExternalApiRepository externalApiRepository;

    @InjectMocks
    private ExternalApiService externalApiService;

    private ExternalApi testApi;
    private ExternalApi testApi2;

    @BeforeEach
    void setUp() {
        testApi = new ExternalApi();
        testApi.setApiId("test-api-001");
        testApi.setApiName("테스트 API");
        testApi.setApiUrl("https://api.test.com");
        testApi.setApiDescription("테스트용 API");
        testApi.setHttpMethod("GET");
        testApi.setApiIssuer("테스트 조직");
        testApi.setApiOwner("테스트 팀");
        testApi.setApiDomain(ApiDomain.TECHNOLOGY);
        testApi.setApiKeyword(ApiKeyword.API_DOCUMENT);
        testApi.setCreatedAt(LocalDateTime.now());
        testApi.setUpdatedAt(LocalDateTime.now());

        testApi2 = new ExternalApi();
        testApi2.setApiId("test-api-002");
        testApi2.setApiName("테스트 API 2");
        testApi2.setApiUrl("https://api2.test.com");
        testApi2.setApiDescription("테스트용 API 2");
        testApi2.setHttpMethod("POST");
        testApi2.setApiIssuer("테스트 조직 2");
        testApi2.setApiOwner("테스트 팀 2");
        testApi2.setApiDomain(ApiDomain.FINANCE);
        testApi2.setApiKeyword(ApiKeyword.ECONOMIC_INDICATOR);
        testApi2.setCreatedAt(LocalDateTime.now());
        testApi2.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("API 등록 성공")
    void registerApi_Success() {
        // given
        when(externalApiRepository.existsByApiName(anyString())).thenReturn(false);
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = externalApiService.registerApi(testApi);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiName()).isEqualTo("테스트 API");
        assertThat(result.getApiEffectiveness()).isTrue();
        assertThat(result.getDeleted()).isFalse();
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        
        verify(externalApiRepository).existsByApiName("테스트 API");
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("API 등록 실패 - 중복된 API 이름")
    void registerApi_DuplicateName() {
        // given
        when(externalApiRepository.existsByApiName(anyString())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
        
        verify(externalApiRepository).existsByApiName("테스트 API");
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 등록 실패 - 필수 필드 누락")
    void registerApi_MissingRequiredFields() {
        // given
        ExternalApi invalidApi = new ExternalApi();
        invalidApi.setApiName(""); // 빈 문자열

        // when & then
        assertThatThrownBy(() -> externalApiService.registerApi(invalidApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API name is required");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 등록 실패 - API 도메인 누락")
    void registerApi_MissingDomain() {
        // given
        testApi.setApiDomain(null);

        // when & then
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API domain is required");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 등록 실패 - API 키워드 누락")
    void registerApi_MissingKeyword() {
        // given
        testApi.setApiKeyword(null);

        // when & then
        assertThatThrownBy(() -> externalApiService.registerApi(testApi))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("API keyword is required");
        
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 등록 (인증 정보 포함) 성공")
    void registerApiWithAuth_Success() {
        // given
        when(externalApiRepository.existsByApiName(anyString())).thenReturn(false);
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        String apiKey = "test-api-key";
        String apiToken = "test-api-token";

        // when
        ExternalApi result = externalApiService.registerApiWithAuth(testApi, apiKey, apiToken);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiToken()).isEqualTo(apiToken);
        assertThat(result.getTokenExpiresAt()).isNotNull();
        
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("API 등록 (인증 정보 포함) - API 키만 제공")
    void registerApiWithAuth_ApiKeyOnly() {
        // given
        when(externalApiRepository.existsByApiName(anyString())).thenReturn(false);
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        String apiKey = "test-api-key";
        String apiToken = null;

        // when
        ExternalApi result = externalApiService.registerApiWithAuth(testApi, apiKey, apiToken);

        // then
        assertThat(result).isNotNull();
        // API 키는 별도로 관리되므로 ExternalApi 엔티티에는 설정되지 않음
        
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("페이징을 사용한 API 조회")
    void getApisWithPaging() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        List<ExternalApi> apiList = Arrays.asList(testApi, testApi2);
        Page<ExternalApi> page = new PageImpl<>(apiList, pageable, apiList.size());
        
        when(externalApiRepository.findValidApis(pageable)).thenReturn(page);

        // when
        Page<ExternalApi> result = externalApiService.getApisWithPaging(pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        verify(externalApiRepository).findValidApis(pageable);
    }

    @Test
    @DisplayName("API 검색")
    void searchApis() {
        // given
        String searchQuery = "테스트";
        List<ExternalApi> searchResults = Arrays.asList(testApi, testApi2);
        
        when(externalApiRepository.findBySearchTermAndDeletedFalse(searchQuery)).thenReturn(searchResults);

        // when
        List<ExternalApi> result = externalApiService.searchApis(searchQuery);

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(externalApiRepository).findBySearchTermAndDeletedFalse(searchQuery);
    }

    @Test
    @DisplayName("API 통계 조회")
    void getApiStatistics() {
        // given
        when(externalApiRepository.countByDeletedFalse()).thenReturn(10L);
        when(externalApiRepository.countByApiEffectivenessTrueAndDeletedFalse()).thenReturn(8L);

        // when
        ApiStatisticsResponse result = externalApiService.getApiStatistics();

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalApis()).isEqualTo(10);
        assertThat(result.getActiveApis()).isEqualTo(8);
        assertThat(result.getInactiveApis()).isEqualTo(2); // 10 - 8
        
        verify(externalApiRepository).countByDeletedFalse();
        verify(externalApiRepository).countByApiEffectivenessTrueAndDeletedFalse();
    }

    @Test
    @DisplayName("API ID로 조회 성공")
    void getApiById_Success() {
        // given
        String apiId = "test-api-001";
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.of(testApi));

        // when
        Optional<ExternalApi> result = externalApiService.getApiById(apiId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getApiId()).isEqualTo(apiId);
        
        verify(externalApiRepository).findByApiId(apiId);
    }

    @Test
    @DisplayName("API ID로 조회 실패 - 존재하지 않는 API")
    void getApiById_NotFound() {
        // given
        String apiId = "non-existent-api";
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.empty());

        // when
        Optional<ExternalApi> result = externalApiService.getApiById(apiId);

        // then
        assertThat(result).isEmpty();
        
        verify(externalApiRepository).findByApiId(apiId);
    }

    @Test
    @DisplayName("API 이름으로 조회 성공")
    void getApiByName_Success() {
        // given
        String apiName = "테스트 API";
        when(externalApiRepository.findByApiName(apiName)).thenReturn(Optional.of(testApi));

        // when
        Optional<ExternalApi> result = externalApiService.getApiByName(apiName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getApiName()).isEqualTo(apiName);
        
        verify(externalApiRepository).findByApiName(apiName);
    }

    @Test
    @DisplayName("모든 활성 API 조회")
    void getAllActiveApis() {
        // given
        List<ExternalApi> activeApis = Arrays.asList(testApi, testApi2);
        when(externalApiRepository.findByDeletedFalse()).thenReturn(activeApis);

        // when
        List<ExternalApi> result = externalApiService.getAllActiveApis();

        // then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        
        verify(externalApiRepository).findByDeletedFalse();
    }

    @Test
    @DisplayName("API 수정 성공")
    void updateApi_Success() {
        // given
        String apiId = "test-api-001";
        ExternalApi updateData = new ExternalApi();
        updateData.setApiName("수정된 API 이름");
        updateData.setApiDescription("수정된 설명");
        
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.of(testApi));
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = externalApiService.updateApi(apiId, updateData);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("API 수정 실패 - 존재하지 않는 API")
    void updateApi_NotFound() {
        // given
        String apiId = "non-existent-api";
        ExternalApi updateData = new ExternalApi();
        
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> externalApiService.updateApi(apiId, updateData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }

    @Test
    @DisplayName("API 삭제 (소프트 삭제) 성공")
    void deleteApi_Success() {
        // given
        String apiId = "test-api-001";
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.of(testApi));
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        externalApiService.deleteApi(apiId);

        // then
        assertThat(testApi.getDeleted()).isTrue();
        assertThat(testApi.getUpdatedAt()).isNotNull();
        
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("API 완전 삭제 (하드 삭제) 성공")
    void hardDeleteApi_Success() {
        // given
        String apiId = "test-api-001";
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.of(testApi));

        // when
        externalApiService.hardDeleteApi(apiId);

        // then
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository).delete(testApi);
    }

    @Test
    @DisplayName("API 유효성 업데이트 성공")
    void updateApiEffectiveness_Success() {
        // given
        String apiId = "test-api-001";
        boolean newEffectiveness = false;
        
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.of(testApi));
        when(externalApiRepository.save(any(ExternalApi.class))).thenReturn(testApi);

        // when
        ExternalApi result = externalApiService.updateApiEffectiveness(apiId, newEffectiveness);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getApiEffectiveness()).isEqualTo(newEffectiveness);
        assertThat(result.getUpdatedAt()).isNotNull();
        
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository).save(testApi);
    }

    @Test
    @DisplayName("API 유효성 업데이트 실패 - 존재하지 않는 API")
    void updateApiEffectiveness_NotFound() {
        // given
        String apiId = "non-existent-api";
        boolean newEffectiveness = false;
        
        when(externalApiRepository.findByApiId(apiId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> externalApiService.updateApiEffectiveness(apiId, newEffectiveness))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
        
        verify(externalApiRepository).findByApiId(apiId);
        verify(externalApiRepository, never()).save(any(ExternalApi.class));
    }
}

