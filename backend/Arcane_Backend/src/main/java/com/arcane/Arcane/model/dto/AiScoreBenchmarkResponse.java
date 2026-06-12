package com.arcane.Arcane.model.dto;

public record AiScoreBenchmarkResponse(
        Integer count,
        Long httpSequentialElapsedMs,
        Long kafkaBatchElapsedMs,
        Double improvementPercent,
        String kafkaError
) {
}
