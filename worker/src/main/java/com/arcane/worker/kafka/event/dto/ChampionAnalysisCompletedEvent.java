package com.arcane.worker.kafka.event.dto;

public record ChampionAnalysisCompletedEvent(
        String jobId,
        String traceId,
        String snapshotId,
        Integer totalParticipants,
        Integer totalMatches,
        Integer totalChampions,
        String patchVersion,
        String status
) {
}
