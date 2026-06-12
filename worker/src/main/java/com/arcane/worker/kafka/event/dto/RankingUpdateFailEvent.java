package com.arcane.worker.kafka.event.dto;

import java.time.LocalDateTime;

public record RankingUpdateFailEvent(
        String eventId,
        String jobId,
        LocalDateTime failedAt,
        String message
) {
}
