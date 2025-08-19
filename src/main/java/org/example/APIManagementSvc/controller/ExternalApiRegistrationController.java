package org.example.APIManagementSvc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegistrationRequest;
import org.example.APIManagementSvc.dto.externalapi.ExternalApiRegistrationResponse;
import org.example.APIManagementSvc.service.ExternalApiRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 🔥 API 등록 컨트롤러
 * 관리자가 API를 등록할 수 있는 엔드포인트 제공
 */
@Slf4j
@RestController
@RequestMapping("/api-registration")
@RequiredArgsConstructor
public class ExternalApiRegistrationController {

    private final ExternalApiRegistrationService externalApiRegistrationService;

    /**
     * 🔥 API 등록 엔드포인트
     * 관리자가 API 기본 정보와 파라미터를 입력하면 AI 분류 후 저장
     * 
     * @param request API 등록 요청 정보
     * @return 등록 완료된 API 정보와 AI 분류 결과
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<ExternalApiRegistrationResponse>> registerApi(
            @Valid @RequestBody ExternalApiRegistrationRequest request) {
        
        log.info("API 등록 요청 수신: {}", request.getApiName());
        
        try {
            // API 등록 서비스 호출
            ExternalApiRegistrationResponse response = externalApiRegistrationService.registerApi(request);
            
            log.info("API 등록 성공: {} (ID: {})", response.getApiName(), response.getApiId());
            
            return ResponseEntity.ok(ApiResponse.success(response, "API가 성공적으로 등록되었습니다"));
            
        } catch (Exception e) {
            log.error("API 등록 실패: {}", request.getApiName(), e);
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("API 등록에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 🔥 API 등록 상태 확인 엔드포인트
     * 서비스 상태 및 등록 가능 여부 확인
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<String>> getRegistrationStatus() {
        log.info("API 등록 서비스 상태 확인 요청");
        
        return ResponseEntity.ok(ApiResponse.success(
            "API 등록 서비스가 정상적으로 동작 중입니다. AI 분류 및 파라미터 관리 기능을 사용할 수 있습니다.",
            "서비스 상태 확인 완료"
        ));
    }

    /**
     * 🔥 API 등록 가이드 엔드포인트
     * API 등록 시 필요한 정보와 형식 안내
     */
    @GetMapping("/guide")
    public ResponseEntity<ApiResponse<String>> getRegistrationGuide() {
        log.info("API 등록 가이드 요청");
        
        String guide = """
            🔥 API 등록 가이드
            
            📋 필수 입력 항목:
            - apiName: API 이름 (최대 255자)
            - apiUrl: API URL 주소 (최대 500자)
            - apiIssuer: API 발급처 (최대 255자)
            - apiOwner: API 소유자 ID (최대 36자)
            - httpMethod: HTTP 메소드 (GET, POST, PUT, DELETE 등)
            - parameters: API 파라미터 목록
            
            📝 파라미터 정보:
            - paramName: 파라미터 이름
            - paramType: 데이터 타입 (STRING, INTEGER, DECIMAL 등)
            - isRequired: 필수 여부
            - paramDescription: 파라미터 설명
            - additionalFields: 동적 추가 필드 (JSON)
            
            🤖 AI 자동 분류:
            - API 이름, 설명, URL, 파라미터를 분석하여 자동으로 도메인과 키워드 분류
            - 분류 신뢰도와 함께 결과 제공
            
            💾 저장 위치:
            - external_api 테이블: API 기본 정보 및 AI 분류 결과
            - api_parameter 테이블: API 파라미터 정보 (동적 필드 포함)
            """;
        
        return ResponseEntity.ok(ApiResponse.success(guide, "API 등록 가이드"));
    }
}
