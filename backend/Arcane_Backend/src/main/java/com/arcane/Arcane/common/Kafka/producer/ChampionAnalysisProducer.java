package com.arcane.Arcane.common.Kafka.producer;

import com.arcane.Arcane.common.Kafka.dto.ChampionAnalysisRequestedEvent;
import com.arcane.Arcane.common.Kafka.service.ChampionAnalysisJobStatusService;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Logging.TraceIds;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChampionAnalysisProducer {
    private static final String SOURCE_COLLECTION = "riot_match_participant_raw";
    private static final int SOLO_RANK_QUEUE_ID = 420;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ChampionAnalysisJobStatusService jobStatusService;

    @Value("${arcane.kafka.topics.champion-analysis-requested}")
    private String championAnalysisRequestedTopic;

    public String requestChampionAnalysis(Long requestedBy) {
        String jobId = "champion-analysis-" + UUID.randomUUID();
        String traceId = TraceIds.newTraceId();

        ChampionAnalysisRequestedEvent event = new ChampionAnalysisRequestedEvent(
                jobId,
                traceId,
                requestedBy,
                SOURCE_COLLECTION,
                SOLO_RANK_QUEUE_ID
        );

        jobStatusService.markPublished(event);

        kafkaTemplate.send(championAnalysisRequestedTopic, jobId, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        jobStatusService.markPublishFailed(jobId, exception.getMessage());
                        log.error(ApiLogSupport.api(
                                        "챔피언 분석 Kafka",
                                        "ChampionAnalysisProducer.requestChampionAnalysis",
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
                            "챔피언 분석 Kafka",
                            "ChampionAnalysisProducer.requestChampionAnalysis",
                            "발행 완료",
                            "jobId=" + jobId
                                    + " | traceId=" + traceId
                                    + " | topic=" + result.getRecordMetadata().topic()
                                    + " | partition=" + result.getRecordMetadata().partition()
                                    + " | offset=" + result.getRecordMetadata().offset()
                    ));
                });

        log.info(ApiLogSupport.api(
                "챔피언 분석 Kafka",
                "ChampionAnalysisProducer.requestChampionAnalysis",
                "발행 요청",
                "jobId=" + jobId
                        + " | requestedBy=" + requestedBy
                        + " | traceId=" + traceId
                        + " | sourceCollection=" + SOURCE_COLLECTION
                        + " | queueId=" + SOLO_RANK_QUEUE_ID
        ));

        return jobId;
    }
}
