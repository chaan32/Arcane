package com.arcane.worker.kafka.consumer;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.common.logging.TraceIds;
import com.arcane.worker.gamedata.service.GameDataSyncService;
import com.arcane.worker.kafka.event.dto.GameDataSyncCompletedEvent;
import com.arcane.worker.kafka.event.dto.GameDataSyncFailedEvent;
import com.arcane.worker.kafka.event.dto.GameDataSyncRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameDataSyncConsumer {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GameDataSyncService gameDataSyncService;

    @Value("${arcane.kafka.topics.game-data-sync-completed:arcane.game-data.sync.completed}")
    private String completedTopic;

    @Value("${arcane.kafka.topics.game-data-sync-failed:arcane.game-data.sync.failed}")
    private String failedTopic;

    @KafkaListener(
            topics = "${arcane.kafka.topics.game-data-sync-requested:arcane.game-data.sync.requested}",
            containerFactory = "gameDataSyncKafkaListenerContainerFactory"
    )
    public void consumeGameDataSync(String message, Acknowledgment ack) {
        GameDataSyncRequestedEvent requestedEvent = null;

        try {
            requestedEvent = objectMapper.readValue(message, GameDataSyncRequestedEvent.class);
            putTraceContext(requestedEvent);

            log.info(logMessage(
                    "GameDataSyncConsumer.consumeGameDataSync",
                    "메시지 수신",
                    "jobId=" + requestedEvent.jobId()
                            + " | traceId=" + traceId(requestedEvent)
                            + " | requestedBy=" + requestedEvent.requestedBy()
            ));

            GameDataSyncService.SyncResult result = gameDataSyncService.syncLatestGameData();
            publishCompleted(requestedEvent, result);
            acknowledge(ack, requestedEvent.jobId(), "완료 커밋");

            log.info(logMessage(
                    "GameDataSyncConsumer.consumeGameDataSync",
                    "처리 완료",
                    "jobId=" + requestedEvent.jobId()
                            + " | version=" + result.version()
                            + " | champions=" + result.championCount()
                            + " | items=" + result.itemCount()
                            + " | spells=" + result.summonerSpellCount()
                            + " | runes=" + result.runeCount()
            ));
        } catch (Exception exception) {
            log.error(logMessage(
                            "GameDataSyncConsumer.consumeGameDataSync",
                            "처리 실패",
                            "jobId=" + (requestedEvent == null ? "-" : requestedEvent.jobId())
                                    + " | reason=" + exception.getMessage()
                    ),
                    exception
            );

            if (requestedEvent != null) {
                publishFailed(requestedEvent, exception);
            }

            acknowledge(ack, requestedEvent == null ? "-" : requestedEvent.jobId(), "실패 커밋");
        } finally {
            MDC.remove("traceId");
            MDC.remove("jobId");
        }
    }

    private void publishCompleted(
            GameDataSyncRequestedEvent requestedEvent,
            GameDataSyncService.SyncResult result
    ) throws Exception {
        GameDataSyncCompletedEvent completedEvent = new GameDataSyncCompletedEvent(
                requestedEvent.jobId(),
                traceId(requestedEvent),
                result.version(),
                result.championCount(),
                result.itemCount(),
                result.summonerSpellCount(),
                result.runeCount(),
                "COMPLETED",
                "게임 데이터 동기화 완료"
        );

        kafkaTemplate.send(completedTopic, requestedEvent.jobId(), completedEvent)
                .get(10, TimeUnit.SECONDS);

        log.info(logMessage(
                "GameDataSyncConsumer.publishCompleted",
                "완료 이벤트 발행",
                "jobId=" + requestedEvent.jobId()
                        + " | topic=" + completedTopic
                        + " | version=" + result.version()
        ));
    }

    private void publishFailed(GameDataSyncRequestedEvent requestedEvent, Exception exception) {
        try {
            GameDataSyncFailedEvent failedEvent = new GameDataSyncFailedEvent(
                    requestedEvent.jobId(),
                    traceId(requestedEvent),
                    exception.getMessage(),
                    "FAILED"
            );

            kafkaTemplate.send(failedTopic, requestedEvent.jobId(), failedEvent)
                    .get(10, TimeUnit.SECONDS);

            log.info(logMessage(
                    "GameDataSyncConsumer.publishFailed",
                    "실패 이벤트 발행",
                    "jobId=" + requestedEvent.jobId() + " | topic=" + failedTopic
            ));
        } catch (Exception publishException) {
            log.error(logMessage(
                            "GameDataSyncConsumer.publishFailed",
                            "실패 이벤트 발행 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | reason=" + publishException.getMessage()
                    ),
                    publishException
            );
        }
    }

    private void putTraceContext(GameDataSyncRequestedEvent requestedEvent) {
        if (requestedEvent.traceId() != null && !requestedEvent.traceId().isBlank()) {
            MDC.put("traceId", traceId(requestedEvent));
        }

        if (requestedEvent.jobId() != null && !requestedEvent.jobId().isBlank()) {
            MDC.put("jobId", requestedEvent.jobId());
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Kafka 게임 데이터 동기화", method, status, detail);
    }

    private String traceId(GameDataSyncRequestedEvent requestedEvent) {
        return TraceIds.normalize(requestedEvent.traceId());
    }

    private void acknowledge(Acknowledgment ack, String jobId, String status) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(logMessage(
                    "GameDataSyncConsumer.acknowledge",
                    status + " 실패",
                    "jobId=" + jobId
                            + " | reason=" + exception.getMessage()
                            + " | note=작업 결과는 이미 저장/발행됐으므로 실패 이벤트로 전환하지 않음"
            ));
        }
    }
}
