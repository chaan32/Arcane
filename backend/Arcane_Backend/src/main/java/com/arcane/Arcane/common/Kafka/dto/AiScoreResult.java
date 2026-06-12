package com.arcane.Arcane.common.Kafka.dto;

public record AiScoreResult(
        String itemId,
        Double score
) {
}
