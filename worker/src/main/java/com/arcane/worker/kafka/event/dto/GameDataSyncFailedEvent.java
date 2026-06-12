package com.arcane.worker.kafka.event.dto;

public record GameDataSyncFailedEvent(
        String jobId,
        String traceId,
        String reason,
        String status
) {
}
