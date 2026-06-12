package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;

public record GameDataSyncRequestedEvent(
        String jobId,
        String traceId,
        Long requestedBy,
        LocalDateTime requestedAt
) {
}
