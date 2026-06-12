package com.arcane.Arcane.common.Kafka.dto;

public record GameDataSyncFailedEvent(
        String jobId,
        String traceId,
        String reason,
        String status
) {
}
