package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiCredential;
import org.example.APIManagementSvc.dto.request.ApiCredentialRequestDto;
import org.example.APIManagementSvc.dto.request.ApiCredentialUpdateDto;
import org.example.APIManagementSvc.dto.response.ApiCredentialResponseDto;
import org.example.APIManagementSvc.service.ApiCredentialService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API 자격증명 관리 컨트롤러
 * 
 * 외부 API 호출에 필요한 인증 정보(API Key, Secret Key)를 관리하는 REST API
 * 보안이 중요한 정보이므로 접근 제어와 입력 검증을 강화
 * 
 * 컨트롤러 책임:
 * - HTTP 요청/응답 처리
 * - 입력 데이터 검증 (Bean Validation)
 * - HTTP 상태 코드 설정
 * - API 문서화 (Swagger)
 * - 요청 로깅
 * 
 * 비즈니스 로직은 ApiCredentialService에 위임
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/credentials")
@RequiredArgsConstructor
@Validated
@Tag(name = "API Credential Management", description = "API 자격증명 관리 API")
public class ApiCredentialController {

    private final ApiCredentialService apiCredentialService;

    /**
     * 새로운 자격증명 등록
     * 
     * 외부 API 제공자로부터 발급받은 인증 정보를 시스템에 등록
     * 입력 데이터 검증 후 서비스 레이어에 위임
     * 
     * @param requestDto 등록할 자격증명 정보
     * @return 등록된 자격증명 정보 (201 Created)
     */
    @Operation(summary = "자격증명 등록", 
               description = "새로운 API 자격증명을 등록합니다. API Key와 Secret Key는 암호화되어 저장됩니다.")
    @PostMapping
    public ResponseEntity<ApiCredentialResponseDto> createCredential(
            @Valid @RequestBody ApiCredentialRequestDto requestDto) {
        
        log.info("Creating API credential for organization: {}", requestDto.getOrganizationName());
        
        ApiCredentialResponseDto response = apiCredentialService.createCredential(requestDto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 자격증명 단건 조회
     * 
     * @param credentialId 조회할 자격증명 ID
     * @return 자격증명 상세 정보 (200 OK)
     */
    @Operation(summary = "자격증명 단건 조회", 
               description = "특정 자격증명의 상세 정보를 조회합니다. 민감한 정보는 마스킹되어 반환됩니다.")
    @GetMapping("/{credentialId}")
    public ResponseEntity<ApiCredentialResponseDto> getCredential(
            @Parameter(description = "자격증명 ID", required = true) 
            @PathVariable String credentialId) {
        
        log.info("Getting API credential: {}", credentialId);
        
        ApiCredentialResponseDto response = apiCredentialService.getCredential(credentialId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 모든 자격증명 목록 조회 (페이징)
     * 
     * @param pageable 페이징 정보 (기본: 20개씩)
     * @return 자격증명 목록 (200 OK)
     */
    @Operation(summary = "자격증명 목록 조회", 
               description = "등록된 모든 자격증명을 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<Page<ApiCredentialResponseDto>> getAllCredentials(
            @Parameter(description = "페이징 정보") 
            @PageableDefault(size = 20) Pageable pageable) {
        
        log.info("Getting all API credentials with pagination");
        
        Page<ApiCredentialResponseDto> response = apiCredentialService.getAllCredentials(pageable);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 활성화된 자격증명만 조회
     * 
     * API 등록 시 선택 가능한 자격증명 목록으로 활용
     * 
     * @return 활성화된 자격증명 목록 (200 OK)
     */
    @Operation(summary = "활성화된 자격증명 조회", 
               description = "현재 활성화된 자격증명만 조회합니다. API 등록 시 선택 옵션으로 활용됩니다.")
    @GetMapping("/active")
    public ResponseEntity<List<ApiCredentialResponseDto>> getActiveCredentials() {
        
        log.info("Getting all active API credentials");
        
        List<ApiCredentialResponseDto> response = apiCredentialService.getActiveCredentials();
        
        return ResponseEntity.ok(response);
    }

    /**
     * 특정 상태의 자격증명 조회
     * 
     * @param status 조회할 자격증명 상태 (ACTIVE, INACTIVE, EXPIRED)
     * @return 해당 상태의 자격증명 목록 (200 OK)
     */
    @Operation(summary = "상태별 자격증명 조회", 
               description = "특정 상태의 자격증명만 조회합니다. 만료된 자격증명 관리에 활용됩니다.")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApiCredentialResponseDto>> getCredentialsByStatus(
            @Parameter(description = "자격증명 상태", required = true) 
            @PathVariable ApiCredential.CredentialStatus status) {
        
        log.info("Getting API credentials by status: {}", status);
        
        List<ApiCredentialResponseDto> response = apiCredentialService.getCredentialsByStatus(status);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 자격증명 정보 수정
     * 
     * null이 아닌 필드만 부분 수정하는 PATCH 방식
     * 
     * @param credentialId 수정할 자격증명 ID
     * @param updateDto 수정할 정보
     * @return 수정된 자격증명 정보 (200 OK)
     */
    @Operation(summary = "자격증명 수정", 
               description = "기존 자격증명 정보를 수정합니다. null이 아닌 필드만 업데이트됩니다.")
    @PatchMapping("/{credentialId}")
    public ResponseEntity<ApiCredentialResponseDto> updateCredential(
            @Parameter(description = "수정할 자격증명 ID", required = true) 
            @PathVariable String credentialId,
            @Valid @RequestBody ApiCredentialUpdateDto updateDto) {
        
        log.info("Updating API credential: {}", credentialId);
        
        ApiCredentialResponseDto response = apiCredentialService.updateCredential(credentialId, updateDto);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 자격증명 소프트 삭제
     * 
     * 실제 데이터 삭제가 아닌 상태를 INACTIVE로 변경
     * 참조 무결성 보장과 감사 추적을 위한 안전한 삭제
     * 
     * @param credentialId 삭제할 자격증명 ID
     * @return 응답 없음 (204 No Content)
     */
    @Operation(summary = "자격증명 소프트 삭제", 
               description = "자격증명을 비활성화합니다. 실제 데이터는 삭제되지 않으며 감사 추적이 가능합니다.")
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<Void> deleteCredential(
            @Parameter(description = "삭제할 자격증명 ID", required = true) 
            @PathVariable String credentialId) {
        
        log.info("Soft deleting API credential: {}", credentialId);
        
        apiCredentialService.deleteCredential(credentialId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * 자격증명 하드 삭제
     * 
     * 실제 데이터베이스에서 완전히 제거하는 위험한 작업
     * 개발/테스트 환경에서만 사용하며, 운영 환경에서는 접근 제어 필요
     * 
     * @param credentialId 완전 삭제할 자격증명 ID
     * @return 응답 없음 (204 No Content)
     */
    @Operation(summary = "자격증명 하드 삭제", 
               description = "자격증명을 완전히 삭제합니다. 이 작업은 되돌릴 수 없으므로 주의가 필요합니다.")
    @DeleteMapping("/{credentialId}/force")
    public ResponseEntity<Void> forceDeleteCredential(
            @Parameter(description = "완전 삭제할 자격증명 ID", required = true) 
            @PathVariable String credentialId) {
        
        log.warn("Force deleting API credential: {} - This is a dangerous operation!", credentialId);
        
        apiCredentialService.forceDeleteCredential(credentialId);
        
        return ResponseEntity.noContent().build();
    }
}