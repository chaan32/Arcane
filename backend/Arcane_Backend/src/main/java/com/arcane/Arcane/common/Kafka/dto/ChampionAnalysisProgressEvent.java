package com.arcane.Arcane.common.Kafka.dto;

public record ChampionAnalysisProgressEvent(
        String jobId,
        String traceId,
        String snapshotId,
        Integer progressPercent,
        String phase,
        String message,
        Integer totalParticipants,
        Integer totalMatches,
        Integer totalChampions,
        String patchVersion,
        String status
) {
}
