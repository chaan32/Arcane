package com.arcane.Arcane.common.Kafka.dto;

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
