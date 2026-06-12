package com.arcane.worker.kafka.event.dto;

import java.time.LocalDateTime;

public record GameDataSyncRequestedEvent(
        String jobId,
        String traceId,
        Long requestedBy,
        LocalDateTime requestedAt
) {
}
