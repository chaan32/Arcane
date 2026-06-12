package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiScoreCompletedEvent(
        String requestId,
        String traceId,
        LocalDateTime completedAt,
        Long elapsedMs,
        List<AiScoreResult> scores
) {
}
