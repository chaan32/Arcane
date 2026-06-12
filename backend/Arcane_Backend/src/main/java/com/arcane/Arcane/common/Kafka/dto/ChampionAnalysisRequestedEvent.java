package com.arcane.Arcane.common.Kafka.dto;

public record ChampionAnalysisRequestedEvent(
        String jobId,
        String traceId,
        Long requestedBy,
        String sourceCollection,
        Integer queueId
) {
}
