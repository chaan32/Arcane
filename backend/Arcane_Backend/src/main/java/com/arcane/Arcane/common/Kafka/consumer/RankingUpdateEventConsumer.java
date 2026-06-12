package com.arcane.Arcane.common.Kafka.consumer;

import com.arcane.Arcane.common.Kafka.dto.RankingUpdateCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.RankingUpdateFailEvent;
import com.arcane.Arcane.common.Kafka.service.RankingUpdateJobStatusService;
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
public class RankingUpdateEventConsumer {
    private final ObjectMapper objectMapper;
    private final RankingUpdateJobStatusService jobStatusService;

    @KafkaListener(
            topics = "${arcane.kafka.topics.ranking-update-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCompleted(String message, Acknowledgment ack) {
        try {
            RankingUpdateCompletedEvent event =
                    objectMapper.readValue(message, RankingUpdateCompletedEvent.class);
            jobStatusService.markCompleted(event);
            acknowledge(ack, "RankingUpdateEventConsumer.consumeCompleted", event.jobId());

            log.info(ApiLogSupport.api(
                    "랭킹 갱신 Kafka",
                    "RankingUpdateEventConsumer.consumeCompleted",
                    "완료 수신",
                    "jobId=" + event.jobId()
                            + " | completedAt=" + event.completedAt()
                            + " | message=" + event.message()
            ));
        } catch (Exception e) {
            acknowledge(ack, "RankingUpdateEventConsumer.consumeCompleted", "-");
            log.error(ApiLogSupport.api(
                            "랭킹 갱신 Kafka",
                            "RankingUpdateEventConsumer.consumeCompleted",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.ranking-update-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailed(String message, Acknowledgment ack) {
        try {
            RankingUpdateFailEvent event =
                    objectMapper.readValue(message, RankingUpdateFailEvent.class);
            jobStatusService.markFailed(event);
            acknowledge(ack, "RankingUpdateEventConsumer.consumeFailed", event.jobId());

            log.warn(ApiLogSupport.api(
                    "랭킹 갱신 Kafka",
                    "RankingUpdateEventConsumer.consumeFailed",
                    "실패 수신",
                    "jobId=" + event.jobId()
                            + " | failedAt=" + event.failedAt()
                            + " | message=" + event.message()
            ));
        } catch (Exception e) {
            acknowledge(ack, "RankingUpdateEventConsumer.consumeFailed", "-");
            log.error(ApiLogSupport.api(
                            "랭킹 갱신 Kafka",
                            "RankingUpdateEventConsumer.consumeFailed",
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
                    "랭킹 갱신 Kafka",
                    method,
                    "커밋 실패",
                    "jobId=" + jobId + " | reason=" + exception.getMessage()
            ));
        }
    }
}
