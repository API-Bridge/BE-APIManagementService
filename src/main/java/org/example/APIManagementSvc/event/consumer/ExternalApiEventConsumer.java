package org.example.APIManagementSvc.event.consumer;

import org.example.APIManagementSvc.event.model.ExternalApiCallFailedEvent;
import org.example.APIManagementSvc.event.model.ExternalApiCallFailedEventWrapper;
import org.example.APIManagementSvc.event.model.ExternalApiDeletedEvent;
import org.example.APIManagementSvc.event.model.ExternalApiHealthCheckEvent;
import org.example.APIManagementSvc.event.model.ExternalApiRegisteredEvent;
import org.example.APIManagementSvc.service.ApiHealthCheckService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 외부 API 이벤트 컨슈머
 * external_api_events 토픽에서 외부 API 관련 이벤트를 구독하여 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalApiEventConsumer {

    private final ApiHealthCheckService apiHealthCheckService;

    /**
     * 외부 API 등록 이벤트 처리
     * 
     * @param event 외부 API 등록 이벤트
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param ack 수동 확인용 객체
     */
    @KafkaListener(
        topics = "external_api_events",
        containerFactory = "kafkaListenerContainerFactory",
        filter = "externalApiRegisteredEventFilter"
    )
    public void handleExternalApiRegistered(@Payload ExternalApiRegisteredEvent event,
                                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                          @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received External API Registered Event - ID: {}, Name: {}, URL: {}, Partition: {}, Offset: {}",
                    event.getApiId(), event.getApiName(), event.getApiUrl(), partition, offset);
            
            // 외부 API 등록 이벤트 처리 로직
            processApiRegistration(event);
            
            log.info("External API Registered Event processed successfully: {}", event.getApiId());
            
        } catch (Exception e) {
            log.error("Error processing External API Registered Event: {}", event, e);
            // 에러 처리 로직 (재시도, DLQ 전송 등)
        }
    }

    /**
     * 외부 API 삭제 이벤트 처리
     * 
     * @param event 외부 API 삭제 이벤트
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param ack 수동 확인용 객체
     */
    @KafkaListener(
        topics = "external_api_events",
        containerFactory = "kafkaListenerContainerFactory",
        filter = "externalApiDeletedEventFilter"
    )
    public void handleExternalApiDeleted(@Payload ExternalApiDeletedEvent event,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received External API Deleted Event - ID: {}, Name: {}, Reason: {}, Partition: {}, Offset: {}",
                    event.getApiId(), event.getApiName(), event.getDeletionReason(), partition, offset);
            
            // 외부 API 삭제 이벤트 처리 로직
            processApiDeletion(event);
            
            log.info("External API Deleted Event processed successfully: {}", event.getApiId());
            
        } catch (Exception e) {
            log.error("Error processing External API Deleted Event: {}", event, e);
            // 에러 처리 로직 (재시도, DLQ 전송 등)
        }
    }

    /**
     * 외부 API 헬스체크 이벤트 처리
     * 
     * @param event 외부 API 헬스체크 이벤트
     * @param partition 파티션 번호
     * @param offset 오프셋
     * @param ack 수동 확인용 객체
     */
    @KafkaListener(
        topics = "external_api_events",
        containerFactory = "kafkaListenerContainerFactory",
        filter = "externalApiHealthCheckEventFilter"
    )
    public void handleExternalApiHealthCheck(@Payload ExternalApiHealthCheckEvent event,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                           @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received External API Health Check Event - ID: {}, Name: {}, Status: {}, ResponseTime: {}ms, Partition: {}, Offset: {}",
                    event.getApiId(), event.getApiName(), event.getStatus(), event.getResponseTime(), partition, offset);
            
            // 비정상 API 정보 로깅
            if (!"HEALTHY".equals(event.getStatus())) {
                log.warn("Unhealthy API detected - ID: {}, Name: {}, Status: {}, Error: {}",
                        event.getApiId(), event.getApiName(), event.getStatus(), event.getErrorMessage());
            }
            
            // 외부 API 헬스체크 이벤트 처리 로직
            processApiHealthCheck(event);
            
            log.info("External API Health Check Event processed successfully: {}", event.getApiId());
            
        } catch (Exception e) {
            log.error("Error processing External API Health Check Event: {}", event, e);
            // 에러 처리 로직 (재시도, DLQ 전송 등)
        }
    }

    /**
     * 외부 API 등록 처리 로직
     */
    private void processApiRegistration(ExternalApiRegisteredEvent event) {
        // TODO: 실제 비즈니스 로직 구현
        // 예: 캐시 갱신, 알림 전송, 통계 업데이트 등
        log.info("Processing API registration for: {}", event.getApiName());
    }

    /**
     * 외부 API 삭제 처리 로직
     */
    private void processApiDeletion(ExternalApiDeletedEvent event) {
        // TODO: 실제 비즈니스 로직 구현
        // 예: 캐시 삭제, 관련 데이터 정리, 알림 전송 등
        log.info("Processing API deletion for: {}", event.getApiName());
    }

    /**
     * 외부 API 헬스체크 처리 로직
     */
    private void processApiHealthCheck(ExternalApiHealthCheckEvent event) {
        // TODO: 실제 비즈니스 로직 구현
        // 예: 상태 업데이트, 알림 전송, 모니터링 대시보드 갱신 등
        log.info("Processing API health check for: {} - Status: {}", event.getApiName(), event.getStatus());
    }

    /**
     * 외부 API 호출 실패 이벤트 처리 (가이드에 따른 구현)
     * 
     * @param event 외부 API 호출 실패 이벤트
     */
    @KafkaListener(
        topics = "external_api_events",
        containerFactory = "kafkaListenerContainerFactory",
        filter = "externalApiCallFailedEventFilter"
    )
    public void handleExternalApiCallFailed(ExternalApiCallFailedEventWrapper event) {
        try {
            log.info("외부 API 호출 실패 이벤트 수신: {}", event.getEventId());
            
            ExternalApiCallFailedEventWrapper.ExternalApiCallFailedPayload payload = event.getPayload();
            if (payload == null) {
                log.warn("No payload found in event: {}", event);
                return;
            }
            
            // 1. 로깅 및 모니터링
            log.error("API 호출 실패 - API: {}, URL: {}, 에러: {}",
                payload.getApiName(),
                payload.getApiUrl(),
                payload.getErrorMessage());
            
            // 2. 처리 로직 호출
            processApiCallFailureWrapper(payload);
            
        } catch (Exception e) {
            log.error("Error processing External API Call Failed Event: {}", event, e);
        }
    }

    /**
     * API 호출 실패 처리 로직
     * 
     * @param payload API 호출 실패 페이로드
     */
    private void processApiCallFailure(ExternalApiCallFailedEvent.ExternalApiCallFailedPayload payload) {
        try {
            log.warn("API call failed - ApiId: {}, ApiName: {}, URL: {}, Error: {}, ErrorType: {}, CalledBy: {}", 
                    payload.getApiId(), payload.getApiName(), payload.getApiUrl(), 
                    payload.getErrorMessage(), payload.getErrorType(), payload.getCalledBy());
            
            // apiId를 사용하여 헬스체크 수행
            if (payload.getApiId() != null) {
                String apiId = payload.getApiId();
                log.info("Triggering immediate health check for failed API: {} ({})", payload.getApiName(), apiId);
                apiHealthCheckService.performImmediateHealthCheck(apiId);
                log.info("Immediate health check triggered for API: {} due to call failure", payload.getApiName());
            } else {
                log.warn("Cannot trigger health check - apiId is null");
            }
            
        } catch (Exception e) {
            log.error("Failed to process API call failure: {}", payload, e);
        }
    }

    /**
     * API 호출 실패 처리 로직 (래퍼 이벤트용)
     * 
     * @param payload API 호출 실패 페이로드
     */
    private void processApiCallFailureWrapper(ExternalApiCallFailedEventWrapper.ExternalApiCallFailedPayload payload) {
        try {
            log.warn("API call failed - ApiId: {}, ApiName: {}, URL: {}, Error: {}, ErrorType: {}, CalledBy: {}", 
                    payload.getApiId(), payload.getApiName(), payload.getApiUrl(), 
                    payload.getErrorMessage(), payload.getErrorType(), payload.getCalledBy());
            
            // apiId를 사용하여 헬스체크 수행
            if (payload.getApiId() != null) {
                String apiId = payload.getApiId();
                log.info("Triggering immediate health check for failed API: {} ({})", payload.getApiName(), apiId);
                apiHealthCheckService.performImmediateHealthCheck(apiId);
                log.info("Immediate health check triggered for API: {} due to call failure", payload.getApiName());
            } else {
                log.warn("Cannot trigger health check - apiId is null");
            }
            
        } catch (Exception e) {
            log.error("Failed to process API call failure: {}", payload, e);
        }
    }

}