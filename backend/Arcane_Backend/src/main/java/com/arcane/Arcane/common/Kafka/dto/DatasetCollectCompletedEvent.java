package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;

public record DatasetCollectCompletedEvent(
        String eventId,
        String jobId,
        LocalDateTime completedAt,
        LocalDateTime requestedAt,
        String message
) {
}
