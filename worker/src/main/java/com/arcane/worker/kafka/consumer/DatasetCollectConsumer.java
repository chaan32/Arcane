package com.arcane.worker.kafka.consumer;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.common.logging.TraceIds;
import com.arcane.worker.dataset.service.DatasetCollectProgressService;
import com.arcane.worker.dataset.service.DatasetCollectService;
import com.arcane.worker.kafka.event.dto.DatasetCollectCompletedEvent;
import com.arcane.worker.kafka.event.dto.DatasetCollectFailEvent;
import com.arcane.worker.kafka.event.dto.DatasetCollectRequestedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatasetCollectConsumer {
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DatasetCollectService datasetCollectService;
    private final DatasetCollectProgressService progressService;

    @Value("${arcane.kafka.topics.dataset-collect-completed}")
    private String completedTopic;

    @Value("${arcane.kafka.topics.dataset-collect-failed}")
    private String failedTopic;

    @KafkaListener(
            topics = "${arcane.kafka.topics.dataset-collect-requested}",
            containerFactory = "datasetCollectKafkaListenerContainerFactory"
    )
    public void consumeDatasetCollect(String message, Acknowledgment ack) {
        DatasetCollectRequestedEvent requestedEvent = null;
        try {
            requestedEvent = objectMapper.readValue(message, DatasetCollectRequestedEvent.class);
            putTraceContext(requestedEvent);

            log.info(logMessage(
                    "DatasetCollectConsumer.consumeDatasetCollect",
                    "메시지 수신",
                    "jobId=" + requestedEvent.jobId()
                            + " | jobKey=" + requestedEvent.jobKey()
                            + " | traceId=" + TraceIds.normalize(requestedEvent.traceId())
            ));

            if (progressService.isTerminalStatus(requestedEvent.jobId())) {
                acknowledge(ack, requestedEvent.jobId(), "종료 작업 커밋");
                log.info(logMessage(
                        "DatasetCollectConsumer.consumeDatasetCollect",
                        "이미 종료된 작업 스킵",
                        "jobId=" + requestedEvent.jobId()
                                + " | status=" + progressService.statusOf(requestedEvent.jobId())
                ));
                return;
            }

            DatasetCollectService.CollectResult result = datasetCollectService.collect(requestedEvent);
            publishCompleted(requestedEvent, result);
            acknowledge(ack, requestedEvent.jobId(), "완료 커밋");

            log.info(logMessage(
                    "DatasetCollectConsumer.consumeDatasetCollect",
                    "처리 완료",
                    "jobId=" + requestedEvent.jobId()
                            + " | processedMatches=" + result.processedMatches()
                            + " | savedMongoParticipants=" + result.savedMongoParticipants()
            ));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            log.error(logMessage(
                            "DatasetCollectConsumer.consumeDatasetCollect",
                            "처리 실패",
                            "jobId=" + (requestedEvent == null ? "-" : requestedEvent.jobId())
                                    + " | reason=" + e.getMessage()
                    ),
                    e
            );

            if (requestedEvent != null) {
                try {
                    progressService.markFailed(requestedEvent.jobId(), e.getMessage());
                } catch (Exception progressException) {
                    log.warn(logMessage(
                            "DatasetCollectConsumer.consumeDatasetCollect",
                            "실패 상태 저장 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | reason=" + progressException.getMessage()
                    ));
                }
                publishFailed(requestedEvent, e);
            }
            acknowledge(ack, requestedEvent == null ? "-" : requestedEvent.jobId(), "실패 커밋");
        } finally {
            MDC.remove("traceId");
            MDC.remove("jobId");
        }
    }

    private void publishCompleted(
            DatasetCollectRequestedEvent requestedEvent,
            DatasetCollectService.CollectResult result
    ) throws Exception {
        DatasetCollectCompletedEvent completedEvent = new DatasetCollectCompletedEvent(
                UUID.randomUUID().toString(),
                requestedEvent.jobId(),
                LocalDateTime.now(),
                requestedEvent.requestedAt(),
                "데이터 수집 완료: uniqueMatchIds="
                        + result.uniqueMatchIds()
                        + ", savedMongoParticipants="
                        + result.savedMongoParticipants()
        );

        kafkaTemplate.send(completedTopic, requestedEvent.jobId(), completedEvent)
                .get(10, TimeUnit.SECONDS);
        log.info(logMessage(
                "DatasetCollectConsumer.publishCompleted",
                "완료 이벤트 발행",
                "jobId=" + requestedEvent.jobId() + " | topic=" + completedTopic
        ));
    }

    private void publishFailed(DatasetCollectRequestedEvent requestedEvent, Exception exception) {
        try {
            DatasetCollectFailEvent failedEvent = new DatasetCollectFailEvent(
                    UUID.randomUUID().toString(),
                    requestedEvent.jobId(),
                    LocalDateTime.now(),
                    exception.getMessage()
            );

            kafkaTemplate.send(failedTopic, requestedEvent.jobId(), failedEvent)
                    .get(10, TimeUnit.SECONDS);
            log.info(logMessage(
                    "DatasetCollectConsumer.publishFailed",
                    "실패 이벤트 발행",
                    "jobId=" + requestedEvent.jobId() + " | topic=" + failedTopic
            ));
        } catch (Exception publishException) {
            log.error(logMessage(
                            "DatasetCollectConsumer.publishFailed",
                            "실패 이벤트 발행 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | reason=" + publishException.getMessage()
                    ),
                    publishException
            );
        }
    }

    private void putTraceContext(DatasetCollectRequestedEvent requestedEvent) {
        if (requestedEvent.traceId() != null && !requestedEvent.traceId().isBlank()) {
            MDC.put("traceId", TraceIds.normalize(requestedEvent.traceId()));
        }

        if (requestedEvent.jobId() != null && !requestedEvent.jobId().isBlank()) {
            MDC.put("jobId", requestedEvent.jobId());
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Kafka 데이터 수집", method, status, detail);
    }

    private void acknowledge(Acknowledgment ack, String jobId, String status) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(logMessage(
                    "DatasetCollectConsumer.acknowledge",
                    status + " 실패",
                    "jobId=" + jobId
                            + " | reason=" + exception.getMessage()
                            + " | note=작업 결과는 이미 저장/발행됐으므로 실패 이벤트로 전환하지 않음"
            ));
        }
    }
}
