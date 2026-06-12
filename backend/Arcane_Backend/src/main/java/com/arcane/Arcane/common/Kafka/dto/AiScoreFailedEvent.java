package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;

public record AiScoreFailedEvent(
        String requestId,
        String traceId,
        LocalDateTime failedAt,
        String reason
) {
}
