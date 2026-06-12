package com.arcane.Arcane.common.Kafka.dto;

import java.time.LocalDateTime;

public record RankingUpdateRequestedEvent(
        // 카프카에 발행되는 이벤트 자체의 고유 ID
        String eventId,
        // 실제 작업 하나를 식별하는 ID -> 작업을 추적할 때 사용
        String jobId,
        // 동일한 작업을 중복 실행하지 않게 하기 위한 키
        String jobKey,
        // 작업 요청 서버
        Long requestedBy,
        // 큐 대기 시간 계산용. 큐 대기 시간 = 작업 시작 시간 - requestedAt
        LocalDateTime requestedAt,
        // 로그 추적용 id
        String traceId,
        // 실제 요청 메세지
        RankingUpdatePayload payload
) {
}
