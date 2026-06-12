package com.arcane.Arcane.common.Kafka.service;

import com.arcane.Arcane.common.Kafka.dto.RankingUpdateCompletedEvent;
import com.arcane.Arcane.common.Kafka.dto.RankingUpdateFailEvent;
import com.arcane.Arcane.common.Kafka.dto.RankingUpdateRequestedEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RankingUpdateJobStatusService {
    private static final int MAX_RECENT_JOBS = 20;

    private final Map<String, RankingUpdateJobStatus> jobs = new ConcurrentHashMap<>();

    public void markPublished(RankingUpdateRequestedEvent event) {
        jobs.put(event.jobId(), new RankingUpdateJobStatus(
                event.jobId(),
                "PUBLISHED",
                event.requestedBy(),
                event.requestedAt(),
                null,
                null,
                "Kafka 요청 발행 완료",
                event.traceId()
        ));
    }

    public void markPublishFailed(String jobId, String message) {
        jobs.compute(jobId, (key, current) -> {
            LocalDateTime requestedAt = current == null ? LocalDateTime.now() : current.requestedAt();
            Long requestedBy = current == null ? null : current.requestedBy();
            String traceId = current == null ? null : current.traceId();

            return new RankingUpdateJobStatus(
                    jobId,
                    "PUBLISH_FAILED",
                    requestedBy,
                    requestedAt,
                    null,
                    LocalDateTime.now(),
                    message,
                    traceId
            );
        });
    }

    public void markCompleted(RankingUpdateCompletedEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new RankingUpdateJobStatus(
                event.jobId(),
                "COMPLETED",
                current == null ? null : current.requestedBy(),
                event.requestedAt() == null
                        ? current == null ? null : current.requestedAt()
                        : event.requestedAt(),
                event.completedAt(),
                null,
                event.message(),
                current == null ? null : current.traceId()
        ));
    }

    public void markFailed(RankingUpdateFailEvent event) {
        jobs.compute(event.jobId(), (key, current) -> new RankingUpdateJobStatus(
                event.jobId(),
                "FAILED",
                current == null ? null : current.requestedBy(),
                current == null ? null : current.requestedAt(),
                null,
                event.failedAt(),
                event.message(),
                current == null ? null : current.traceId()
        ));
    }

    public List<RankingUpdateJobStatus> recentJobs() {
        return jobs.values().stream()
                .sorted(Comparator.comparing(
                        RankingUpdateJobStatus::lastChangedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(MAX_RECENT_JOBS)
                .toList();
    }

    public RankingUpdateJobStatus latestJob() {
        return recentJobs().stream().findFirst().orElse(null);
    }

    public record RankingUpdateJobStatus(
            String jobId,
            String status,
            Long requestedBy,
            LocalDateTime requestedAt,
            LocalDateTime completedAt,
            LocalDateTime failedAt,
            String message,
            String traceId
    ) {
        public LocalDateTime lastChangedAt() {
            if (completedAt != null) {
                return completedAt;
            }
            if (failedAt != null) {
                return failedAt;
            }
            return requestedAt;
        }
    }
}
