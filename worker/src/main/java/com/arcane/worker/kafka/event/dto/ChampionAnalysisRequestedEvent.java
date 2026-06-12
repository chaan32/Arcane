package com.arcane.worker.kafka.event.dto;

public record ChampionAnalysisRequestedEvent(
        String jobId,
        String traceId,
        Long requestedBy,
        String sourceCollection,
        Integer queueId
) {
}
