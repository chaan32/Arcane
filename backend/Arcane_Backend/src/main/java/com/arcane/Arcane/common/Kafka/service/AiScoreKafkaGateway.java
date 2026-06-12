package com.arcane.Arcane.common.Kafka.service;

import com.arcane.Arcane.common.Kafka.dto.AiScoreCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.AiScoreFailedEvent;
import com.arcane.Arcane.common.Kafka.dto.AiScoreItem;
import com.arcane.Arcane.common.Kafka.dto.AiScoreRequestedEvent;
import com.arcane.Arcane.common.Kafka.dto.AiScoreResult;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.common.Logging.TraceIds;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiScoreKafkaGateway {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CompletableFuture<AiScoreCompletedEvent>> pendingRequests = new ConcurrentHashMap<>();

    @Value("${arcane.kafka.topics.ai-score-requested}")
    private String aiScoreRequestedTopic;

    @Value("${modeling.kafka-timeout-ms:3000}")
    private long kafkaTimeoutMs;

    public List<Double> requestScores(List<Map<String, Object>> featuresList) {
        if (featuresList == null || featuresList.isEmpty()) {
            return List.of();
        }

        String requestId = "ai-score-" + UUID.randomUUID();
        String traceId = TraceIds.newTraceId();
        List<AiScoreItem> items = new ArrayList<>();
        for (int index = 0; index < featuresList.size(); index++) {
            items.add(new AiScoreItem(String.valueOf(index), featuresList.get(index)));
        }

        AiScoreRequestedEvent event = new AiScoreRequestedEvent(
                requestId,
                traceId,
                LocalDateTime.now(),
                items
        );

        CompletableFuture<AiScoreCompletedEvent> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        long startedAt = System.currentTimeMillis();
        kafkaTemplate.send(aiScoreRequestedTopic, requestId, event)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        return;
                    }
                    CompletableFuture<AiScoreCompletedEvent> pending = pendingRequests.remove(requestId);
                    if (pending != null) {
                        pending.completeExceptionally(exception);
                    }
                    log.warn(ApiLogSupport.api(
                            "AI 점수 Kafka",
                            "AiScoreKafkaGateway.requestScores",
                            "발행 실패",
                            "requestId=" + requestId + " | traceId=" + traceId + " | reason=" + exception.getMessage()
                    ));
                });

        try {
            AiScoreCompletedEvent response = future.get(kafkaTimeoutMs, TimeUnit.MILLISECONDS);
            log.info(ApiLogSupport.api(
                    "AI 점수 Kafka",
                    "AiScoreKafkaGateway.requestScores",
                    "응답 완료",
                    "requestId=" + requestId
                            + " | traceId=" + traceId
                            + " | count=" + featuresList.size()
                            + " | elapsedMs=" + (System.currentTimeMillis() - startedAt)
                            + " | aiElapsedMs=" + response.elapsedMs()
            ));
            return response.scores().stream()
                    .sorted(Comparator.comparingInt(score -> Integer.parseInt(score.itemId())))
                    .map(AiScoreResult::score)
                    .toList();
        } catch (TimeoutException exception) {
            throw new IllegalStateException("AI score Kafka response timeout. requestId=" + requestId, exception);
        } catch (Exception exception) {
            throw new IllegalStateException("AI score Kafka request failed. requestId=" + requestId, exception);
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.ai-score-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeCompleted(String message, Acknowledgment ack) {
        try {
            AiScoreCompletedEvent event = objectMapper.readValue(message, AiScoreCompletedEvent.class);
            CompletableFuture<AiScoreCompletedEvent> future = pendingRequests.remove(event.requestId());
            if (future != null) {
                future.complete(event);
            }
            acknowledge(ack, "AiScoreKafkaGateway.consumeCompleted", event.requestId());
        } catch (Exception exception) {
            acknowledge(ack, "AiScoreKafkaGateway.consumeCompleted", "-");
            log.warn(ApiLogSupport.api(
                    "AI 점수 Kafka",
                    "AiScoreKafkaGateway.consumeCompleted",
                    "수신 실패",
                    "reason=" + exception.getMessage() + " | payload=" + message
            ));
        }
    }

    @KafkaListener(
            topics = "${arcane.kafka.topics.ai-score-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeFailed(String message, Acknowledgment ack) {
        try {
            AiScoreFailedEvent event = objectMapper.readValue(message, AiScoreFailedEvent.class);
            CompletableFuture<AiScoreCompletedEvent> future = pendingRequests.remove(event.requestId());
            if (future != null) {
                future.completeExceptionally(new IllegalStateException(event.reason()));
            }
            acknowledge(ack, "AiScoreKafkaGateway.consumeFailed", event.requestId());
        } catch (Exception exception) {
            acknowledge(ack, "AiScoreKafkaGateway.consumeFailed", "-");
            log.warn(ApiLogSupport.api(
                    "AI 점수 Kafka",
                    "AiScoreKafkaGateway.consumeFailed",
                    "수신 실패",
                    "reason=" + exception.getMessage() + " | payload=" + message
            ));
        }
    }

    private void acknowledge(Acknowledgment ack, String method, String requestId) {
        try {
            ack.acknowledge();
        } catch (RuntimeException exception) {
            log.warn(ApiLogSupport.api(
                    "AI 점수 Kafka",
                    method,
                    "커밋 실패",
                    "requestId=" + requestId + " | reason=" + exception.getMessage()
            ));
        }
    }
}
