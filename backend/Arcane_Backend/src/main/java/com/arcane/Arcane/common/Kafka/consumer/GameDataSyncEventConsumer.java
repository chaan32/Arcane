package com.arcane.Arcane.common.Kafka.consumer;

import com.arcane.Arcane.common.Kafka.dto.GameDataSyncCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.GameDataSyncFailedEvent;
import com.arcane.Arcane.common.Kafka.service.GameDataSyncJobStatusService;
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
public class GameDataSyncEventConsumer {
    private final ObjectMapper objectMapper;
    private final GameDataSyncJobStatusService jobStatusService;

    @KafkaListener(
            topics = "${arcane.kafka.topics.game-data-sync-completed:arcane.game-data.sync.completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCompleted(String message, Acknowledgment ack) {
        try {
            GameDataSyncCompletedEvent event =
                    objectMapper.readValue(message, GameDataSyncCompletedEvent.class);
            jobStatusService.markCompleted(event);
            ack.acknowledge();
            log.info(ApiLogSupport.api(
                    "게임 데이터 동기화 Kafka",
                    "GameDataSyncEventConsumer.consumeCompleted",
                    "완료 수신",
                    "jobId=" + event.jobId()
                            + " | version=" + event.version()
                            + " | champions=" + event.championCount()
                            + " | items=" + event.itemCount()
                            + " | spells=" + event.summonerSpellCount()
                            + " | runes=" + event.runeCount()
            ));
        } catch (Exception e) {
            ack.acknowledge();
            log.error(ApiLogSupport.api(
                            "게임 데이터 동기화 Kafka",
                            "GameDataSyncEventConsumer.consumeCompleted",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.game-data-sync-failed:arcane.game-data.sync.failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailed(String message, Acknowledgment ack) {
        try {
            GameDataSyncFailedEvent event =
                    objectMapper.readValue(message, GameDataSyncFailedEvent.class);
            jobStatusService.markFailed(event);
            ack.acknowledge();
            log.warn(ApiLogSupport.api(
                    "게임 데이터 동기화 Kafka",
                    "GameDataSyncEventConsumer.consumeFailed",
                    "실패 수신",
                    "jobId=" + event.jobId() + " | reason=" + event.reason()
            ));
        } catch (Exception e) {
            ack.acknowledge();
            log.error(ApiLogSupport.api(
                            "게임 데이터 동기화 Kafka",
                            "GameDataSyncEventConsumer.consumeFailed",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }
}
