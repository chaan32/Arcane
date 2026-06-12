package com.arcane.worker.kafka.event.dto;

import java.time.LocalDateTime;

public record RankingUpdateCompletedEvent(
        String eventId,
        String jobId,
        LocalDateTime completedAt,
        LocalDateTime requestedAt,
        String message
) {
}
