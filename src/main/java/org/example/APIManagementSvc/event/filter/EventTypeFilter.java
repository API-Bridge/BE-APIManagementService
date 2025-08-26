package org.example.APIManagementSvc.event.filter;

import org.example.APIManagementSvc.event.model.BaseEvent;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.stereotype.Component;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Kafka 메시지 필터링을 위한 컴포넌트
 * 이벤트 타입에 따라 메시지를 필터링하여 해당하는 리스너로만 전달
 */
@Component
public class EventTypeFilter {

    @Component("externalApiRegisteredEventFilter")
    public static class ExternalApiRegisteredEventFilter implements RecordFilterStrategy<String, Object> {
        @Override
        public boolean filter(ConsumerRecord<String, Object> consumerRecord) {
            if (consumerRecord.value() instanceof BaseEvent) {
                BaseEvent event = (BaseEvent) consumerRecord.value();
                return !"EXTERNAL_API_REGISTERED".equals(event.getEventType());
            }
            return true; // BaseEvent가 아닌 경우 필터링
        }
    }

    @Component("externalApiDeletedEventFilter")
    public static class ExternalApiDeletedEventFilter implements RecordFilterStrategy<String, Object> {
        @Override
        public boolean filter(ConsumerRecord<String, Object> consumerRecord) {
            if (consumerRecord.value() instanceof BaseEvent) {
                BaseEvent event = (BaseEvent) consumerRecord.value();
                return !"EXTERNAL_API_DELETED".equals(event.getEventType());
            }
            return true; // BaseEvent가 아닌 경우 필터링
        }
    }

    @Component("externalApiHealthCheckEventFilter")
    public static class ExternalApiHealthCheckEventFilter implements RecordFilterStrategy<String, Object> {
        @Override
        public boolean filter(ConsumerRecord<String, Object> consumerRecord) {
            if (consumerRecord.value() instanceof BaseEvent) {
                BaseEvent event = (BaseEvent) consumerRecord.value();
                return !"EXTERNAL_API_HEALTH_CHECK".equals(event.getEventType());
            }
            return true; // BaseEvent가 아닌 경우 필터링
        }
    }
}