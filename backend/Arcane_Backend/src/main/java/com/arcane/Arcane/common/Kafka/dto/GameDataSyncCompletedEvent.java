package com.arcane.Arcane.common.Kafka.dto;

public record GameDataSyncCompletedEvent(
        String jobId,
        String traceId,
        String version,
        Integer championCount,
        Integer itemCount,
        Integer summonerSpellCount,
        Integer runeCount,
        String status,
        String message
) {
}
