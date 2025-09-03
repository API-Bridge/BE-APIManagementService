package org.example.APIManagementSvc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.common.serialization.Deserializer;
import org.example.APIManagementSvc.event.model.ExternalApiCallFailedEventWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 서비스에서 전송되는 이벤트 형식에 맞춘 커스텀 JSON Deserializer
 */
public class CustomJsonDeserializer implements Deserializer<Object> {
    
    private static final Logger log = LoggerFactory.getLogger(CustomJsonDeserializer.class);
    private final ObjectMapper objectMapper;
    
    public CustomJsonDeserializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        // Configuration if needed
    }
    
    @Override
    public Object deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        
        try {
            String json = new String(data, StandardCharsets.UTF_8);
            log.debug("Deserializing JSON: {}", json);
            
            // JSON을 Map으로 먼저 파싱
            @SuppressWarnings("unchecked")
            Map<String, Object> rawEvent = objectMapper.readValue(json, Map.class);
            
            String eventType = (String) rawEvent.get("eventType");
            
            if ("external-api-call-failed".equals(eventType)) {
                return deserializeExternalApiCallFailedEvent(rawEvent);
            }
            
            // 다른 이벤트 타입들은 기본 처리
            return objectMapper.readValue(json, Object.class);
            
        } catch (Exception e) {
            log.error("Failed to deserialize message from topic {}: {}", topic, e.getMessage(), e);
            return null;
        }
    }
    
    private ExternalApiCallFailedEventWrapper deserializeExternalApiCallFailedEvent(Map<String, Object> rawEvent) {
        try {
            ExternalApiCallFailedEventWrapper event = new ExternalApiCallFailedEventWrapper();
            
            event.setEventId((String) rawEvent.get("eventId"));
            event.setTraceId((String) rawEvent.get("traceId"));
            event.setServiceName((String) rawEvent.get("serviceName"));
            event.setEventType((String) rawEvent.get("eventType"));
            
            // timestamp 배열 처리 [2025,9,3,9,56,28,578169000]
            Object timestampObj = rawEvent.get("timestamp");
            if (timestampObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Integer> timestampList = (List<Integer>) timestampObj;
                if (timestampList.size() >= 6) {
                    LocalDateTime timestamp = LocalDateTime.of(
                        timestampList.get(0), // year
                        timestampList.get(1), // month
                        timestampList.get(2), // day
                        timestampList.get(3), // hour
                        timestampList.get(4), // minute
                        timestampList.get(5), // second
                        timestampList.size() > 6 ? timestampList.get(6) : 0 // nanoseconds
                    );
                    event.setTimestamp(timestamp);
                }
            }
            
            // payload 처리
            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = (Map<String, Object>) rawEvent.get("payload");
            if (payloadMap != null) {
                ExternalApiCallFailedEventWrapper.ExternalApiCallFailedPayload payload = 
                    new ExternalApiCallFailedEventWrapper.ExternalApiCallFailedPayload();
                
                payload.setApiId((String) payloadMap.get("apiId"));
                payload.setApiName((String) payloadMap.get("apiName"));
                payload.setApiUrl((String) payloadMap.get("apiUrl"));
                payload.setHttpMethod((String) payloadMap.get("httpMethod"));
                payload.setStatusCode((Integer) payloadMap.get("statusCode"));
                payload.setErrorMessage((String) payloadMap.get("errorMessage"));
                payload.setErrorType((String) payloadMap.get("errorType"));
                payload.setResponseTime((Integer) payloadMap.get("responseTime"));
                payload.setRequestId((String) payloadMap.get("requestId"));
                payload.setCalledBy((String) payloadMap.get("calledBy"));
                
                // failedAt 처리 (배열 형식)
                Object failedAtObj = payloadMap.get("failedAt");
                if (failedAtObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Integer> failedAtList = (List<Integer>) failedAtObj;
                    if (failedAtList.size() >= 6) {
                        LocalDateTime failedAt = LocalDateTime.of(
                            failedAtList.get(0), // year
                            failedAtList.get(1), // month
                            failedAtList.get(2), // day
                            failedAtList.get(3), // hour
                            failedAtList.get(4), // minute
                            failedAtList.get(5), // second
                            failedAtList.size() > 6 ? failedAtList.get(6) : 0 // nanoseconds
                        );
                        payload.setFailedAt(failedAt);
                    }
                }
                
                event.setPayload(payload);
            }
            
            log.debug("Successfully deserialized ExternalApiCallFailedEvent: {}", event.getEventId());
            return event;
            
        } catch (Exception e) {
            log.error("Failed to deserialize ExternalApiCallFailedEvent", e);
            return null;
        }
    }
    
    @Override
    public void close() {
        // Cleanup if needed
    }
}