package com.arcane.Arcane.common.Kafka.producer;

import com.arcane.Arcane.common.Kafka.dto.GameDataSyncRequestedEvent;
import com.arcane.Arcane.common.Kafka.service.GameDataSyncJobStatusService;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Logging.TraceIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameDataSyncProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GameDataSyncJobStatusService jobStatusService;

    @Value("${arcane.kafka.topics.game-data-sync-requested:arcane.game-data.sync.requested}")
    private String gameDataSyncRequestedTopic;

    public String requestGameDataSync(Long requestedBy) {
        String jobId = "game-data-sync-" + UUID.randomUUID();
        String traceId = TraceIds.newTraceId();

        GameDataSyncRequestedEvent event = new GameDataSyncRequestedEvent(
                jobId,
                traceId,
                requestedBy,
                LocalDateTime.now()
        );

        jobStatusService.markPublished(event);

        kafkaTemplate.send(gameDataSyncRequestedTopic, jobId, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        jobStatusService.markPublishFailed(jobId, exception.getMessage());
                        log.error(ApiLogSupport.api(
                                        "게임 데이터 동기화 Kafka",
                                        "GameDataSyncProducer.requestGameDataSync",
                                        "발행 실패",
                                        "jobId=" + jobId
                                                + " | traceId=" + traceId
                                                + " | reason=" + exception.getMessage()
                                ),
                                exception
                        );
                        return;
                    }

                    log.info(ApiLogSupport.api(
                            "게임 데이터 동기화 Kafka",
                            "GameDataSyncProducer.requestGameDataSync",
                            "발행 완료",
                            "jobId=" + jobId
                                    + " | traceId=" + traceId
                                    + " | topic=" + result.getRecordMetadata().topic()
                                    + " | partition=" + result.getRecordMetadata().partition()
                                    + " | offset=" + result.getRecordMetadata().offset()
                    ));
                });

        log.info(ApiLogSupport.api(
                "게임 데이터 동기화 Kafka",
                "GameDataSyncProducer.requestGameDataSync",
                "발행 요청",
                "jobId=" + jobId
                        + " | requestedBy=" + requestedBy
                        + " | traceId=" + traceId
        ));

        return jobId;
    }
}
