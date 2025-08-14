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
 * API 파라미터의 등록, 조회, 수정, 삭제 기능 제공
 */
@Slf4j
@RestController
@RequestMapping("/external-apis/{apiId}/parameters")
@RequiredArgsConstructor
public class ApiParameterController {

    private final ApiParameterService apiParameterService;

    /**
     * 새로운 API 파라미터 등록
     * @param apiId API ID
     * @param request 파라미터 등록 요청 데이터
     * @return 등록된 파라미터 정보
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiParameterResponse>> registerParameter(
            @PathVariable String apiId,
            @Valid @RequestBody ApiParameterRegisterRequest request) {
        log.info("Registering new parameter for API {}: {}", apiId, request.getParamName());
        
        ApiParameterResponse response = apiParameterService.saveParameter(request, apiId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "파라미터가 성공적으로 등록되었습니다."));
    }

    /**
     * 파라미터 조회 (ID로)
     * GET /api/v1/external-apis/{apiId}/parameters/{parameterId}
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
     * GET /api/v1/external-apis/{apiId}/parameters
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
     * GET /api/v1/external-apis/{apiId}/parameters/required
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
     * 파라미터 수정
     * PUT /api/v1/external-apis/{apiId}/parameters/{parameterId}
     */
    @PutMapping("/{parameterId}")
    public ResponseEntity<ApiResponse<ApiParameterResponse>> updateParameter(
            @PathVariable String apiId,
            @PathVariable String parameterId,
            @Valid @RequestBody ApiParameterRegisterRequest request) {
        
        log.info("Updating parameter: {} for API: {}", parameterId, apiId);
        
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
            
            // 파라미터 수정
            ApiParameter updateData = convertToEntity(request, apiId);
            ApiParameter updatedParameter = apiParameterService.updateParameter(parameterId, updateData);
            
            ApiParameterResponse response = convertToResponse(updatedParameter);
            return ResponseEntity.ok(ApiResponse.success(response, "파라미터가 성공적으로 수정되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to update parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 수정에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 삭제 (Soft Delete)
     * DELETE /api/v1/external-apis/{apiId}/parameters/{parameterId}
     */
    @DeleteMapping("/{parameterId}")
    public ResponseEntity<ApiResponse<Void>> deleteParameter(
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
            
            return ResponseEntity.ok(ApiResponse.success(null, "파라미터가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to delete parameter: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * API의 모든 파라미터 삭제
     * DELETE /api/v1/external-apis/{apiId}/parameters
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteAllParameters(@PathVariable String apiId) {
        log.info("Deleting all parameters for API: {}", apiId);
        
        try {
            apiParameterService.deleteAllParametersByApiId(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(null, "모든 파라미터가 성공적으로 삭제되었습니다."));
            
        } catch (Exception e) {
            log.error("Failed to delete all parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 일괄 삭제에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 파라미터 통계 조회
     * GET /api/v1/external-apis/{apiId}/parameters/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Object>> getParameterStatistics(@PathVariable String apiId) {
        log.info("Fetching parameter statistics for API: {}", apiId);
        
        try {
            var statistics = apiParameterService.getParameterStatistics(apiId);
            
            return ResponseEntity.ok(ApiResponse.success(statistics));
            
        } catch (Exception e) {
            log.error("Failed to fetch parameter statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파라미터 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    // ===== Private Helper Methods =====

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


                .paramName(parameter.getParamName())
                .paramType(parameter.getParamType())
                .isRequired(parameter.getIsRequired())
                .defaultValue(parameter.getDefaultValue())





                .build();
    }
}

