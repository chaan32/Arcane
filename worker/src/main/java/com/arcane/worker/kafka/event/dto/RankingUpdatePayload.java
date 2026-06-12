package com.arcane.worker.kafka.event.dto;

public record RankingUpdatePayload(
        String region,
        Integer limit,
        Boolean all ){
}