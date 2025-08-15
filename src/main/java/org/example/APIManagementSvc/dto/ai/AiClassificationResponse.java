package org.example.APIManagementSvc.dto.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.example.APIManagementSvc.domain.enums.ApiDomain;
import org.example.APIManagementSvc.domain.enums.ApiKeyword;

import java.time.LocalDateTime;

/**
 * AI 분류 응답 DTO
 */
@Getter
@Setter
public class AiClassificationResponse {

    /** 분류 ID */
    private String classificationId;

    /** API ID */
    private String apiId;

    /** 분류된 도메인 */
    private ApiDomain classifiedDomain;

    /** 분류된 키워드 */
    private ApiKeyword classifiedKeyword;

    /** 분류 일시 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime classifiedAt;

    /** 분류 로그 */
    private String classificationLog;

    /** 분석된 텍스트 */
    private String analyzedText;

    /** 모델 버전 */
    private String modelVersion;

    /** 메타데이터 */
    private String metadata;
}


