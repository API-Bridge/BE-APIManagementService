package org.example.APIManagementSvc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiToken;
import org.example.APIManagementSvc.service.SgisTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SGIS 토큰 관리 컨트롤러
 * 
 * 통계청 SGIS API 토큰의 수동 관리를 위한 REST API를 제공
 * 관리자가 토큰 상태를 확인하고 필요 시 수동으로 갱신할 수 있는 기능
 * 
 * 컨트롤러 책임:
 * - HTTP 요청/응답 처리
 * - 토큰 정보 응답 시 민감한 정보 마스킹
 * - HTTP 상태 코드 설정
 * - API 문서화 (Swagger)
 * - 요청 로깅
 * 
 * 비즈니스 로직은 SgisTokenService에 위임
 * 
 * 보안 고려사항:
 * - 액세스 토큰은 마스킹하여 응답
 * - 관리자 권한 확인 (현재는 구현하지 않음, 향후 보안 강화 시 추가)
 * 
 * @author API Bridge Team
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/sgis/tokens")
@RequiredArgsConstructor
@Tag(name = "SGIS Token Management", description = "SGIS API 토큰 관리 API")
public class SgisTokenController {

    private final SgisTokenService sgisTokenService;

    /**
     * SGIS 토큰 상태 조회
     * 
     * 현재 저장된 SGIS 토큰의 상태를 조회하여 반환
     * 토큰 존재 여부, 만료 시간, 유효 여부 등의 정보를 제공
     * 
     * 응답 정보:
     * - 토큰 존재 여부
     * - 토큰 ID (있는 경우)
     * - 마스킹된 액세스 토큰 (보안상 일부만 표시)
     * - 토큰 타입 (예: Bearer)
     * - 만료 시간
     * - 유효 여부
     * - 생성/수정 시간
     * 
     * @return ResponseEntity<Map<String, Object>> 토큰 상태 정보 (200 OK)
     */
    @Operation(summary = "SGIS 토큰 상태 조회", 
               description = "현재 저장된 SGIS 토큰의 상태를 조회합니다. 민감한 정보는 마스킹되어 반환됩니다.")
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getTokenStatus() {
        log.info("Getting SGIS token status");
        
        Map<String, Object> response = new HashMap<>();
        
        Optional<ApiToken> tokenOpt = sgisTokenService.getTokenStatus();
        
        if (tokenOpt.isPresent()) {
            ApiToken token = tokenOpt.get();
            boolean isValid = sgisTokenService.isTokenValid();
            boolean isExpiringWithin30Minutes = sgisTokenService.isTokenExpiringWithin(30);
            
            response.put("exists", true);
            response.put("tokenId", token.getTokenId());
            response.put("accessToken", maskToken(token.getAccessToken()));
            response.put("tokenType", token.getTokenType());
            response.put("expiresAt", token.getExpiresAt());
            response.put("isValid", isValid);
            response.put("isExpiringWithin30Minutes", isExpiringWithin30Minutes);
            response.put("createdAt", token.getCreatedAt());
            response.put("updatedAt", token.getUpdatedAt());
            response.put("credentialId", token.getCredential().getCredentialId());
            response.put("organizationName", token.getCredential().getOrganizationName());
            
            log.info("SGIS token status retrieved - Valid: {}, Expires at: {}", isValid, token.getExpiresAt());
        } else {
            response.put("exists", false);
            response.put("message", "SGIS 토큰이 존재하지 않습니다. 토큰을 발급해주세요.");
            
            log.info("No SGIS token found");
        }
        
        response.put("currentTime", LocalDateTime.now());
        return ResponseEntity.ok(response);
    }

    /**
     * SGIS 토큰 수동 발급
     * 
     * 관리자가 수동으로 새로운 SGIS 토큰을 발급
     * 기존 토큰이 있는 경우 업데이트하고, 없는 경우 새로 생성
     * 
     * 사용 시나리오:
     * - 토큰이 만료되었을 때 즉시 갱신
     * - 스케줄러 대기 없이 토큰이 필요한 경우
     * - 토큰 발급 테스트 목적
     * 
     * @return ResponseEntity<Map<String, Object>> 발급된 토큰 정보 (201 Created)
     */
    @Operation(summary = "SGIS 토큰 수동 발급", 
               description = "새로운 SGIS 토큰을 수동으로 발급합니다. 기존 토큰이 있는 경우 업데이트됩니다.")
    @PostMapping("/issue")
    public ResponseEntity<Map<String, Object>> issueToken() {
        log.info("Manual SGIS token issuance requested");
        
        try {
            ApiToken newToken = sgisTokenService.issueToken();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "SGIS 토큰이 성공적으로 발급되었습니다.");
            response.put("tokenId", newToken.getTokenId());
            response.put("accessToken", maskToken(newToken.getAccessToken()));
            response.put("tokenType", newToken.getTokenType());
            response.put("expiresAt", newToken.getExpiresAt());
            response.put("issuedAt", LocalDateTime.now());
            response.put("credentialId", newToken.getCredential().getCredentialId());
            
            log.info("SGIS token issued successfully - Token ID: {}, Expires at: {}", 
                    newToken.getTokenId(), newToken.getExpiresAt());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalStateException e) {
            log.error("SGIS token issuance failed due to credential issue: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "credential_error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (RuntimeException e) {
            log.error("SGIS token issuance failed due to API error: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "api_error");
            errorResponse.put("message", "SGIS API 호출 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * SGIS 토큰 강제 갱신
     * 
     * 현재 토큰의 만료 여부와 관계없이 새로운 토큰을 강제로 발급
     * 기존 토큰을 새 토큰으로 교체
     * 
     * 사용 시나리오:
     * - 토큰에 문제가 있어 강제로 갱신이 필요한 경우
     * - 보안상 토큰을 주기적으로 교체하고 싶은 경우
     * - 테스트 또는 디버깅 목적
     * 
     * @return ResponseEntity<Map<String, Object>> 갱신된 토큰 정보 (200 OK)
     */
    @Operation(summary = "SGIS 토큰 강제 갱신", 
               description = "현재 토큰의 만료 여부와 관계없이 새로운 SGIS 토큰을 강제로 발급합니다.")
    @PutMapping("/refresh")
    public ResponseEntity<Map<String, Object>> forceRefreshToken() {
        log.info("Force refresh SGIS token requested");
        
        try {
            ApiToken refreshedToken = sgisTokenService.forceRefreshToken();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "SGIS 토큰이 성공적으로 갱신되었습니다.");
            response.put("tokenId", refreshedToken.getTokenId());
            response.put("accessToken", maskToken(refreshedToken.getAccessToken()));
            response.put("tokenType", refreshedToken.getTokenType());
            response.put("expiresAt", refreshedToken.getExpiresAt());
            response.put("refreshedAt", LocalDateTime.now());
            response.put("credentialId", refreshedToken.getCredential().getCredentialId());
            
            log.info("SGIS token refreshed successfully - Token ID: {}, Expires at: {}", 
                    refreshedToken.getTokenId(), refreshedToken.getExpiresAt());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalStateException e) {
            log.error("SGIS token refresh failed due to credential issue: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "credential_error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            
        } catch (RuntimeException e) {
            log.error("SGIS token refresh failed due to API error: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "api_error");
            errorResponse.put("message", "SGIS API 호출 중 오류가 발생했습니다: " + e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * SGIS 토큰 유효성 검증
     * 
     * 현재 저장된 SGIS 토큰이 유효한지 확인
     * 토큰 존재 여부와 만료 여부를 종합적으로 판단
     * 
     * @return ResponseEntity<Map<String, Object>> 토큰 유효성 정보 (200 OK)
     */
    @Operation(summary = "SGIS 토큰 유효성 검증", 
               description = "현재 저장된 SGIS 토큰이 유효한지 확인합니다.")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken() {
        log.info("SGIS token validation requested");
        
        boolean isValid = sgisTokenService.isTokenValid();
        boolean isExpiringWithin30Minutes = sgisTokenService.isTokenExpiringWithin(30);
        
        Map<String, Object> response = new HashMap<>();
        response.put("isValid", isValid);
        response.put("isExpiringWithin30Minutes", isExpiringWithin30Minutes);
        response.put("validatedAt", LocalDateTime.now());
        
        if (isValid) {
            response.put("message", "SGIS 토큰이 유효합니다.");
        } else {
            response.put("message", "SGIS 토큰이 만료되었거나 존재하지 않습니다.");
        }
        
        if (isExpiringWithin30Minutes && isValid) {
            response.put("warning", "토큰이 30분 이내에 만료됩니다. 갱신을 고려해주세요.");
        }
        
        log.info("SGIS token validation result - Valid: {}, Expiring within 30min: {}", 
                isValid, isExpiringWithin30Minutes);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 액세스 토큰 마스킹 처리
     * 
     * 보안상 전체 토큰을 노출하지 않고 일부만 표시
     * 앞 8자리와 뒤 4자리만 표시하고 나머지는 '*'로 처리
     * 
     * @param token 마스킹할 토큰 문자열
     * @return String 마스킹된 토큰 문자열
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "****";
        }
        
        String start = token.substring(0, 8);
        String end = token.substring(token.length() - 4);
        int maskLength = token.length() - 12;
        String mask = "*".repeat(maskLength);
        
        return start + mask + end;
    }
}