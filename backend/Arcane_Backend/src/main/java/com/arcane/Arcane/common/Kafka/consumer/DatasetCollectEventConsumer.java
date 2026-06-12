package com.arcane.Arcane.common.Kafka.consumer;

import com.arcane.Arcane.common.Kafka.dto.DatasetCollectCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.DatasetCollectFailEvent;
import com.arcane.Arcane.common.Kafka.service.DatasetCollectJobStatusService;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetCollectEventConsumer {
    private final ObjectMapper objectMapper;
    private final DatasetCollectJobStatusService jobStatusService;

    @KafkaListener(
            topics = "${arcane.kafka.topics.dataset-collect-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCompleted(String message, Acknowledgment ack) {
        try {
            DatasetCollectCompletedEvent event =
                    objectMapper.readValue(message, DatasetCollectCompletedEvent.class);
            jobStatusService.markCompleted(event);
            acknowledge(ack, "DatasetCollectEventConsumer.consumeCompleted", event.jobId());
            log.info(ApiLogSupport.api(
                    "데이터 수집 Kafka",
                    "DatasetCollectEventConsumer.consumeCompleted",
                    "완료 수신",
                    "jobId=" + event.jobId() + " | message=" + event.message()
            ));
        } catch (Exception e) {
            acknowledge(ack, "DatasetCollectEventConsumer.consumeCompleted", "-");
            log.error(ApiLogSupport.api(
                            "데이터 수집 Kafka",
                            "DatasetCollectEventConsumer.consumeCompleted",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.dataset-collect-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailed(String message, Acknowledgment ack) {
        try {
            DatasetCollectFailEvent event =
                    objectMapper.readValue(message, DatasetCollectFailEvent.class);
            jobStatusService.markFailed(event);
            acknowledge(ack, "DatasetCollectEventConsumer.consumeFailed", event.jobId());
            log.warn(ApiLogSupport.api(
                    "데이터 수집 Kafka",
                    "DatasetCollectEventConsumer.consumeFailed",
                    "실패 수신",
                    "jobId=" + event.jobId() + " | message=" + event.message()
            ));
        } catch (Exception e) {
            acknowledge(ack, "DatasetCollectEventConsumer.consumeFailed", "-");
            log.error(ApiLogSupport.api(
                            "데이터 수집 Kafka",
                            "DatasetCollectEventConsumer.consumeFailed",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    private void acknowledge(Acknowledgment ack, String method, String jobId) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(ApiLogSupport.api(
                    "데이터 수집 Kafka",
                    method,
                    "커밋 실패",
                    "jobId=" + jobId + " | reason=" + exception.getMessage()
            ));
        }
    }
}
