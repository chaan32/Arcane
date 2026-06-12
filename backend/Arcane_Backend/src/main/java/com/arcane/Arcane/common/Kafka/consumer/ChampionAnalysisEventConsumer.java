package com.arcane.Arcane.common.Kafka.consumer;

import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisFailedEvent;
import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisProgressEvent;
import com.arcane.Arcane.common.Kafka.service.ChampionAnalysisJobStatusService;
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
public class ChampionAnalysisEventConsumer {
    private final ObjectMapper objectMapper;
    private final ChampionAnalysisJobStatusService jobStatusService;

    @KafkaListener(
            topics = "${arcane.kafka.topics.champion-analysis-progress}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeProgress(String message, Acknowledgment ack) {
        try {
            ChampionAnalysisProgressEvent event =
                    objectMapper.readValue(message, ChampionAnalysisProgressEvent.class);
            jobStatusService.markProgress(event);
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeProgress", event.jobId());
            log.info(ApiLogSupport.api(
                    "챔피언 분석 Kafka",
                    "ChampionAnalysisEventConsumer.consumeProgress",
                    "진행 수신",
                    "jobId=" + event.jobId()
                            + " | snapshotId=" + event.snapshotId()
                            + " | progress=" + event.progressPercent() + "%"
                            + " | phase=" + event.phase()
            ));
        } catch (Exception e) {
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeProgress", "-");
            log.error(ApiLogSupport.api(
                            "챔피언 분석 Kafka",
                            "ChampionAnalysisEventConsumer.consumeProgress",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.champion-analysis-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCompleted(String message, Acknowledgment ack) {
        try {
            ChampionAnalysisCompletedEvent event =
                    objectMapper.readValue(message, ChampionAnalysisCompletedEvent.class);
            jobStatusService.markCompleted(event);
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeCompleted", event.jobId());
            log.info(ApiLogSupport.api(
                    "챔피언 분석 Kafka",
                    "ChampionAnalysisEventConsumer.consumeCompleted",
                    "완료 수신",
                    "jobId=" + event.jobId()
                            + " | snapshotId=" + event.snapshotId()
                            + " | patchVersion=" + event.patchVersion()
                            + " | participants=" + event.totalParticipants()
                            + " | matches=" + event.totalMatches()
                            + " | champions=" + event.totalChampions()
            ));
        } catch (Exception e) {
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeCompleted", "-");
            log.error(ApiLogSupport.api(
                            "챔피언 분석 Kafka",
                            "ChampionAnalysisEventConsumer.consumeCompleted",
                            "수신 실패",
                            "reason=" + e.getMessage() + " | payload=" + message
                    ),
                    e
            );
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.champion-analysis-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailed(String message, Acknowledgment ack) {
        try {
            ChampionAnalysisFailedEvent event =
                    objectMapper.readValue(message, ChampionAnalysisFailedEvent.class);
            jobStatusService.markFailed(event);
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeFailed", event.jobId());
            log.warn(ApiLogSupport.api(
                    "챔피언 분석 Kafka",
                    "ChampionAnalysisEventConsumer.consumeFailed",
                    "실패 수신",
                    "jobId=" + event.jobId() + " | reason=" + event.reason()
            ));
        } catch (Exception e) {
            acknowledge(ack, "ChampionAnalysisEventConsumer.consumeFailed", "-");
            log.error(ApiLogSupport.api(
                            "챔피언 분석 Kafka",
                            "ChampionAnalysisEventConsumer.consumeFailed",
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
                    "챔피언 분석 Kafka",
                    method,
                    "커밋 실패",
                    "jobId=" + jobId + " | reason=" + exception.getMessage()
            ));
        }
    }
}
