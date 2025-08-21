package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.annotation.RateLimit;
import org.example.APIManagementSvc.domain.Entity.ApiKey;
import org.example.APIManagementSvc.domain.enums.ApiKeyStatus;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.common.PageResponse;
import org.example.APIManagementSvc.dto.apikey.ApiKeyRegistrationRequest;
import org.example.APIManagementSvc.dto.apikey.ApiKeyUpdateRequest;
import org.example.APIManagementSvc.service.ApiKeyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * API 키 관리 컨트롤러
 * 외부 API 서비스의 API 키를 관리하는 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * API 키 등록
     */
    @PostMapping("/register")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiKey>> registerApiKey(@Valid @RequestBody ApiKeyRegistrationRequest request) {
        log.info("API 키 등록 요청: Organization={}, Service={}", request.getOrganizationName(), request.getApiServiceName());
        
        try {
            ApiKey registeredApiKey = apiKeyService.registerApiKey(
                request.getOrganizationName(),
                request.getOrganizationCode(),
                request.getContactEmail(),
                request.getContactPhone(),
                request.getApiServiceName(),
                request.getApiServiceUrl(),
                request.getApiKey(),
                request.getSecretKey(),
                request.getDailyLimit(),
                request.getMonthlyLimit(),
                request.getExpiresAt(),
                request.getDescription(),
                request.getRequestedApis()
            );
            
            return ResponseEntity.ok(ApiResponse.success(registeredApiKey, "API 키가 성공적으로 등록되었습니다."));
            
        } catch (Exception e) {
            log.error("API 키 등록 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 등록 실패: " + e.getMessage()));
        }
    }

    /**
     * API 키 조회
     */
    @GetMapping("/{keyId}")
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiKey>> getApiKey(@PathVariable String keyId) {
        log.debug("API 키 조회: {}", keyId);
        
        try {
            ApiKey apiKey = apiKeyService.getApiKey(keyId)
                    .orElseThrow(() -> new IllegalArgumentException("API key not found: " + keyId));
            
            return ResponseEntity.ok(ApiResponse.success(apiKey));
            
        } catch (Exception e) {
            log.error("API 키 조회 실패: {} - {}", keyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 기관명으로 API 키 조회
     */
    @GetMapping("/organization/{organizationName}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeysByOrganization(@PathVariable String organizationName) {
        log.debug("기관별 API 키 조회: {}", organizationName);

        try {
            List<ApiKey> apiKeys = apiKeyService.getApiKeysByOrganization(organizationName);
            return ResponseEntity.ok(ApiResponse.success(apiKeys));

        } catch (Exception e) {
            log.error("기관별 API 키 조회 실패: {} - {}", organizationName, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("기관별 API 키 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * API 서비스별 API 키 조회
     */
    @GetMapping("/service/{apiServiceName}")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiKey>>> getApiKeysByService(@PathVariable String apiServiceName) {
        log.debug("서비스별 API 키 조회: {}", apiServiceName);
        
        try {
            List<ApiKey> apiKeys = apiKeyService.getApiKeysByService(apiServiceName);
            return ResponseEntity.ok(ApiResponse.success(apiKeys));
            
        } catch (Exception e) {
            log.error("서비스별 API 키 조회 실패: {} - {}", apiServiceName, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("서비스별 API 키 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * 모든 API 키 페이징 조회
     */
    @GetMapping
    @RateLimit(value = 30, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<PageResponse<ApiKey>>> getAllApiKeys(Pageable pageable) {
        log.debug("모든 API 키 페이징 조회");
        
        try {
            Page<ApiKey> apiKeyPage = apiKeyService.getApiKeysWithPaging(pageable);
            
            PageResponse<ApiKey> pageResponse = PageResponse.<ApiKey>builder()
                    .content(apiKeyPage.getContent())
                    .totalElements(apiKeyPage.getTotalElements())
                    .totalPages(apiKeyPage.getTotalPages())
                    .pageNumber(pageable.getPageNumber())
                    .pageSize(pageable.getPageSize())
                    .build();
            
            return ResponseEntity.ok(ApiResponse.success(pageResponse));
            
        } catch (Exception e) {
            log.error("모든 API 키 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("모든 API 키 조회 실패: " + e.getMessage()));
        }
    }

    /**
     * API 키 수정
     */
    @PutMapping("/{keyId}")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiKey>> updateApiKey(@PathVariable String keyId, 
                                                          @Valid @RequestBody ApiKeyUpdateRequest request) {
        log.info("API 키 수정: {}", keyId);
        
        try {
            ApiKey updateData = convertToEntity(request);
            ApiKey updatedApiKey = apiKeyService.updateApiKey(keyId, updateData);
            
            return ResponseEntity.ok(ApiResponse.success(updatedApiKey, "API 키가 성공적으로 수정되었습니다."));
            
        } catch (Exception e) {
            log.error("API 키 수정 실패: {} - {}", keyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 수정 실패: " + e.getMessage()));
        }
    }

    /**
     * API 키 상태 변경
     */
    @PatchMapping("/{keyId}/status")
    @RateLimit(value = 10, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<ApiKey>> updateApiKeyStatus(@PathVariable String keyId, 
                                                               @RequestParam ApiKeyStatus status) {
        log.info("API 키 상태 변경: {} -> {}", keyId, status);
        
        try {
            ApiKey updatedApiKey = apiKeyService.updateApiKeyStatus(keyId, status);
            
            return ResponseEntity.ok(ApiResponse.success(updatedApiKey, "API 키 상태가 성공적으로 변경되었습니다."));
            
        } catch (Exception e) {
            log.error("API 키 상태 변경 실패: {} - {}", keyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 상태 변경 실패: " + e.getMessage()));
        }
    }

    /**
     * API 키 삭제 (소프트 삭제)
     */
    @DeleteMapping("/{keyId}")
    @RateLimit(value = 5, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<String>> deleteApiKey(@PathVariable String keyId) {
        log.info("API 키 삭제: {}", keyId);
        
        try {
            apiKeyService.deleteApiKey(keyId);
            
            return ResponseEntity.ok(ApiResponse.success("API 키가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("API 키 삭제 실패: {} - {}", keyId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API 키 삭제 실패: " + e.getMessage()));
        }
    }

    /**
     * 활성 API 키 조회 -> 페이지네이션 필요
     */
    @GetMapping("/active")
    @RateLimit(value = 20, timeUnit = TimeUnit.MINUTES, keyType = RateLimit.KeyType.IP_ADDRESS)
    public ResponseEntity<ApiResponse<List<ApiKey>>> getActiveApiKeys() {
        log.debug("활성 API 키 조회");
        
        try {
            List<ApiKey> activeApiKeys = apiKeyService.getAllActiveApiKeys();
            return ResponseEntity.ok(ApiResponse.success(activeApiKeys));
            
        } catch (Exception e) {
            log.error("활성 API 키 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("활성 API 키 조회 실패: " + e.getMessage()));
        }
    }

    // === DTO 변환 메서드 ===

    private ApiKey convertToEntity(ApiKeyUpdateRequest request) {
        return ApiKey.builder()
                .organizationName(request.getOrganizationName())
                .organizationCode(request.getOrganizationCode())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .apiServiceUrl(request.getApiServiceUrl())
                .dailyLimit(request.getDailyLimit())
                .monthlyLimit(request.getMonthlyLimit())
                .expiresAt(request.getExpiresAt())
                .description(request.getDescription())
                .requestedApis(request.getRequestedApis())
                .build();
    }

}
