package org.example.APIManagementSvc.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.APIManagementSvc.domain.Entity.ApiParameter;
import org.example.APIManagementSvc.dto.common.ApiResponse;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterRegisterRequest;
import org.example.APIManagementSvc.dto.externalapi.ApiParameterResponse;
import org.example.APIManagementSvc.service.ApiParameterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API 파라미터 관리 컨트롤러
 * API 파라미터의 조회, 수정, 삭제 기능 제공
 * (파라미터 등록은 API 등록 시 함께 처리됨)
 */
@Slf4j
@RestController
@RequestMapping("/external-apis/{apiId}/parameters")
@RequiredArgsConstructor
public class ApiParameterController {

    private final ApiParameterService apiParameterService;

    /**
     * 파라미터 조회 (ID로)
     * GET /external-apis/{apiId}/parameters/{parameterId}
     */
    @GetMapping("/{parameterId}")
    public ResponseEntity<ApiResponse<ApiParameterResponse>> getParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId) {
        
        log.info("Fetching parameter: {} for API: {}", parameterId, apiId);
        
        try {
            var parameter = apiParameterService.getParameterById(parameterId);
            
            if (parameter.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("파라미터를 찾을 수 없습니다: " + parameterId));
            }
            
            // API ID 검증
            if (!apiId.equals(parameter.get().getApiId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("API ID가 일치하지 않습니다."));
            }
            
            ApiParameterResponse response = convertToResponse(parameter.get());
            return ResponseEntity.ok(ApiResponse.success(response));
            
        } catch (Exception e) {
            log.error("Failed to fetch parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API의 모든 파라미터 조회
     * GET /external-apis/{apiId}/parameters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiParameterResponse>>> getParameters(@PathVariable String apiId) {
        log.info("Fetching all parameters for API: {}", apiId);
        
        try {
            List<ApiParameter> parameters = apiParameterService.getParametersByApiId(apiId);
            
            List<ApiParameterResponse> responses = parameters.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Failed to fetch parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 필수 파라미터만 조회
     * GET /external-apis/{apiId}/parameters/required
     */
    @GetMapping("/required")
    public ResponseEntity<ApiResponse<List<ApiParameterResponse>>> getRequiredParameters(@PathVariable String apiId) {
        log.info("Fetching required parameters for API: {}", apiId);
        
        try {
            List<ApiParameter> parameters = apiParameterService.getRequiredParametersByApiId(apiId);
            
            List<ApiParameterResponse> responses = parameters.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(responses));
            
        } catch (Exception e) {
            log.error("Failed to fetch required parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("필수 파라미터 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 수정 또는 추가
     * PUT /external-apis/{apiId}/parameters/{parameterId}
     * 
     * 기존 파라미터가 있으면 수정, 없으면 새로 추가
     */
    @PutMapping("/{parameterId}")
    public ResponseEntity<ApiResponse<ApiParameterResponse>> updateOrCreateParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId,
            @Valid @RequestBody ApiParameterRegisterRequest request) {
        
        log.info("Updating or creating parameter: {} for API: {}", parameterId, apiId);
        
        try {
            // 기존 파라미터 조회
            var existingParameter = apiParameterService.getParameterById(parameterId);
            
            ApiParameter resultParameter;
            
            if (existingParameter.isPresent()) {
                // 기존 파라미터가 있으면 수정
                log.info("Updating existing parameter: {}", parameterId);
                
                // API ID 검증
                if (!apiId.equals(existingParameter.get().getApiId())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.error("API ID가 일치하지 않습니다."));
                }
                
                // 파라미터 수정
                ApiParameter updateData = convertToEntity(request, apiId);
                resultParameter = apiParameterService.updateParameter(parameterId, updateData);
                
            } else {
                // 기존 파라미터가 없으면 새로 생성
                log.info("Creating new parameter with ID: {}", parameterId);
                
                // 파라미터 생성
                ApiParameter newParameter = convertToEntity(request, apiId);
                newParameter.setParameterId(parameterId);
                resultParameter = apiParameterService.saveParameter(newParameter);
            }
            
            ApiParameterResponse response = convertToResponse(resultParameter);
            String message = existingParameter.isPresent() ? "파라미터가 성공적으로 수정되었습니다." : "파라미터가 성공적으로 추가되었습니다.";
            
            return ResponseEntity.ok(ApiResponse.success(response, message));
            
        } catch (Exception e) {
            log.error("Failed to update or create parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 수정/추가에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 삭제 (Soft Delete)
     * DELETE /external-apis/{apiId}/parameters/{parameterId}
     */
    @DeleteMapping("/{parameterId}")
    public ResponseEntity<ApiResponse<String>> deleteParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId) {
        
        log.info("Deleting parameter: {} for API: {}", parameterId, apiId);
        
        try {
            // 기존 파라미터 조회
            var existingParameter = apiParameterService.getParameterById(parameterId);
            
            if (existingParameter.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("파라미터를 찾을 수 없습니다: " + parameterId));
            }
            
            // API ID 검증
            if (!apiId.equals(existingParameter.get().getApiId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("API ID가 일치하지 않습니다."));
            }
            
            // 파라미터 삭제
            apiParameterService.deleteParameter(parameterId);
            
            return ResponseEntity.ok(ApiResponse.success("파라미터가 성공적으로 삭제되었습니다.", "파라미터가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to delete parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 하드 삭제 (완전 삭제)
     * DELETE /external-apis/{apiId}/parameters/{parameterId}/hard
     */
    @DeleteMapping("/{parameterId}/hard")
    public ResponseEntity<ApiResponse<String>> hardDeleteParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId) {
        
        log.info("Hard deleting parameter: {} for API: {}", parameterId, apiId);
        
        try {
            // 기존 파라미터 조회
            var existingParameter = apiParameterService.getParameterById(parameterId);
            
            if (existingParameter.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("파라미터를 찾을 수 없습니다: " + parameterId));
            }
            
            // API ID 검증
            if (!apiId.equals(existingParameter.get().getApiId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error("API ID가 일치하지 않습니다."));
            }
            
            // 파라미터 하드 삭제
            apiParameterService.hardDeleteParameter(parameterId);
            
            return ResponseEntity.ok(ApiResponse.success("파라미터가 완전히 삭제되었습니다.", "파라미터가 완전히 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to hard delete parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 완전 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    // === Helper Methods ===

    /**
     * CreateRequest를 Entity로 변환
     */
    private ApiParameter convertToEntity(ApiParameterRegisterRequest request, String apiId) {
        ApiParameter parameter = new ApiParameter();
        parameter.setApiId(apiId);
        parameter.setParamName(request.getParamName());
        parameter.setParamType(request.getParamType());
        parameter.setIsRequired(request.getIsRequired());
        parameter.setDefaultValue(request.getDefaultValue());
        parameter.setDeleted(false);
        return parameter;
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private ApiParameterResponse convertToResponse(ApiParameter parameter) {
        return ApiParameterResponse.builder()
                .parameterId(parameter.getParameterId())
                .apiId(parameter.getApiId())
                .paramName(parameter.getParamName())
                .paramType(parameter.getParamType())
                .isRequired(parameter.getIsRequired())
                .defaultValue(parameter.getDefaultValue())

                .createdAt(parameter.getCreatedAt())
                .updatedAt(parameter.getUpdatedAt())
                .build();
    }
}

