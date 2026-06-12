package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiScoreRequestedEvent(
        String requestId,
        String traceId,
        LocalDateTime requestedAt,
        List<AiScoreItem> items
) {
}
