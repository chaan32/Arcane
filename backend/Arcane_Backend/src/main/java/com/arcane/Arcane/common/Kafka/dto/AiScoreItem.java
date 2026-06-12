package com.arcane.Arcane.common.Kafka.dto;

import java.util.Map;

public record AiScoreItem(
        String itemId,
        Map<String, Object> features
) {
}
