package com.arcane.Arcane.common.Kafka.dto;

public record GameDataSyncRequestResponse(
        String jobId,
        String message
) {
}
