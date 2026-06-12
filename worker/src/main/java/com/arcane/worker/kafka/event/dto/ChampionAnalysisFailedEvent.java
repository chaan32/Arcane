package com.arcane.worker.kafka.event.dto;

public record ChampionAnalysisFailedEvent(
        String jobId,
        String traceId,
        String reason,
        String status
) {
}
