package com.arcane.Arcane.common.Kafka.producer;


import com.arcane.Arcane.common.Kafka.dto.RankingUpdatePayload;
import com.arcane.Arcane.common.Kafka.dto.RankingUpdateRequestedEvent;
import com.arcane.Arcane.common.Kafka.service.RankingUpdateJobStatusService;
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
public class RankingUpdateProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RankingUpdateJobStatusService jobStatusService;
    // Kafka로 메시지를 발행하는 객체

    @Value("${arcane.kafka.topics.ranking-update-requested}")
    private String rankingUpdateRequestedTopic;
    // worker가 구독하고 있는 요청 topic 이름

    public String requestRankingUpdate(Long requestedBy) {
        // 관리자 또는 스케줄러가 랭킹 업데이트를 요청할 때 호출하는 메소드

        String jobId = "ranking-" + UUID.randomUUID();
        // 이번 랭킹 업데이트 작업의 고유 ID 생성

        String traceId = TraceIds.newTraceId();
        // API 서버 로그와 worker 로그를 연결하기 위한 추적 ID

        RankingUpdateRequestedEvent event = new RankingUpdateRequestedEvent(
                UUID.randomUUID().toString(),
                jobId,
                "ranking:update:kr",
                requestedBy,
                LocalDateTime.now(),
                traceId,
                new RankingUpdatePayload("KR", 10000, Boolean.TRUE)
                // payload: 실제 작업 옵션. KR 서버 상위 10000명 업데이트
        );

        jobStatusService.markPublished(event);

        kafkaTemplate.send(
                // 메시지를 보낼 topic
                rankingUpdateRequestedTopic,
                // Kafka message key
                jobId,
                // 실제 메시지 body
                event
        ).whenComplete((result, exception) -> {
            if (exception != null) {
                jobStatusService.markPublishFailed(jobId, exception.getMessage());
                log.error(ApiLogSupport.api(
                                "랭킹 갱신 Kafka",
                                "RankingUpdateProducer.requestRankingUpdate",
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
                    "랭킹 갱신 Kafka",
                    "RankingUpdateProducer.requestRankingUpdate",
                    "발행 완료",
                    "jobId=" + jobId
                            + " | traceId=" + traceId
                            + " | topic=" + result.getRecordMetadata().topic()
                            + " | partition=" + result.getRecordMetadata().partition()
                            + " | offset=" + result.getRecordMetadata().offset()
            ));
        });

        log.info(ApiLogSupport.api(
                "랭킹 갱신 Kafka",
                "RankingUpdateProducer.requestRankingUpdate",
                "발행 요청",
                "jobId=" + jobId + " | requestedBy=" + requestedBy + " | traceId=" + traceId
        ));
        // Kafka 요청 발행 로그

        return jobId;
    }

}
