package com.arcane.worker.kafka.event.dto;

import java.time.LocalDateTime;

public record DatasetCollectRequestedEvent(
        String eventId,
        String jobId,
        String jobKey,
        Long requestedBy,
        LocalDateTime requestedAt,
        String traceId,
        DatasetCollectPayload payload
) {
}
