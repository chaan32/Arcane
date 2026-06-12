package com.arcane.Arcane.common.Kafka.dto;

public record DatasetCollectPayload(
        String region,
        String rankingKey,
        Integer rankerLimit,
        Integer matchCount,
        Integer queueId,
        Boolean saveMysql,
        Boolean saveMongo
) {
}
