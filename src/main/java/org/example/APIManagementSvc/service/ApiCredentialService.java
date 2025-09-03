package org.example.APIManagementSvc.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiCredential;
import org.example.APIManagementSvc.dto.request.ApiCredentialRequestDto;
import org.example.APIManagementSvc.dto.request.ApiCredentialUpdateDto;
import org.example.APIManagementSvc.dto.response.ApiCredentialResponseDto;
import org.example.APIManagementSvc.repository.ApiCredentialRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * API 자격증명 관리 서비스
 * 
 * 외부 API 호출에 필요한 인증 정보를 관리하는 비즈니스 로직을 제공
 * 보안이 중요한 정보이므로 접근 제어와 감사 로깅을 강화
 * 
 * 주요 기능:
 * - 자격증명 CRUD 작업
 * - 자격증명 상태 관리 (ACTIVE, INACTIVE, EXPIRED)
 * - 중복 자격증명 ID 검증
 * - 민감한 정보 보호 (API Key, Secret Key)
 * 
 * 보안 고려사항:
 * - API Key/Secret Key는 암호화 저장 권장 (현재는 평문)
 * - 접근 로그 기록으로 감사 추적 가능
 * - 응답 시 민감한 정보 마스킹 처리
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApiCredentialService {

    private final ApiCredentialRepository apiCredentialRepository;

    /**
     * 새로운 자격증명 등록
     * 
     * 외부 API 제공자로부터 발급받은 인증 정보를 시스템에 등록
     * 중복된 자격증명 ID가 있는지 검증 후 저장
     * 
     * @param requestDto 등록할 자격증명 정보
     * @return 등록된 자격증명 정보 (민감한 정보 마스킹)
     * @throws IllegalArgumentException 중복된 자격증명 ID인 경우
     */
    @Transactional
    public ApiCredentialResponseDto createCredential(ApiCredentialRequestDto requestDto) {
        log.info("Creating API credential for organization: {}", requestDto.getOrganizationName());
        
        // 중복 자격증명 ID 검증
        if (apiCredentialRepository.existsById(requestDto.getCredentialId())) {
            throw new IllegalArgumentException("이미 존재하는 자격증명 ID입니다: " + requestDto.getCredentialId());
        }

        // ApiCredential 엔티티 생성
        ApiCredential credential = ApiCredential.builder()
                .credentialId(requestDto.getCredentialId())
                .organizationName(requestDto.getOrganizationName())
                .apiKey(requestDto.getApiKey())
                .secretKey(requestDto.getSecretKey())
                .status(ApiCredential.CredentialStatus.ACTIVE)  // 기본값: ACTIVE
                .issuedAt(requestDto.getIssuedAt() != null ? requestDto.getIssuedAt() : LocalDateTime.now())
                .contactEmail(requestDto.getContactEmail())
                .build();

        ApiCredential savedCredential = apiCredentialRepository.save(credential);
        
        log.info("Successfully created API credential: {} for organization: {}", 
                savedCredential.getCredentialId(), savedCredential.getOrganizationName());

        return ApiCredentialResponseDto.from(savedCredential);
    }

    /**
     * 자격증명 단건 상세 조회
     * 
     * @param credentialId 조회할 자격증명 ID
     * @return 자격증명 상세 정보 (민감한 정보 마스킹)
     * @throws IllegalArgumentException 존재하지 않는 자격증명 ID인 경우
     */
    public ApiCredentialResponseDto getCredential(String credentialId) {
        log.info("Getting API credential: {}", credentialId);
        
        ApiCredential credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID입니다: " + credentialId));

        return ApiCredentialResponseDto.from(credential);
    }

    /**
     * 모든 자격증명 목록 조회 (페이징)
     * 
     * @param pageable 페이징 정보
     * @return 자격증명 목록 (페이징 적용)
     */
    public Page<ApiCredentialResponseDto> getAllCredentials(Pageable pageable) {
        log.info("Getting all API credentials with pagination");
        
        return apiCredentialRepository.findAll(pageable)
                .map(ApiCredentialResponseDto::from);
    }

    /**
     * 활성화된 자격증명만 조회
     * 
     * 현재 사용 가능한 자격증명만 필터링하여 조회
     * API 등록 시 선택 가능한 자격증명 목록으로 활용
     * 
     * @return 활성화된 자격증명 목록
     */
    public List<ApiCredentialResponseDto> getActiveCredentials() {
        log.info("Getting all active API credentials");
        
        List<ApiCredential> activeCredentials = apiCredentialRepository.findByStatus(ApiCredential.CredentialStatus.ACTIVE);
        
        return activeCredentials.stream()
                .map(ApiCredentialResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 자격증명 정보 수정
     * 
     * null이 아닌 필드만 업데이트하는 부분 수정 지원
     * 민감한 정보 변경 시 별도 로깅으로 감사 추적
     * 
     * @param credentialId 수정할 자격증명 ID
     * @param updateDto 수정할 정보
     * @return 수정된 자격증명 정보
     * @throws IllegalArgumentException 존재하지 않는 자격증명 ID인 경우
     */
    @Transactional
    public ApiCredentialResponseDto updateCredential(String credentialId, ApiCredentialUpdateDto updateDto) {
        log.info("Updating API credential: {}", credentialId);
        
        ApiCredential credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID입니다: " + credentialId));

        // 부분 업데이트: null이 아닌 필드만 수정
        if (updateDto.getOrganizationName() != null) {
            credential.setOrganizationName(updateDto.getOrganizationName());
        }
        
        if (updateDto.getApiKey() != null) {
            log.warn("API Key updated for credential: {}", credentialId);  // 보안 감사용 로그
            credential.setApiKey(updateDto.getApiKey());
        }
        
        if (updateDto.getSecretKey() != null) {
            log.warn("Secret Key updated for credential: {}", credentialId);  // 보안 감사용 로그
            credential.setSecretKey(updateDto.getSecretKey());
        }
        
        if (updateDto.getStatus() != null) {
            log.info("Status changed from {} to {} for credential: {}", 
                    credential.getStatus(), updateDto.getStatus(), credentialId);
            credential.setStatus(updateDto.getStatus());
        }
        
        if (updateDto.getIssuedAt() != null) {
            credential.setIssuedAt(updateDto.getIssuedAt());
        }
        
        if (updateDto.getContactEmail() != null) {
            credential.setContactEmail(updateDto.getContactEmail());
        }

        ApiCredential updatedCredential = apiCredentialRepository.save(credential);
        
        log.info("Successfully updated API credential: {}", credentialId);

        return ApiCredentialResponseDto.from(updatedCredential);
    }

    /**
     * 자격증명 삭제
     * 
     * 실제 데이터 삭제가 아닌 상태를 INACTIVE로 변경하는 소프트 삭제
     * 참조 무결성 보장과 감사 추적을 위한 안전한 삭제 방식
     * 
     * @param credentialId 삭제할 자격증명 ID
     * @throws IllegalArgumentException 존재하지 않는 자격증명 ID인 경우
     * @throws IllegalStateException 사용 중인 자격증명을 삭제하려는 경우
     */
    @Transactional
    public void deleteCredential(String credentialId) {
        log.info("Deleting API credential: {}", credentialId);
        
        ApiCredential credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID입니다: " + credentialId));

        // 사용 중인 자격증명인지 검증 (외부 API에서 참조하는지)
        if (credential.getApiSpecs() != null && !credential.getApiSpecs().isEmpty()) {
            throw new IllegalStateException("사용 중인 자격증명은 삭제할 수 없습니다. 먼저 연관된 API들을 삭제하거나 다른 자격증명으로 변경해주세요.");
        }

        // 소프트 삭제: 상태를 INACTIVE로 변경
        credential.setStatus(ApiCredential.CredentialStatus.INACTIVE);
        apiCredentialRepository.save(credential);
        
        log.warn("API credential soft deleted: {} for organization: {}", 
                credentialId, credential.getOrganizationName());
    }

    /**
     * 자격증명 하드 삭제
     * 
     * 실제 데이터베이스에서 완전히 제거하는 하드 삭제
     * 개발/테스트 환경에서만 사용하며, 운영 환경에서는 사용 금지
     * 
     * @param credentialId 완전 삭제할 자격증명 ID
     * @throws IllegalArgumentException 존재하지 않는 자격증명 ID인 경우
     * @throws IllegalStateException 사용 중인 자격증명을 삭제하려는 경우
     */
    @Transactional
    public void forceDeleteCredential(String credentialId) {
        log.warn("Force deleting API credential: {} - This is a dangerous operation!", credentialId);
        
        ApiCredential credential = apiCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자격증명 ID입니다: " + credentialId));

        // 사용 중인 자격증명인지 검증
        if (credential.getApiSpecs() != null && !credential.getApiSpecs().isEmpty()) {
            throw new IllegalStateException("사용 중인 자격증명은 삭제할 수 없습니다. 먼저 연관된 API들을 삭제해주세요.");
        }

        // 하드 삭제
        apiCredentialRepository.delete(credential);
        
        log.error("API credential permanently deleted: {} for organization: {} - THIS ACTION CANNOT BE UNDONE!", 
                credentialId, credential.getOrganizationName());
    }

    /**
     * 특정 상태의 자격증명 목록 조회
     * 
     * 만료된 자격증명 관리, 비활성화된 자격증명 정리 등에 활용
     * 
     * @param status 조회할 자격증명 상태
     * @return 해당 상태의 자격증명 목록
     */
    public List<ApiCredentialResponseDto> getCredentialsByStatus(ApiCredential.CredentialStatus status) {
        log.info("Getting API credentials by status: {}", status);
        
        List<ApiCredential> credentials = apiCredentialRepository.findByStatus(status);
        
        return credentials.stream()
                .map(ApiCredentialResponseDto::from)
                .collect(Collectors.toList());
    }
}