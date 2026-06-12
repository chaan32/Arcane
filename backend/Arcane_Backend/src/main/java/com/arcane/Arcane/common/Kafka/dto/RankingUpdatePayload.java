package com.arcane.Arcane.common.Kafka.dto;

public record RankingUpdatePayload(
        String region,
        Integer limit,
        Boolean all
) {
}
