package com.arcane.Arcane.common.Kafka.dto;

public record ChampionAnalysisFailedEvent(
        String jobId,
        String traceId,
        String reason,
        String status
) {
}
