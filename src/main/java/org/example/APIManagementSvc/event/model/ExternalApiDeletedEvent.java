package org.example.APIManagementSvc.event.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 외부 API 삭제 이벤트
 * 외부 API가 시스템에서 삭제되었을 때 발행되는 이벤트
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExternalApiDeletedEvent extends BaseEvent {
    
    private String apiId;
    private String apiName;
    private String apiUrl;
    private String deletedBy;
    private String deletionReason;
    
    public ExternalApiDeletedEvent(String apiId, String apiName, String apiUrl,
                                 String deletedBy, String deletionReason) {
        super("EXTERNAL_API_DELETED");
        this.apiId = apiId;
        this.apiName = apiName;
        this.apiUrl = apiUrl;
        this.deletedBy = deletedBy;
        this.deletionReason = deletionReason;
    }
}