package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;

public record DatasetCollectFailEvent(
        String eventId,
        String jobId,
        LocalDateTime failedAt,
        String message
) {
}
