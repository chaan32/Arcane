package com.arcane.worker.kafka.event.dto;

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
