package com.arcane.worker.kafka.consumer;

import com.arcane.worker.analysis.service.ChampionAnalysisService;
import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.common.logging.TraceIds;
import com.arcane.worker.kafka.event.dto.ChampionAnalysisCompletedEvent;
import com.arcane.worker.kafka.event.dto.ChampionAnalysisFailedEvent;
import com.arcane.worker.kafka.event.dto.ChampionAnalysisProgressEvent;
import com.arcane.worker.kafka.event.dto.ChampionAnalysisRequestedEvent;
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
public class ChampionAnalysisConsumer {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ChampionAnalysisService championAnalysisService;

    @Value("${arcane.kafka.topics.champion-analysis-completed}")
    private String completedTopic;

    @Value("${arcane.kafka.topics.champion-analysis-progress}")
    private String progressTopic;

    @Value("${arcane.kafka.topics.champion-analysis-failed}")
    private String failedTopic;

    @KafkaListener(
            topics = "${arcane.kafka.topics.champion-analysis-requested}",
            containerFactory = "championAnalysisKafkaListenerContainerFactory"
    )
    public void consumeChampionAnalysis(String message, Acknowledgment ack) {
        ChampionAnalysisRequestedEvent requestedEvent = null;

        try {
            requestedEvent = objectMapper.readValue(message, ChampionAnalysisRequestedEvent.class);
            putTraceContext(requestedEvent);

            log.info(logMessage(
                    "ChampionAnalysisConsumer.consumeChampionAnalysis",
                    "메시지 수신",
                    "jobId=" + requestedEvent.jobId()
                            + " | traceId=" + traceId(requestedEvent)
                            + " | queueId=" + requestedEvent.queueId()
                            + " | sourceCollection=" + requestedEvent.sourceCollection()
            ));

            ChampionAnalysisRequestedEvent currentEvent = requestedEvent;
            ChampionAnalysisService.AnalysisResult result = championAnalysisService.analyze(
                    currentEvent,
                    progress -> publishProgress(currentEvent, progress)
            );
            publishCompleted(requestedEvent, result);
            acknowledge(ack, requestedEvent.jobId(), "완료 커밋");

            log.info(logMessage(
                    "ChampionAnalysisConsumer.consumeChampionAnalysis",
                    "처리 완료",
                    "jobId=" + requestedEvent.jobId()
                            + " | snapshotId=" + result.snapshotId()
                            + " | participants=" + result.totalParticipants()
                            + " | matches=" + result.totalMatches()
                            + " | champions=" + result.totalChampions()
            ));
        } catch (Exception exception) {
            log.error(logMessage(
                            "ChampionAnalysisConsumer.consumeChampionAnalysis",
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
            ChampionAnalysisRequestedEvent requestedEvent,
            ChampionAnalysisService.AnalysisResult result
    ) throws Exception {
        ChampionAnalysisCompletedEvent completedEvent = new ChampionAnalysisCompletedEvent(
                requestedEvent.jobId(),
                traceId(requestedEvent),
                result.snapshotId(),
                result.totalParticipants(),
                result.totalMatches(),
                result.totalChampions(),
                result.patchVersion(),
                "COMPLETED"
        );

        kafkaTemplate.send(completedTopic, requestedEvent.jobId(), completedEvent)
                .get(10, TimeUnit.SECONDS);

        log.info(logMessage(
                "ChampionAnalysisConsumer.publishCompleted",
                "완료 이벤트 발행",
                "jobId=" + requestedEvent.jobId()
                        + " | topic=" + completedTopic
                        + " | snapshotId=" + result.snapshotId()
                        + " | patchVersion=" + result.patchVersion()
        ));
    }

    private void publishFailed(ChampionAnalysisRequestedEvent requestedEvent, Exception exception) {
        try {
            ChampionAnalysisFailedEvent failedEvent = new ChampionAnalysisFailedEvent(
                    requestedEvent.jobId(),
                    traceId(requestedEvent),
                    exception.getMessage(),
                    "FAILED"
            );

            kafkaTemplate.send(failedTopic, requestedEvent.jobId(), failedEvent)
                    .get(10, TimeUnit.SECONDS);

            log.info(logMessage(
                    "ChampionAnalysisConsumer.publishFailed",
                    "실패 이벤트 발행",
                    "jobId=" + requestedEvent.jobId() + " | topic=" + failedTopic
            ));
        } catch (Exception publishException) {
            log.error(logMessage(
                            "ChampionAnalysisConsumer.publishFailed",
                            "실패 이벤트 발행 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | reason=" + publishException.getMessage()
                    ),
                    publishException
            );
        }
    }

    private void publishProgress(
            ChampionAnalysisRequestedEvent requestedEvent,
            ChampionAnalysisService.AnalysisProgress progress
    ) {
        ChampionAnalysisProgressEvent progressEvent = new ChampionAnalysisProgressEvent(
                requestedEvent.jobId(),
                traceId(requestedEvent),
                progress.snapshotId(),
                progress.progressPercent(),
                progress.phase(),
                progress.message(),
                progress.totalParticipants(),
                progress.totalMatches(),
                progress.totalChampions(),
                progress.patchVersion(),
                "RUNNING"
        );

        kafkaTemplate.send(progressTopic, requestedEvent.jobId(), progressEvent)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        return;
                    }

                    log.warn(logMessage(
                            "ChampionAnalysisConsumer.publishProgress",
                            "진행 이벤트 발행 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | progress=" + progress.progressPercent() + "%"
                                    + " | reason=" + exception.getMessage()
                    ));
                });
    }

    private void putTraceContext(ChampionAnalysisRequestedEvent requestedEvent) {
        if (requestedEvent.traceId() != null && !requestedEvent.traceId().isBlank()) {
            MDC.put("traceId", traceId(requestedEvent));
        }

        if (requestedEvent.jobId() != null && !requestedEvent.jobId().isBlank()) {
            MDC.put("jobId", requestedEvent.jobId());
        }
    }

    private String logMessage(String method, String status, String detail) {
        return WorkerLogSupport.log("Kafka 챔피언 분석", method, status, detail);
    }

    private String traceId(ChampionAnalysisRequestedEvent requestedEvent) {
        return TraceIds.normalize(requestedEvent.traceId());
    }

    private void acknowledge(Acknowledgment ack, String jobId, String status) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(logMessage(
                    "ChampionAnalysisConsumer.acknowledge",
                    status + " 실패",
                    "jobId=" + jobId
                            + " | reason=" + exception.getMessage()
                            + " | note=작업 결과는 이미 저장/발행됐으므로 실패 이벤트로 전환하지 않음"
            ));
        }
    }
}
