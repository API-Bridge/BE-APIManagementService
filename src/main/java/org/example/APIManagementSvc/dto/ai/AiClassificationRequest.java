package org.example.APIManagementSvc.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * AI 분류 요청 DTO
 */
@Getter
@Setter
public class AiClassificationRequest {

    /** API ID */
    @NotBlank(message = "API ID는 필수입니다")
    private String apiId;

    /** API 이름 */
    @NotBlank(message = "API 이름은 필수입니다")
    private String apiName;

    /** API 설명 */
    private String apiDescription;

    /** API URL */
    private String apiUrl;
}

