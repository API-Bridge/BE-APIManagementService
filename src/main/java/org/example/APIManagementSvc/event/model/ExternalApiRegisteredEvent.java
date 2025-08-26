package org.example.APIManagementSvc.event.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 외부 API 등록 이벤트
 * 새로운 외부 API가 시스템에 등록되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiRegisteredEvent extends BaseEvent {
    
    private String apiId;
    private String apiName;
    private String apiUrl;
    private String apiDescription;
    private String category;
    private String version;
    private String registeredBy;
    
    public ExternalApiRegisteredEvent(String apiId, String apiName, String apiUrl, 
                                    String apiDescription, String category, 
                                    String version, String registeredBy) {
        super("EXTERNAL_API_REGISTERED");
        this.apiId = apiId;
        this.apiName = apiName;
        this.apiUrl = apiUrl;
        this.apiDescription = apiDescription;
        this.category = category;
        this.version = version;
        this.registeredBy = registeredBy;
    }
}