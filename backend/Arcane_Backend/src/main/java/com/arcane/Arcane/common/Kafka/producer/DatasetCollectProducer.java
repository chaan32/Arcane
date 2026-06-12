package com.arcane.Arcane.common.Kafka.producer;

import com.arcane.Arcane.common.Kafka.dto.DatasetCollectPayload;
import com.arcane.Arcane.common.Kafka.dto.DatasetCollectRequestedEvent;
import com.arcane.Arcane.common.Kafka.service.DatasetCollectJobStatusService;
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
public class DatasetCollectProducer {
    private static final String DEFAULT_REGION = "KR";
    private static final String DEFAULT_RANKING_KEY = "ranking:all";
    private static final int DEFAULT_RANKER_LIMIT = 500;
    private static final int DEFAULT_MATCH_COUNT = 15;
    private static final int DEFAULT_QUEUE_ID = 420;
    private static final int MAX_RANKER_LIMIT = 10_000;
    private static final int MAX_MATCH_COUNT = 100;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final DatasetCollectJobStatusService jobStatusService;

    @Value("${arcane.kafka.topics.dataset-collect-requested}")
    private String datasetCollectRequestedTopic;

    public String requestDatasetCollect(Long requestedBy) {
        return requestDatasetCollect(requestedBy, DEFAULT_RANKING_KEY, DEFAULT_RANKER_LIMIT, DEFAULT_MATCH_COUNT, DEFAULT_QUEUE_ID);
    }

    public String requestDatasetCollect(
            Long requestedBy,
            String rankingKey,
            Integer rankerLimit,
            Integer matchCount,
            Integer queueId
    ) {
        String jobId = "dataset-" + UUID.randomUUID();
        String traceId = TraceIds.newTraceId();
        String safeRankingKey = normalizeRankingKey(rankingKey);
        int safeRankerLimit = clamp(
                rankerLimit == null ? DEFAULT_RANKER_LIMIT : rankerLimit,
                1,
                MAX_RANKER_LIMIT
        );
        int safeMatchCount = clamp(
                matchCount == null ? DEFAULT_MATCH_COUNT : matchCount,
                1,
                MAX_MATCH_COUNT
        );
        int safeQueueId = queueId == null || queueId <= 0 ? DEFAULT_QUEUE_ID : queueId;

        DatasetCollectRequestedEvent event = new DatasetCollectRequestedEvent(
                UUID.randomUUID().toString(),
                jobId,
                "dataset:collect:" + safeRankingKey.replace("ranking:", ""),
                requestedBy,
                LocalDateTime.now(),
                traceId,
                new DatasetCollectPayload(
                        DEFAULT_REGION,
                        safeRankingKey,
                        safeRankerLimit,
                        safeMatchCount,
                        safeQueueId,
                        Boolean.TRUE,
                        Boolean.TRUE
                )
        );

        jobStatusService.markPublished(event);

        kafkaTemplate.send(datasetCollectRequestedTopic, jobId, event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        jobStatusService.markPublishFailed(jobId, exception.getMessage());
                        log.error(ApiLogSupport.api(
                                        "데이터 수집 Kafka",
                                        "DatasetCollectProducer.requestDatasetCollect",
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
                            "데이터 수집 Kafka",
                            "DatasetCollectProducer.requestDatasetCollect",
                            "발행 완료",
                            "jobId=" + jobId
                                    + " | traceId=" + traceId
                                    + " | topic=" + result.getRecordMetadata().topic()
                                    + " | partition=" + result.getRecordMetadata().partition()
                                    + " | offset=" + result.getRecordMetadata().offset()
                    ));
                });

        log.info(ApiLogSupport.api(
                "데이터 수집 Kafka",
                "DatasetCollectProducer.requestDatasetCollect",
                "발행 요청",
                "jobId=" + jobId
                        + " | requestedBy=" + requestedBy
                        + " | traceId=" + traceId
                        + " | rankingKey=" + safeRankingKey
                        + " | rankerLimit=" + safeRankerLimit
                        + " | matchCount=" + safeMatchCount
                        + " | queueId=" + safeQueueId
        ));

        return jobId;
    }

    private String normalizeRankingKey(String rankingKey) {
        if (rankingKey == null || rankingKey.isBlank()) {
            return DEFAULT_RANKING_KEY;
        }

        return switch (rankingKey) {
            case "ranking:all", "ranking:challenger", "ranking:grandmaster", "ranking:master" -> rankingKey;
            default -> DEFAULT_RANKING_KEY;
        };
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
