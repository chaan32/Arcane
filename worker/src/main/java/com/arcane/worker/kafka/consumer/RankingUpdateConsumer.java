package com.arcane.worker.kafka.consumer;

import com.arcane.worker.common.logging.WorkerLogSupport;
import com.arcane.worker.common.logging.TraceIds;
import com.arcane.worker.kafka.event.dto.RankingUpdateCompletedEvent;
import com.arcane.worker.kafka.event.dto.RankingUpdateFailEvent;
import com.arcane.worker.kafka.event.dto.RankingUpdateRequestedEvent;
import com.arcane.worker.ranker.service.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingUpdateConsumer {
    // worker가 성공/실패 이벤트를 Kafka에 다시 보낼 때 사용
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RankingService rankingService;

    // 성공|실패 이벤트를 보낼 topic 이름
    @Value("${arcane.kafka.topics.ranking-update-completed}")
    private String completedTopic;
    @Value("${arcane.kafka.topics.ranking-update-failed}")
    private String failedTopic;


    @KafkaListener(
            // API 서버가 랭킹 갱신 요청 이벤트를 발행하는 topic.
            topics = "${arcane.kafka.topics.ranking-update-requested}",
            // 같은 group 안의 worker 인스턴스끼리는 요청 메시지를 나눠서 처리한다.
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeRankingUpdate(RankingUpdateRequestedEvent requestedEvent, Acknowledgment ack) {
        putTraceContext(requestedEvent);
        log.info(WorkerLogSupport.log(
                "Kafka 랭킹 갱신",
                "RankingUpdateConsumer.consumeRankingUpdate",
                "메시지 수신",
                "jobId=" + requestedEvent.jobId()
                        + " | jobKey=" + requestedEvent.jobKey()
                        + " | traceId=" + traceId(requestedEvent)
        ));
        try {
            rankingService.updateRanking(requestedEvent);

            publishCompleted(requestedEvent);
            // 작업 완료 이벤트 발행까지 성공한 뒤 offset을 커밋한다.
            acknowledge(ack, requestedEvent.jobId(), "완료 커밋");

            log.info(WorkerLogSupport.log(
                    "Kafka 랭킹 갱신",
                    "RankingUpdateConsumer.consumeRankingUpdate",
                    "처리 완료",
                    "jobId=" + requestedEvent.jobId() + " | traceId=" + traceId(requestedEvent)
            ));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error(WorkerLogSupport.log(
                            "Kafka 랭킹 갱신",
                            "RankingUpdateConsumer.consumeRankingUpdate",
                            "처리 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | traceId=" + traceId(requestedEvent)
                                    + " | reason=" + e.getMessage()
                    ),
                    e
            );
            publishFailed(requestedEvent, e);
            // 실패 이벤트 발행을 시도한 뒤 offset을 커밋해 같은 무거운 작업이 무한 재시도되지 않게 한다.
            acknowledge(ack, requestedEvent.jobId(), "실패 커밋");
        } finally {
            MDC.remove("traceId");
            MDC.remove("jobId");
        }
    }

    private void publishCompleted(RankingUpdateRequestedEvent requestedEvent) throws Exception {
        RankingUpdateCompletedEvent completedEvent = new RankingUpdateCompletedEvent(
                UUID.randomUUID().toString(),
                requestedEvent.jobId(),
                LocalDateTime.now(),
                requestedEvent.requestedAt(),
                "랭킹 업데이트 완료"
        );

        kafkaTemplate.send(completedTopic, requestedEvent.jobId(), completedEvent)
                .get(10, TimeUnit.SECONDS);
        log.info(WorkerLogSupport.log(
                "Kafka 랭킹 갱신",
                "RankingUpdateConsumer.publishCompleted",
                "완료 이벤트 발행",
                "jobId=" + requestedEvent.jobId() + " | topic=" + completedTopic
        ));
    }

    private void publishFailed(RankingUpdateRequestedEvent requestedEvent, Exception exception) {
        try {
            RankingUpdateFailEvent failedEvent = new RankingUpdateFailEvent(
                    UUID.randomUUID().toString(),
                    requestedEvent.jobId(),
                    LocalDateTime.now(),
                    exception.getMessage()
            );

            kafkaTemplate.send(failedTopic, requestedEvent.jobId(), failedEvent)
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception publishException) {
            log.error(WorkerLogSupport.log(
                            "Kafka 랭킹 갱신",
                            "RankingUpdateConsumer.publishFailed",
                            "실패 이벤트 발행 실패",
                            "jobId=" + requestedEvent.jobId()
                                    + " | traceId=" + traceId(requestedEvent)
                                    + " | reason=" + publishException.getMessage()
                    ),
                    publishException
            );
        }
    }

    private void putTraceContext(RankingUpdateRequestedEvent requestedEvent) {
        if (requestedEvent.traceId() != null && !requestedEvent.traceId().isBlank()) {
            MDC.put("traceId", traceId(requestedEvent));
        }

        if (requestedEvent.jobId() != null && !requestedEvent.jobId().isBlank()) {
            MDC.put("jobId", requestedEvent.jobId());
        }
    }

    private String traceId(RankingUpdateRequestedEvent requestedEvent) {
        return TraceIds.normalize(requestedEvent.traceId());
    }

    private void acknowledge(Acknowledgment ack, String jobId, String status) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(WorkerLogSupport.log(
                    "Kafka 랭킹 갱신",
                    "RankingUpdateConsumer.acknowledge",
                    status + " 실패",
                    "jobId=" + jobId
                            + " | reason=" + exception.getMessage()
                            + " | note=작업 결과는 이미 저장/발행됐으므로 실패 이벤트로 전환하지 않음"
            ));
        }
    }
}
